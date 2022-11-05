package com.gitee.ui.cloneDialog

import com.gitee.api.data.GiteeAuthenticatedUser
import com.gitee.api.data.GiteeRepo
import com.gitee.authentication.accounts.GiteeAccount
import javax.swing.AbstractListModel

internal class GECloneDialogRepositoryListModel : AbstractListModel<GERepositoryListItem>() {

  private val itemsByAccount = LinkedHashMap<GiteeAccount, MutableList<GERepositoryListItem>>()
  private val repositoriesByAccount = hashMapOf<GiteeAccount, MutableSet<GiteeRepo>>()

  override fun getSize(): Int = itemsByAccount.values.sumOf { it.size }

  override fun getElementAt(index: Int): GERepositoryListItem {
    var offset = 0
    for ((_, items) in itemsByAccount) {
      if (index >= offset + items.size) {
        offset += items.size
        continue
      }
      return items[index - offset]
    }
    throw IndexOutOfBoundsException(index)
  }

  fun getItemAt(index: Int): Pair<GiteeAccount, GERepositoryListItem> {
    var offset = 0
    for ((account, items) in itemsByAccount) {
      if (index >= offset + items.size) {
        offset += items.size
        continue
      }
      return account to items[index - offset]
    }
    throw IndexOutOfBoundsException(index)
  }

  fun indexOf(account: GiteeAccount, item: GERepositoryListItem): Int {
    if (!itemsByAccount.containsKey(account)) return -1
    var startOffset = 0
    for ((_account, items) in itemsByAccount) {
      if (_account == account) {
        val idx = items.indexOf(item)
        if (idx < 0) return -1
        return startOffset + idx
      }
      else {
        startOffset += items.size
      }
    }
    return -1
  }

  fun clear(account: GiteeAccount) {
    repositoriesByAccount.remove(account)
    val (startOffset, endOffset) = findAccountOffsets(account) ?: return
    itemsByAccount.remove(account)
    fireIntervalRemoved(this, startOffset, endOffset)
  }

  fun setError(account: GiteeAccount, error: Throwable) {
    val accountItems = itemsByAccount.getOrPut(account) { mutableListOf() }
    val (startOffset, endOffset) = findAccountOffsets(account) ?: return
    val errorItem = GERepositoryListItem.Error(account, error)
    accountItems.add(0, errorItem)
    fireIntervalAdded(this, endOffset, endOffset + 1)
    fireContentsChanged(this, startOffset, endOffset + 1)
  }

  /**
   * Since each repository can be in several states at the same time (shared access for a collaborator and shared access for org member) and
   * repositories for collaborators are loaded in separate request before repositories for org members, we need to update order of re-added
   * repo in order to place it close to other organization repos
   */
  fun addRepositories(account: GiteeAccount, details: GiteeAuthenticatedUser, repos: List<GiteeRepo>) {
    val repoSet = repositoriesByAccount.getOrPut(account) { mutableSetOf() }
    val items = itemsByAccount.getOrPut(account) { mutableListOf() }
    var (startOffset, endOffset) = findAccountOffsets(account) ?: return

    val toAdd = mutableListOf<GERepositoryListItem.Repo>()
    for (repo in repos) {
      val item = GERepositoryListItem.Repo(account, details, repo)
      val isNew = repoSet.add(repo)
      if (isNew) {
        toAdd.add(item)
      }
      else {
        val idx = items.indexOf(item)
        items.removeAt(idx)
        fireIntervalRemoved(this, startOffset + idx, startOffset + idx)
        endOffset--
      }
    }
    items.addAll(toAdd)
    fireIntervalAdded(this, endOffset, endOffset + toAdd.size)
  }

  private fun findAccountOffsets(account: GiteeAccount): Pair<Int, Int>? {
    if (!itemsByAccount.containsKey(account)) return null
    var startOffset = 0
    var endOffset = 0
    for ((_account, items) in itemsByAccount) {
      endOffset = startOffset + items.size
      if (_account == account) {
        break
      }
      else {
        startOffset += items.size
      }
    }
    return startOffset to endOffset
  }
}