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
package com.gitee.tasks;

import com.intellij.tasks.impl.SimpleComment;
import com.intellij.util.text.DateFormatUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

import static com.gitee.i18n.GiteeBundle.message;

/**
 * @author Yuyou Chow
 * <p>
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/tasks/GithubComment.java
 * @author JetBrains s.r.o.
 * @author Dennis.Ushakov
 */
public class GiteeComment extends SimpleComment {
	@Nullable
	private final String myAvatarUrl;
	@NotNull
	private final String myUserHtmlUrl;

	public GiteeComment(@Nullable Date date, @Nullable String author, @NotNull String text, @Nullable String avatarUrl, @NotNull String userHtmlUrl) {
		super(date, author, text);
		myAvatarUrl = avatarUrl;
		myUserHtmlUrl = userHtmlUrl;
	}

	@Override
	public void appendTo(StringBuilder builder) {
		builder.append("<hr>");
		builder.append("<table>");
		builder.append("<tr><td>");

		if (myAvatarUrl != null) {
			builder.append("<img src=\"").append(myAvatarUrl).append("\" height=\"40\" width=\"40\"/><br>");
		}
		builder.append("</td><td>");

		if (getAuthor() != null) {
			builder.append("<b>").append(message("task.comment.author")).append("</b> <a href=\"").
					append(myUserHtmlUrl).append("\">").append(getAuthor()).append("</a><br>");
		}

		if (getDate() != null) {
			builder.append("<b>").append(message("task.comment.date")).append("</b> ")
					.append(DateFormatUtil.formatDateTime(getDate())).append("<br>");
		}

		builder.append("</td></tr></table>");

		builder.append(getText()).append("<br>");
	}
}
