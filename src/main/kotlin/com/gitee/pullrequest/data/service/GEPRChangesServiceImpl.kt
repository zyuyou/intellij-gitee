// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data.service

import com.gitee.api.GEGQLRequests
import com.gitee.api.GERepositoryCoordinates
import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeApiRequests
import com.gitee.api.data.GECommit
import com.gitee.api.data.GECommitHash
import com.gitee.api.util.SimpleGEGQLPagesLoader
import com.gitee.pullrequest.data.GEPRChangesProvider
import com.gitee.pullrequest.data.GEPRChangesProviderImpl
import com.gitee.pullrequest.data.GEPRIdentifier
import com.gitee.pullrequest.data.service.GEServiceUtil.logError
import com.gitee.util.GitRemoteUrlCoordinates
import com.google.common.graph.Graph
import com.google.common.graph.GraphBuilder
import com.google.common.graph.ImmutableGraph
import com.google.common.graph.Traverser
import com.intellij.collaboration.async.CompletableFutureUtil
import com.intellij.collaboration.async.CompletableFutureUtil.submitIOTask
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diff.impl.patch.FilePatch
import com.intellij.openapi.diff.impl.patch.PatchReader
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.ProgressWrapper
import com.intellij.openapi.project.Project
import git4idea.fetch.GitFetchSupport
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

class GEPRChangesServiceImpl(private val progressManager: ProgressManager,
                             private val project: Project,
                             private val requestExecutor: GiteeApiRequestExecutor,
                             private val gitRemote: GitRemoteUrlCoordinates,
                             private val ghRepository: GERepositoryCoordinates) : GEPRChangesService {

  override fun fetch(progressIndicator: ProgressIndicator, refspec: String) =
    progressManager.submitIOTask(progressIndicator) {
      GitFetchSupport.fetchSupport(project)
        .fetch(gitRemote.repository, gitRemote.remote, refspec).throwExceptionIfFailed()
    }.logError(LOG, "Error occurred while fetching \"$refspec\"")

  override fun fetchBranch(progressIndicator: ProgressIndicator, branch: String) =
    fetch(progressIndicator, branch).logError(LOG, "Error occurred while fetching \"$branch\"")

  override fun loadCommitsFromApi(progressIndicator: ProgressIndicator, pullRequestId: GEPRIdentifier) =
    progressManager.submitIOTask(progressIndicator) { indicator ->
      SimpleGEGQLPagesLoader(requestExecutor, { p ->
        GEGQLRequests.PullRequest.commits(ghRepository, pullRequestId.number, p)
      }).loadAll(indicator).map { it.commit }.let(::buildCommitsTree)
    }.logError(LOG, "Error occurred while loading commits for PR ${pullRequestId.number}")

  override fun loadCommitDiffs(progressIndicator: ProgressIndicator, baseRefOid: String, oid: String) =
    progressManager.submitIOTask(progressIndicator) {
      val commitDiff = requestExecutor.execute(it,
                                               GiteeApiRequests.Repos.Commits.getDiff(ghRepository, oid))

      val cumulativeDiff = requestExecutor.execute(it,
                                                   GiteeApiRequests.Repos.Commits.getDiff(ghRepository, baseRefOid, oid))
      commitDiff to cumulativeDiff
    }.logError(LOG, "Error occurred while loading diffs for commit $oid")

  override fun loadMergeBaseOid(progressIndicator: ProgressIndicator, baseRefOid: String, headRefOid: String) =
    progressManager.submitIOTask(progressIndicator) {
      requestExecutor.execute(it,
                              GiteeApiRequests.Repos.Commits.compare(ghRepository, baseRefOid, headRefOid)).mergeBaseCommit.sha
    }.logError(LOG, "Error occurred while calculating merge base for $baseRefOid and $headRefOid")

  override fun createChangesProvider(progressIndicator: ProgressIndicator, mergeBaseOid: String, commits: Pair<GECommit, Graph<GECommit>>) =
    progressManager.submitIOTask(progressIndicator) {
      val (lastCommit, graph) = commits
      val commitsDiffsRequests = mutableMapOf<GECommit, CompletableFuture<Pair<String, String>>>()
      for (commit in Traverser.forGraph(graph).depthFirstPostOrder(lastCommit)) {
        commitsDiffsRequests[commit] = loadCommitDiffs(ProgressWrapper.wrap(it), mergeBaseOid, commit.oid)
      }

      CompletableFuture.allOf(*commitsDiffsRequests.values.toTypedArray()).joinCancellable()
      val patchesByCommits = commitsDiffsRequests.mapValues { (_, request) ->
        val diffs = request.joinCancellable()
        val commitPatches = readAllPatches(diffs.first)
        val cumulativePatches = readAllPatches(diffs.second)
        commitPatches to cumulativePatches
      }
      it.checkCanceled()

      GEPRChangesProviderImpl(gitRemote.repository, mergeBaseOid, graph, lastCommit, patchesByCommits) as GEPRChangesProvider
    }.logError(LOG, "Error occurred while building changes from commits")

  companion object {
    private val LOG = logger<GEPRChangesService>()

    @Throws(ProcessCanceledException::class)
    private fun <T> CompletableFuture<T>.joinCancellable(): T {
      try {
        return join()
      }
      catch (e: CancellationException) {
        throw ProcessCanceledException(e)
      }
      catch (e: CompletionException) {
        if (CompletableFutureUtil.isCancellation(e)) throw ProcessCanceledException(e)
        throw CompletableFutureUtil.extractError(e)
      }
    }

    private fun readAllPatches(diffFile: String): List<FilePatch> {
      val reader = PatchReader(diffFile, true)
      reader.parseAllPatches()
      return reader.allPatches
    }

    private fun buildCommitsTree(commits: List<GECommit>): Pair<GECommit, Graph<GECommit>> {
      val commitsBySha = mutableMapOf<String, GECommit>()
      val parentCommits = mutableSetOf<GECommitHash>()

      for (commit in commits) {
        commitsBySha[commit.oid] = commit
        parentCommits.addAll(commit.parents)
      }

      // Last commit is a commit which is not a parent of any other commit
      // We start searching from the last hoping for some semblance of order
      val lastCommit = commits.findLast { !parentCommits.contains(it) } ?: error("Could not determine last commit")

      fun ImmutableGraph.Builder<GECommit>.addCommits(commit: GECommit) {
        addNode(commit)
        for (parent in commit.parents) {
          val parentCommit = commitsBySha[parent.oid]
          if (parentCommit != null) {
            putEdge(commit, parentCommit)
            addCommits(parentCommit)
          }
        }
      }

      return lastCommit to GraphBuilder
        .directed()
        .allowsSelfLoops(false)
        .immutable<GECommit>()
        .apply {
          addCommits(lastCommit)
        }.build()
    }
  }
}