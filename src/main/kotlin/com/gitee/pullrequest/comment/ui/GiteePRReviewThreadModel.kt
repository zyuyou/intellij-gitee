// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.comment.ui

import com.gitee.api.data.pullrequest.GEPullRequestReviewThread
import com.gitee.pullrequest.ui.SimpleEventListener
import com.intellij.ui.CollectionListModel
import com.intellij.util.EventDispatcher
import kotlin.properties.Delegates.observable

class GiteePRReviewThreadModel(thread: GEPullRequestReviewThread)
  : CollectionListModel<GiteePRReviewCommentModel>(thread.comments.map(GiteePRReviewCommentModel.Companion::convert)) {

  private val collapseStateEventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)


  val id: String = thread.id
  val createdAt = thread.createdAt
  val filePath = thread.path
  val diffHunk = thread.diffHunk

  var fold by observable(true) { _, _, _ ->
    collapseStateEventDispatcher.multicaster.eventOccurred()
  }

  // New comments can only appear at the end of the list and cannot change order
  fun update(thread: GEPullRequestReviewThread) {
    var removed = 0
    for (i in 0 until items.size) {
      val idx = i - removed
      val newComment = thread.comments.getOrNull(idx)
      if (newComment == null) {
        removeRange(idx, items.size - 1)
        break
      }

      val comment = items.getOrNull(idx) ?: break
      if (comment.id == newComment.id) {
        if (comment.update(newComment))
          fireContentsChanged(this, idx, idx)
      }
      else {
        remove(idx)
        removed++
      }
    }

    if (size == thread.comments.size) return
    val newComments = thread.comments.subList(size, thread.comments.size)
    add(newComments.map { GiteePRReviewCommentModel.convert(it) })
  }

  fun addFoldStateChangeListener(listener: () -> Unit) {
    collapseStateEventDispatcher.addListener(object : SimpleEventListener {
      override fun eventOccurred() {
        listener()
      }
    })
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is GiteePRReviewThreadModel) return false

    if (id != other.id) return false

    return true
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }
}