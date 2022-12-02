package com.gitee.ui.cloneDialog

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeApiRequests
import com.gitee.api.data.GiteeRepo
import com.gitee.api.data.request.Affiliation
import com.gitee.api.data.request.GiteeRequestPagination
import com.gitee.api.util.GiteeApiPagesLoader
import com.gitee.authentication.accounts.GEAccountManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.exceptions.GiteeMissingTokenException
import com.intellij.collaboration.async.CompletableFutureUtil.errorOnEdt
import com.intellij.collaboration.async.CompletableFutureUtil.submitIOTask
import com.intellij.collaboration.ui.SimpleEventListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.ui.SingleSelectionModel
import com.intellij.util.EventDispatcher
import kotlinx.coroutines.runBlocking
import javax.swing.ListSelectionModel

internal class GECloneDialogRepositoryListLoaderImpl : GECloneDialogRepositoryListLoader, Disposable {

  private val indicatorsMap = mutableMapOf<GiteeAccount, ProgressIndicator>()
  private val loadingEventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)

  override val loading: Boolean
    get() = indicatorsMap.isNotEmpty()

  override val listModel = GECloneDialogRepositoryListModel()
  override val listSelectionModel = SingleSelectionModel()

  override fun loadRepositories(account: GiteeAccount) {
    if (indicatorsMap.containsKey(account)) return

    val indicator = EmptyProgressIndicator()
    indicatorsMap[account] = indicator
    loadingEventDispatcher.multicaster.eventOccurred()

    ProgressManager.getInstance().submitIOTask(indicator) {
      val credentials = runBlocking { service<GEAccountManager>().findCredentials(account) }?: throw GiteeMissingTokenException(account)
      val executor = service<GiteeApiRequestExecutor.Factory>().create(credentials)

      val details = executor.execute(indicator, GiteeApiRequests.CurrentUser.get(account.server))

      val repoPagesRequest = GiteeApiRequests.CurrentUser.Repos.pages(account.server,
        affiliation = Affiliation.combine(Affiliation.OWNER, Affiliation.COLLABORATOR),
        pagination = GiteeRequestPagination.DEFAULT)
      val pageItemsConsumer: (List<GiteeRepo>) -> Unit = {
        indicator.checkCanceled()
        runInEdt {
          indicator.checkCanceled()
          preservingSelection(listModel, listSelectionModel) {
            listModel.addRepositories(account, details, it)
          }
        }
      }
      GiteeApiPagesLoader.loadAll(executor, indicator, repoPagesRequest, pageItemsConsumer)

      val orgsRequest = GiteeApiRequests.CurrentUser.Orgs.pages(account.server)
      val userOrganizations = GiteeApiPagesLoader.loadAll(executor, indicator, orgsRequest).sortedBy { it.login }

      for (org in userOrganizations) {
        val orgRepoRequest = GiteeApiRequests.Organisations.Repos.pages(account.server, org.login, GiteeRequestPagination.DEFAULT)
        GiteeApiPagesLoader.loadAll(executor, indicator, orgRepoRequest, pageItemsConsumer)
      }
    }.whenComplete { _, _ ->
      indicatorsMap.remove(account)
      loadingEventDispatcher.multicaster.eventOccurred()
    }.errorOnEdt(ModalityState.any()) {
      preservingSelection(listModel, listSelectionModel) {
        listModel.setError(account, it)
      }
    }
  }

  override fun clear(account: GiteeAccount) {
    indicatorsMap[account]?.cancel()
    listModel.clear(account)
    loadingEventDispatcher.multicaster.eventOccurred()
  }

  override fun addLoadingStateListener(listener: () -> Unit) = SimpleEventListener.addListener(loadingEventDispatcher, listener)

  override fun dispose() {
    indicatorsMap.forEach { (_, indicator) -> indicator.cancel() }
    loadingEventDispatcher.multicaster.eventOccurred()
  }

  companion object {
    private fun preservingSelection(listModel: GECloneDialogRepositoryListModel, selectionModel: ListSelectionModel, action: () -> Unit) {
      val selection = if (selectionModel.isSelectionEmpty) {
        null
      }
      else {
        selectionModel.leadSelectionIndex.let {
          if (it < 0 || listModel.size == 0) null
          else listModel.getItemAt(it)
        }
      }
      action()
      if (selection != null) {
        val (account, item) = selection
        val newIdx = listModel.indexOf(account, item)
        if (newIdx >= 0) {
          selectionModel.setSelectionInterval(newIdx, newIdx)
        }
      }
    }
  }
}