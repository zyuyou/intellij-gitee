// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.comment.ui

import com.gitee.pullrequest.data.model.GiteePRDiffReviewThreadMapping
import com.intellij.util.EventDispatcher
import com.intellij.util.containers.SortedList
import java.util.*

class GiteePREditorReviewThreadsModel {
  private val changeEventDispatcher = EventDispatcher.create(ChangesListener::class.java)

  val threadsByLine: MutableMap<Int, SortedList<GiteePRReviewThreadModel>> = mutableMapOf()

  fun addChangesListener(listener: ChangesListener) = changeEventDispatcher.addListener(listener)

  fun update(mappingsByLine: Map<Int, List<GiteePRDiffReviewThreadMapping>>) {
    val removedLines = threadsByLine.keys - mappingsByLine.keys
    for (line in removedLines) {
      val removed = threadsByLine.remove(line).orEmpty()
      changeEventDispatcher.multicaster.threadsRemoved(line, removed.toList())
    }

    for ((line, mappings) in mappingsByLine) {
      val threads = threadsByLine.computeIfAbsent(line) { SortedList(compareBy { it.createdAt }) }

      val threadsById = threads.map { it.id to it }.toMap()
      val mappingsById = mappings.map { it.thread.id to it }.toMap()
      val removedThreads = (threadsById - mappingsById.keys).values
      if (removedThreads.isNotEmpty()) {
        threads.removeAll(removedThreads)
        changeEventDispatcher.multicaster.threadsRemoved(line, removedThreads.toList())
      }

      val addedThreads = mutableListOf<GiteePRReviewThreadModel>()
      for ((id, mapping) in mappingsById) {
        val current = threadsById[id]
        if (current == null) {
          val thread = GiteePRReviewThreadModel(mapping.thread)
          threads.add(thread)
          addedThreads.add(thread)
        }
        else {
          current.update(mapping.thread)
        }
      }
      if (addedThreads.isNotEmpty()) changeEventDispatcher.multicaster.threadsAdded(line, addedThreads)
    }
  }

  interface ChangesListener : EventListener {
    fun threadsAdded(line: Int, threads: List<GiteePRReviewThreadModel>)
    fun threadsRemoved(line: Int, threads: List<GiteePRReviewThreadModel>)
  }
}