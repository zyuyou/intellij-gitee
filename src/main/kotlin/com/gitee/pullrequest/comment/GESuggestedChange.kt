// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.gitee.pullrequest.comment

import com.gitee.util.GEPatchHunkUtil
import com.intellij.openapi.diff.impl.patch.PatchHunk
import com.intellij.openapi.diff.impl.patch.PatchLine
import com.intellij.openapi.diff.impl.patch.PatchReader

data class GESuggestedChange(
  val commentBody: String,
  val patchHunk: PatchHunk,
  val filePath: String,
  val startLine: Int,
  val endLine: Int
) {
  fun cutContextContent(): List<String> = patchHunk.lines
    .filter { it.type != PatchLine.Type.REMOVE }
    .dropLast(endLine - startLine + 1)
    .takeLast(3)
    .map { it.text }

  fun cutChangedContent(): List<String> = patchHunk.lines
    .filter { it.type != PatchLine.Type.REMOVE }
    .takeLast(endLine - startLine + 1)
    .map { it.text }

  fun cutSuggestedChangeContent(): List<String> {
    return commentBody.lines()
      .dropWhile { !it.startsWith("```suggestion") }
      .drop(1)
      .takeWhile { !it.startsWith("```") }
  }

  companion object {
    fun create(commentBody: String, diffHunk: String, filePath: String, startLine: Int, endLine: Int): GESuggestedChange {
      val patchHunk = parseDiffHunk(diffHunk, filePath)

      return GESuggestedChange(commentBody, patchHunk, filePath, startLine - 1, endLine - 1)
    }

    fun containsSuggestedChange(markdownText: String): Boolean = markdownText.lines().any { it.startsWith("```suggestion") }

    private fun parseDiffHunk(diffHunk: String, filePath: String): PatchHunk {
      val patchReader = PatchReader(GEPatchHunkUtil.createPatchFromHunk(filePath, diffHunk))
      patchReader.readTextPatches()
      return patchReader.textPatches[0].hunks.lastOrNull() ?: PatchHunk(0, 0, 0, 0)
    }
  }
}