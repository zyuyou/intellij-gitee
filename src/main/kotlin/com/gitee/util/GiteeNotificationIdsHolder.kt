// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.util

import com.intellij.notification.impl.NotificationIdsHolder

class GiteeNotificationIdsHolder : NotificationIdsHolder {
  override fun getNotificationIds(): List<String> {
    return listOf(MISSING_DEFAULT_ACCOUNT,
                  PULL_REQUEST_CANNOT_SET_TRACKING_BRANCH,
                  CLONE_UNABLE_TO_CREATE_DESTINATION_DIR,
                  CLONE_UNABLE_TO_FIND_DESTINATION,
                  OPEN_IN_BROWSER_FILE_IS_NOT_UNDER_REPO,
                  OPEN_IN_BROWSER_CANNOT_GET_LAST_REVISION,
                  REBASE_SUCCESS,
                  GIST_CANNOT_CREATE,
                  PULL_REQUEST_CANNOT_LOAD_BRANCHES,
                  PULL_REQUEST_CANNOT_COLLECT_ADDITIONAL_DATA,
                  PULL_REQUEST_CANNOT_LOAD_FORKS,
                  PULL_REQUEST_FAILED_TO_ADD_REMOTE,
                  PULL_REQUEST_PUSH_FAILED,
                  PULL_REQUEST_CREATION_ERROR,
                  PULL_REQUEST_CANNOT_COLLECT_DIFF_DATA,
                  PULL_REQUEST_CANNOT_FIND_REPO,
                  PULL_REQUEST_CREATED,
                  PULL_REQUEST_CANNOT_PROCESS_REMOTE,
                  PULL_REQUEST_NO_CURRENT_BRANCH,
                  REBASE_CANNOT_VALIDATE_UPSTREAM_REMOTE,
                  REBASE_UPSTREAM_IS_OWN_REPO,
                  REBASE_CANNOT_GER_USER_INFO,
                  REBASE_CANNOT_RETRIEVE_UPSTREAM_INFO,
                  REBASE_CANNOT_CONFIGURE_UPSTREAM_REMOTE,
                  REBASE_REPO_NOT_FOUND,
                  REBASE_CANNOT_LOAD_REPO_INFO,
                  REBASE_REPO_IS_NOT_A_FORK,
                  REBASE_MULTI_REPO_NOT_SUPPORTED,
                  REBASE_REMOTE_ORIGIN_NOT_FOUND,
                  REBASE_ACCOUNT_NOT_FOUND,
                  REBASE_FAILED_TO_MATCH_REPO,
                  SHARE_CANNOT_FIND_GIT_REPO,
                  SHARE_CANNOT_CREATE_REPO,
                  SHARE_PROJECT_SUCCESSFULLY_SHARED,
                  SHARE_EMPTY_REPO_CREATED,
                  SHARE_PROJECT_INIT_COMMIT_FAILED,
                  SHARE_PROJECT_INIT_PUSH_FAILED,
                  GIST_CREATED,
                  GIT_REPO_INIT_REPO)
  }

  companion object {
    const val MISSING_DEFAULT_ACCOUNT = "gitee.missing.default.account"
    const val PULL_REQUEST_CANNOT_SET_TRACKING_BRANCH = "gitee.pull.request.cannot.set.tracking.branch"
    const val CLONE_UNABLE_TO_CREATE_DESTINATION_DIR = "gitee.clone.unable.to.create.destination.dir"
    const val CLONE_UNABLE_TO_FIND_DESTINATION = "gitee.clone.unable.to.find.destination"
    const val OPEN_IN_BROWSER_FILE_IS_NOT_UNDER_REPO = "gitee.open.in.browser.file.is.not.under.repo"
    const val OPEN_IN_BROWSER_CANNOT_GET_LAST_REVISION = "gitee.open.in.browser.cannot.get.last.revision"
    const val REBASE_SUCCESS = "gitee.rebase.success"
    const val GIST_CANNOT_CREATE = "gitee.gist.cannot.create"
    const val PULL_REQUEST_CANNOT_LOAD_BRANCHES = "gitee.pull.request.cannot.load.branches"
    const val PULL_REQUEST_CANNOT_COLLECT_ADDITIONAL_DATA = "gitee.pull.request.cannot.collect.additional.data"
    const val PULL_REQUEST_CANNOT_LOAD_FORKS = "gitee.pull.request.cannot.load.forks"
    const val PULL_REQUEST_FAILED_TO_ADD_REMOTE = "gitee.pull.request.failed.to.add.remote"
    const val PULL_REQUEST_PUSH_FAILED = "gitee.pull.request.push.failed"
    const val PULL_REQUEST_CREATION_ERROR = "gitee.pull.request.creation.error"
    const val PULL_REQUEST_CANNOT_COLLECT_DIFF_DATA = "gitee.pull.request.cannot.collect.diff.data"
    const val PULL_REQUEST_CANNOT_FIND_REPO = "gitee.pull.request.cannot.find.repo"
    const val PULL_REQUEST_CREATED = "gitee.pull.request.created"
    const val PULL_REQUEST_CANNOT_PROCESS_REMOTE = "gitee.pull.request.cannot.process.remote"
    const val PULL_REQUEST_NO_CURRENT_BRANCH = "gitee.pull.request.no.current.branch"
    const val REBASE_CANNOT_VALIDATE_UPSTREAM_REMOTE = "gitee.rebase.cannot.validate.upstream.remote"
    const val REBASE_UPSTREAM_IS_OWN_REPO = "gitee.rebase.upstream.is.own.repo"
    const val REBASE_CANNOT_GER_USER_INFO = "gitee.rebase.cannot.get.user.info"
    const val REBASE_CANNOT_RETRIEVE_UPSTREAM_INFO = "gitee.rebase.cannot.retrieve.upstream.info"
    const val REBASE_CANNOT_CONFIGURE_UPSTREAM_REMOTE = "gitee.rebase.cannot.configure.upstream.remote"
    const val REBASE_REPO_NOT_FOUND = "gitee.rebase.repo.not.found"
    const val REBASE_CANNOT_LOAD_REPO_INFO = "gitee.rebase.cannot.load.repo.info"
    const val REBASE_REPO_IS_NOT_A_FORK = "gitee.rebase.repo.is.not.a.fork"
    const val REBASE_MULTI_REPO_NOT_SUPPORTED = "gitee.rebase.multi.repo.not.supported"
    const val REBASE_REMOTE_ORIGIN_NOT_FOUND = "gitee.rebase.remote.origin.not.found"
    const val REBASE_ACCOUNT_NOT_FOUND = "gitee.rebase.account.not.found"
    const val REBASE_FAILED_TO_MATCH_REPO = "rebase.error.failed.to.match.ge.repo"
    const val SHARE_CANNOT_FIND_GIT_REPO = "gitee.share.cannot.find.git.repo"
    const val SHARE_CANNOT_CREATE_REPO = "gitee.share.cannot.create.repo"
    const val SHARE_PROJECT_SUCCESSFULLY_SHARED = "gitee.share.project.successfully.shared"
    const val SHARE_EMPTY_REPO_CREATED = "gitee.share.empty.repo.created"
    const val SHARE_PROJECT_INIT_COMMIT_FAILED = "gitee.share.project.created.init.commit.failed"
    const val SHARE_PROJECT_INIT_PUSH_FAILED = "gitee.share.init.push.failed"
    const val GIST_CREATED = "gitee.gist.created"
    const val GIT_REPO_INIT_REPO = "gitee.git.repo.init.error"
  }
}