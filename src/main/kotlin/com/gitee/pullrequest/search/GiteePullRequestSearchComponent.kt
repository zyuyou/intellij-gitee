/*
 *  Copyright 2016-2019 码云 - Gitee
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.gitee.pullrequest.search

import com.gitee.api.data.GiteeIssueState
import com.gitee.api.data.GiteePullRequest
import com.gitee.api.search.GiteePullRequestsSearchSort
import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.util.TextFieldCompletionProviderDumbAware
import com.intellij.util.textCompletion.TextFieldWithCompletion
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/pullrequest/search/GithubPullRequestSearchComponent.kt
 * @author JetBrains s.r.o.
 */
internal class GiteePullRequestSearchComponent(project: Project,
                                               private val autoPopupController: AutoPopupController,
                                               private val model: GiteePullRequestSearchModel) : BorderLayoutPanel() {

  private val searchField = object : TextFieldWithCompletion(project, SearchCompletionProvider(), "", true, true, false, false) {

    override fun processKeyBinding(ks: KeyStroke?, e: KeyEvent?, condition: Int, pressed: Boolean): Boolean {
      if (e?.keyCode == KeyEvent.VK_ENTER && pressed) {
        updateQuery()
        return true
      }
      return super.processKeyBinding(ks, e, condition, pressed)
    }

    override fun createEditor(): EditorEx {
      return super.createEditor().apply {
        putUserData(AutoPopupController.NO_ADS, true)
      }
    }

    override fun setupBorder(editor: EditorEx) {
      editor.setBorder(JBUI.Borders.empty(6, 5))
    }
  }

  var searchText: String
    get() = searchField.text
    set(value) {
      searchField.text = value
      updateQuery()
    }

  init {
    val icon = JBLabel(AllIcons.Actions.Find).apply {
      border = JBUI.Borders.emptyLeft(5)
    }
    addToLeft(icon)
    addToCenter(searchField)
    UIUtil.setNotOpaqueRecursively(searchField)
  }

  private fun updateQuery() {
    model.query = GiteePullRequestSearchQuery.parseFromString(searchField.text)
  }

  override fun updateUI() {
    super.updateUI()
    background = UIUtil.getListBackground()
  }

  private inner class SearchCompletionProvider : TextFieldCompletionProviderDumbAware(true) {
    private val addColonInsertHandler = object : InsertHandler<LookupElement> {
      override fun handleInsert(context: InsertionContext, item: LookupElement) {
        if (context.completionChar == ':') return
        val editor = context.editor
        if (!isAtColon(context)) {
          EditorModificationUtil.insertStringAtCaret(editor, ":")
          context.commitDocument()
        }
        autoPopupController.autoPopupMemberLookup(editor, null)
      }

      private fun isAtColon(context: InsertionContext): Boolean {
        val startOffset = context.startOffset
        val document = context.document
        return document.textLength > startOffset && document.charsSequence[startOffset] == ':'
      }
    }

    override fun addCompletionVariants(text: String, offset: Int, prefix: String, result: CompletionResultSet) {
      val qualifierName = getCurrentQualifierName(text, offset)
      if (qualifierName == null) {
        result.addElement(LookupElementBuilder.create(GiteePullRequestSearchQuery.QualifierName.state)
                            .withTailText(":")
                            .withInsertHandler(addColonInsertHandler))
        result.addElement(LookupElementBuilder.create(GiteePullRequestSearchQuery.QualifierName.author)
                            .withTailText(":")
                            .withTypeText("username", true)
                            .withInsertHandler(addColonInsertHandler))
        result.addElement(LookupElementBuilder.create(GiteePullRequestSearchQuery.QualifierName.assignee)
                            .withTailText(":")
                            .withTypeText("username", true)
                            .withInsertHandler(addColonInsertHandler))
        result.addElement(LookupElementBuilder.create(GiteePullRequestSearchQuery.QualifierName.after)
                            .withTailText(":")
                            .withTypeText("YYYY-MM-DD", true)
                            .withInsertHandler(addColonInsertHandler))
        result.addElement(LookupElementBuilder.create(GiteePullRequestSearchQuery.QualifierName.before)
                            .withTailText(":")
                            .withTypeText("YYYY-MM-DD", true)
                            .withInsertHandler(addColonInsertHandler))
        result.addElement(LookupElementBuilder.create(GiteePullRequestSearchQuery.QualifierName.sortBy)
                            .withTailText(":")
                            .withInsertHandler(addColonInsertHandler))
      }
      else when {
        qualifierName.equals(GiteePullRequestSearchQuery.QualifierName.state.name, true) -> {
          for (state in GiteeIssueState.values()) {
            result.addElement(LookupElementBuilder.create(state.name))
          }
        }
        qualifierName.equals(GiteePullRequestSearchQuery.QualifierName.sortBy.name, true) -> {
          for (sort in GiteePullRequestsSearchSort.values()) {
            result.addElement(LookupElementBuilder.create(sort.name))
          }
        }
      }
    }

    /**
     * Prefix is the char sequence from last space or first colon after space/line start to caret
     */
    override fun getPrefix(currentTextPrefix: String): String {
      val spaceIdx = currentTextPrefix.lastIndexOf(' ')
      val colonIdx = currentTextPrefix.indexOf(':', Math.max(spaceIdx, 0))
      return currentTextPrefix.substring(Math.max(spaceIdx, colonIdx) + 1)
    }

    /**
     * Current qualifier name is the nearest char sequence in between space and colon or before first colon
     * "qname:test" -> "qname"
     * "qname:test:test" -> "qname"
     * " qname:test:test" -> "qname"
     * " qname:test:test " -> null
     */
    private fun getCurrentQualifierName(text: String, offset: Int): String? {
      val spaceIdx = text.lastIndexOf(' ', offset - 1)
      val colonIdx = text.indexOf(':', Math.max(spaceIdx, 0))
      if (colonIdx < 0 || spaceIdx > colonIdx) return null
      return text.substring(spaceIdx + 1, colonIdx)
    }
  }
}