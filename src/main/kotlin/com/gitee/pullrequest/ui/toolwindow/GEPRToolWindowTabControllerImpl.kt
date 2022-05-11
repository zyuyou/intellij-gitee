// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.toolwindow

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeApiRequestExecutorManager
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.i18n.GiteeBundle
import com.gitee.pullrequest.action.GEPRActionKeys
import com.gitee.pullrequest.config.GiteePullRequestsProjectUISettings
import com.gitee.pullrequest.data.GEPRDataContext
import com.gitee.pullrequest.data.GEPRDataContextRepository
import com.gitee.pullrequest.data.GEPRIdentifier
import com.gitee.pullrequest.ui.GEApiLoadingErrorHandler
import com.gitee.pullrequest.ui.GECompletableFutureLoadingModel
import com.gitee.pullrequest.ui.GELoadingPanelFactory
import com.gitee.pullrequest.ui.toolwindow.create.GEPRCreateComponentHolder
import com.gitee.ui.util.GEUIUtil
import com.gitee.util.GEGitRepositoryMapping
import com.gitee.util.GEProjectRepositoriesManager
import com.intellij.collaboration.auth.AccountsListener
import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ClearableLazyValue
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.content.Content
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import kotlin.properties.Delegates

internal class GEPRToolWindowTabControllerImpl(private val project: Project,
                                               private val authManager: GiteeAuthenticationManager,
                                               private val repositoryManager: GEProjectRepositoriesManager,
                                               private val dataContextRepository: GEPRDataContextRepository,
                                               private val projectSettings: GiteePullRequestsProjectUISettings,
                                               private val tab: Content) : GEPRToolWindowTabController {

  private var currentRepository: GEGitRepositoryMapping? = null
  private var currentAccount: GiteeAccount? = null

  private val mainPanel = tab.component.apply {
    layout = BorderLayout()
    background = UIUtil.getListBackground()
  }
  private var contentDisposable by Delegates.observable<Disposable?>(null) { _, oldValue, newValue ->
    if (oldValue != null) Disposer.dispose(oldValue)
    if (newValue != null) Disposer.register(tab.disposer!!, newValue)
  }
  private var showingSelectors: Boolean? = null

  override var initialView = GEPRToolWindowViewType.LIST
  override val componentController: GEPRToolWindowTabComponentController?
    get() {
      for (component in mainPanel.components) {
        val controller = UIUtil.getClientProperty(component, GEPRToolWindowTabComponentController.KEY)
        if (controller != null) return controller
      }
      return null
    }

  init {
    authManager.addListener(tab.disposer!!, object : AccountsListener<GiteeAccount> {
      override fun onAccountCredentialsChanged(account: GiteeAccount) {
        ApplicationManager.getApplication().invokeLater(Runnable { Updater().update() }) {
          Disposer.isDisposed(tab.disposer!!)
        }
      }
    })
    repositoryManager.addRepositoryListChangedListener(tab.disposer!!) {
      Updater().update()
    }
    Updater().update()
  }

  private inner class Updater {
    private val repos = repositoryManager.knownRepositories
    private val accounts = authManager.getAccounts()

    fun update() {
      val wasReset = resetIfMissing()
      guessAndSetRepoAndAccount()?.let { (repo, account) ->
        try {
          val requestExecutor = GiteeApiRequestExecutorManager.getInstance().getExecutor(account)
          showPullRequestsComponent(repo, account, requestExecutor, wasReset)
        }
        catch (e: Exception) {
          null
        }
      } ?: showSelectors()
    }

    private fun guessAndSetRepoAndAccount(): Pair<GEGitRepositoryMapping, GiteeAccount>? {
      val saved = projectSettings.selectedRepoAndAccount
      if (saved != null) {
        currentRepository = saved.first
        currentAccount = saved.second
        return saved
      }

      if (currentRepository == null && repos.size == 1) {
        currentRepository = repos.single()
      }

      val repo = currentRepository
      if (repo != null && currentAccount == null) {
        val matchingAccounts = accounts.filter { it.server.equals(repo.geRepositoryCoordinates.serverPath, true) }
        if (matchingAccounts.size == 1) {
          currentAccount = matchingAccounts.single()
        }
      }
      val account = currentAccount
      return if (repo != null && account != null) repo to account else null
    }

    private fun resetIfMissing(): Boolean {
      var wasReset = false
      val repo = currentRepository
      if (repo != null && !repos.contains(repo)) {
        currentRepository = null
        currentAccount = null
        wasReset = true
      }

      val account = currentAccount
      if (account != null && !accounts.contains(account)) {
        currentAccount = null
        wasReset = true
      }
      return wasReset
    }
  }

  private fun showSelectors() {
    if (showingSelectors == true) return
    val disposable = Disposer.newDisposable()
    contentDisposable = disposable
    tab.displayName = GiteeBundle.message("toolwindow.stripe.Pull_Requests")

    val component = GEPRRepositorySelectorComponentFactory(project, authManager, repositoryManager).create(disposable) { repo, account ->
      currentRepository = repo
      currentAccount = account
      val requestExecutor = GiteeApiRequestExecutorManager.getInstance().getExecutor(account, mainPanel) ?: return@create
      projectSettings.selectedRepoAndAccount = repo to account
      showPullRequestsComponent(repo, account, requestExecutor, false)
      GEUIUtil.focusPanel(mainPanel)
    }
    with(mainPanel) {
      removeAll()
      add(component, BorderLayout.NORTH)
      revalidate()
      repaint()
    }
    showingSelectors = true
  }

  private fun showPullRequestsComponent(repositoryMapping: GEGitRepositoryMapping,
                                        account: GiteeAccount,
                                        requestExecutor: GiteeApiRequestExecutor,
                                        force: Boolean) {
    if (showingSelectors == false && !force) return
    tab.displayName = GiteeBundle.message("toolwindow.stripe.Pull_Requests")

    val repository = repositoryMapping.geRepositoryCoordinates
    val remote = repositoryMapping.gitRemoteUrlCoordinates

    val disposable = Disposer.newDisposable()
    contentDisposable = Disposable {
      Disposer.dispose(disposable)
      dataContextRepository.clearContext(repository)
    }

    val loadingModel = GECompletableFutureLoadingModel<GEPRDataContext>(disposable).apply {
      future = dataContextRepository.acquireContext(repository, remote, account, requestExecutor)
    }

    val panel = GELoadingPanelFactory(loadingModel, null, GiteeBundle.message("cannot.load.data.from.gitee"),
                                      GEApiLoadingErrorHandler(project, account) {
                                        val contextRepository = dataContextRepository
                                        contextRepository.clearContext(repository)
                                        loadingModel.future = contextRepository.acquireContext(repository, remote, account, requestExecutor)
                                      }).create { parent, result ->
      val wrapper = Wrapper()
      ComponentController(result, wrapper, disposable).also {
        UIUtil.putClientProperty(parent, GEPRToolWindowTabComponentController.KEY, it)
      }
      initialView = GEPRToolWindowViewType.LIST
      wrapper
    }

    with(mainPanel) {
      removeAll()
      add(panel, BorderLayout.CENTER)
      revalidate()
      repaint()
    }
    showingSelectors = false
  }

  override fun canResetRemoteOrAccount(): Boolean {
    if (currentRepository == null) return false
    if (currentAccount == null) return false

    val singleRepo = repositoryManager.knownRepositories.singleOrNull()
    if (singleRepo == null) return true

    val matchingAccounts = authManager.getAccounts().filter { it.server.equals(singleRepo.geRepositoryCoordinates.serverPath, true) }
    return matchingAccounts.size != 1
  }

  override fun resetRemoteAndAccount() {
    currentRepository = null
    currentAccount = null
    projectSettings.selectedRepoAndAccount = null
    Updater().update()
  }

  private inner class ComponentController(private val dataContext: GEPRDataContext,
                                          private val wrapper: Wrapper,
                                          private val parentDisposable: Disposable) : GEPRToolWindowTabComponentController {

    private val listComponent by lazy { GEPRListComponent.create(project, dataContext, parentDisposable) }
    private val createComponentHolder = ClearableLazyValue.create {
      GEPRCreateComponentHolder(ActionManager.getInstance(), project, projectSettings, repositoryManager, dataContext, this,
                                parentDisposable)
    }

    override lateinit var currentView: GEPRToolWindowViewType
    private var currentDisposable: Disposable? = null
    private var currentPullRequest: GEPRIdentifier? = null

    init {
      when (initialView) {
        GEPRToolWindowViewType.NEW -> createPullRequest(false)
        else -> viewList(false)
      }

      DataManager.registerDataProvider(wrapper) { dataId ->
        when {
          GEPRActionKeys.PULL_REQUESTS_TAB_CONTROLLER.`is`(dataId) -> this
          else -> null
        }
      }
    }

    override fun createPullRequest(requestFocus: Boolean) {
      val allRepos = repositoryManager.knownRepositories.map(GEGitRepositoryMapping::geRepositoryCoordinates)
      tab.displayName = GiteeBundle.message("tab.title.pull.requests.new",
                                             GEUIUtil.getRepositoryDisplayName(allRepos,
                                                                               dataContext.repositoryDataService.repositoryCoordinates))
      currentDisposable?.let { Disposer.dispose(it) }
      currentPullRequest = null
      currentView = GEPRToolWindowViewType.NEW
      wrapper.setContent(createComponentHolder.value.component)
      wrapper.repaint()
      if (requestFocus) GEUIUtil.focusPanel(wrapper.targetComponent)
    }

    override fun resetNewPullRequestView() {
      createComponentHolder.value.resetModel()
    }

    override fun viewList(requestFocus: Boolean) {
      val allRepos = repositoryManager.knownRepositories.map(GEGitRepositoryMapping::geRepositoryCoordinates)
      tab.displayName = GiteeBundle.message("tab.title.pull.requests.at",
                                             GEUIUtil.getRepositoryDisplayName(allRepos,
                                                                               dataContext.repositoryDataService.repositoryCoordinates))
      currentDisposable?.let { Disposer.dispose(it) }
      currentPullRequest = null
      currentView = GEPRToolWindowViewType.LIST
      wrapper.setContent(listComponent)
      wrapper.repaint()
      if (requestFocus) GEUIUtil.focusPanel(wrapper.targetComponent)
    }

    override fun refreshList() {
      dataContext.listLoader.reset()
      dataContext.repositoryDataService.resetData()
    }

    override fun viewPullRequest(id: GEPRIdentifier, requestFocus: Boolean, onShown: ((GEPRViewComponentController?) -> Unit)?) {
      tab.displayName = GiteeBundle.message("pull.request.num", id.number)
      if (currentPullRequest != id) {
        currentDisposable?.let { Disposer.dispose(it) }
        currentDisposable = Disposer.newDisposable("Pull request component disposable").also {
          Disposer.register(parentDisposable, it)
        }
        currentPullRequest = id
        currentView = GEPRToolWindowViewType.DETAILS
        val pullRequestComponent = GEPRViewComponentFactory(ActionManager.getInstance(), project, dataContext, this, id,
                                                            currentDisposable!!)
          .create()
        wrapper.setContent(pullRequestComponent)
        wrapper.repaint()
      }
      if (onShown != null) onShown(UIUtil.getClientProperty(wrapper.targetComponent, GEPRViewComponentController.KEY))
      if (requestFocus) GEUIUtil.focusPanel(wrapper.targetComponent)
    }

    override fun openPullRequestTimeline(id: GEPRIdentifier, requestFocus: Boolean) =
      dataContext.filesManager.createAndOpenTimelineFile(id, requestFocus)

    override fun openPullRequestDiff(id: GEPRIdentifier, requestFocus: Boolean) =
      dataContext.filesManager.createAndOpenDiffFile(id, requestFocus)

    override fun openNewPullRequestDiff(requestFocus: Boolean) {
      dataContext.filesManager.openNewPRDiffFile(requestFocus)
    }
  }
}