// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data

import com.gitee.api.data.GECommit
import com.google.common.graph.Graph
import com.google.common.graph.Traverser
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diff.impl.patch.FilePatch
import com.intellij.openapi.diff.impl.patch.TextFilePatch
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.changes.Change
import com.intellij.util.containers.CollectionFactory
import com.intellij.util.containers.HashingStrategy
import com.intellij.vcsUtil.VcsUtil
import git4idea.GitContentRevision
import git4idea.GitRevisionNumber
import git4idea.repo.GitRepository
import java.util.*

class GEPRChangesProviderImpl(private val repository: GitRepository,
                              private val mergeBaseRef: String,
                              commitsGraph: Graph<GECommit>,
                              private val lastCommit: GECommit,
                              patchesByCommits: Map<GECommit, Pair<List<FilePatch>, List<FilePatch>>>)
  : GEPRChangesProvider {

  override val changes = mutableListOf<Change>()
  override val changesByCommits = mutableMapOf<String, List<Change>>()
  override val linearHistory: Boolean

  private val diffDataByChange = CollectionFactory.createCustomHashingStrategyMap<Change, GEPRChangeDiffData>(object : HashingStrategy<Change> {
    override fun equals(o1: Change?, o2: Change?): Boolean {
      return o1 == o2 &&
             o1?.beforeRevision == o2?.beforeRevision &&
             o1?.afterRevision == o2?.afterRevision
    }

    override fun hashCode(change: Change?) = Objects.hash(change, change?.beforeRevision, change?.afterRevision)
  })

  override fun findChangeDiffData(change: Change) = diffDataByChange[change]

  override fun findCumulativeChange(commitSha: String, filePath: String): Change? {
    return diffDataByChange.entries.find {
      it.value is GEPRChangeDiffData.Cumulative && it.value.contains(commitSha, filePath)
    }?.key
  }

  init {
    val commitsBySha = LinkedHashMap<String, GECommitWithPatches>()
    Traverser.forGraph(commitsGraph).depthFirstPostOrder(lastCommit).forEach {
      val (commitPatches, cumulativePatches) = patchesByCommits.getValue(it)
      commitsBySha[it.oid] = GECommitWithPatches(it, commitPatches, cumulativePatches)
    }

    // One or more merge commit for changes that are included into PR (master merges are ignored)
    linearHistory = commitsBySha.values.all { commit ->
      commit.parents.count { commitsBySha.contains(it) } <= 1
    }

    if (linearHistory) {
      initForLinearHistory(commitsBySha)
    }
    else {
      initForHistoryWithMerges(commitsBySha)
    }
  }

  private fun initForLinearHistory(commitsBySha: Map<String, GECommitWithPatches>) {
    val commitsWithPatches = commitsBySha.values
    val fileHistoriesByLastKnownFilePath = mutableMapOf<String, GEPRMutableLinearFileHistory>()

    var previousCommitSha = mergeBaseRef

    val commitsHashes = commitsWithPatches.map { it.sha }
    for (commitWithPatches in commitsWithPatches) {

      val commitSha = commitWithPatches.sha
      val commitChanges = mutableListOf<Change>()

      for (patch in commitWithPatches.commitPatches) {
        val change = createChangeFromPatch(previousCommitSha, commitSha, patch)
        commitChanges.add(change)

        if (patch is TextFilePatch) {
          val beforePath = patch.beforeName
          val afterPath = patch.afterName

          val historyBefore = beforePath?.let { fileHistoriesByLastKnownFilePath.remove(it) }
          val fileHistory = (historyBefore ?: GEPRMutableLinearFileHistory(commitsHashes)).apply {
            append(commitSha, patch)
          }
          if (afterPath != null) {
            fileHistoriesByLastKnownFilePath[afterPath] = fileHistory
          }
          val firstKnownPath = fileHistory.firstKnownFilePath

          val cumulativePatch = findPatchByFilePaths(commitWithPatches.cumulativePatches, firstKnownPath, afterPath) as? TextFilePatch
          if (cumulativePatch == null) {
            LOG.debug("Unable to find cumulative patch for commit patch")
            continue
          }

          diffDataByChange[change] = GEPRChangeDiffData.Commit(commitSha, patch.filePath, patch, cumulativePatch, fileHistory)
        }
      }
      changesByCommits[commitWithPatches.commit.oid] = commitChanges
      previousCommitSha = commitSha
    }

    val fileHistoriesBySummaryFilePath = fileHistoriesByLastKnownFilePath.mapKeys {
      it.value.lastKnownFilePath
    }

    for (patch in commitsBySha.getValue(lastCommit.oid).cumulativePatches) {
      val change = createChangeFromPatch(mergeBaseRef, lastCommit.oid, patch)
      changes.add(change)

      if (patch is TextFilePatch) {
        val filePath = patch.filePath
        val fileHistory = fileHistoriesBySummaryFilePath[filePath]
        if (fileHistory == null) {
          LOG.debug("Unable to find file history for cumulative patch for $filePath")
          continue
        }

        diffDataByChange[change] = GEPRChangeDiffData.Cumulative(lastCommit.oid, filePath, patch, fileHistory)
      }
    }
  }

  private fun initForHistoryWithMerges(commitsBySha: Map<String, GECommitWithPatches>) {
    for (commitWithPatches in commitsBySha.values) {
      val previousCommitSha = commitWithPatches.parents.find { commitsBySha.contains(it) } ?: mergeBaseRef
      val commitSha = commitWithPatches.sha
      val commitChanges = commitWithPatches.commitPatches.map { createChangeFromPatch(previousCommitSha, commitSha, it) }
      changesByCommits[commitWithPatches.commit.oid] = commitChanges
    }

    for (patch in commitsBySha.getValue(lastCommit.oid).cumulativePatches) {
      val change = createChangeFromPatch(mergeBaseRef, lastCommit.oid, patch)
      changes.add(change)

      if (patch is TextFilePatch) {
        diffDataByChange[change] = GEPRChangeDiffData.Cumulative(lastCommit.oid, patch.filePath, patch,
                                                                 GEPRGraphFileHistory(commitsBySha, lastCommit, patch.filePath))
      }
    }
  }

  private fun createChangeFromPatch(beforeRef: String, afterRef: String, patch: FilePatch): Change {
    val project = repository.project
    val (beforePath, afterPath) = getPatchPaths(patch)
    val beforeRevision = beforePath?.let { GitContentRevision.createRevision(it, GitRevisionNumber(beforeRef), project) }
    val afterRevision = afterPath?.let { GitContentRevision.createRevision(it, GitRevisionNumber(afterRef), project) }

    return Change(beforeRevision, afterRevision)
  }

  private fun getPatchPaths(patch: FilePatch): Pair<FilePath?, FilePath?> {
    val beforeName = if (patch.isNewFile) null else patch.beforeName
    val afterName = if (patch.isDeletedFile) null else patch.afterName

    return beforeName?.let { VcsUtil.getFilePath(repository.root, it) } to afterName?.let { VcsUtil.getFilePath(repository.root, it) }
  }

  private fun findPatchByFilePaths(patches: Collection<FilePatch>, beforePath: String?, afterPath: String?): FilePatch? =
    patches.find { (afterPath != null && it.afterName == afterPath) || (afterPath == null && it.beforeName == beforePath) }

  companion object {
    private val LOG = logger<GEPRChangesProvider>()

    private val FilePatch.filePath
      get() = (afterName ?: beforeName)!!
  }
}