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

package com.gitee.exceptions;

import com.gitee.api.data.GiteeErrorMessage;
import org.jetbrains.annotations.Nullable;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/exceptions/GithubStatusCodeException.java
 * @author JetBrains s.r.o.
 * @author Aleksey Pivovarov
 */
public class GiteeStatusCodeException extends GiteeConfusingException {
	private final int myStatusCode;
	private final GiteeErrorMessage myError;

	public GiteeStatusCodeException(String message, int statusCode) {
		this(message, null, statusCode);
	}

	public GiteeStatusCodeException(String message, GiteeErrorMessage error, int statusCode) {
		super(message);
		myStatusCode = statusCode;
		myError = error;
	}

	public int getStatusCode() {
		return myStatusCode;
	}

	@Nullable
	public GiteeErrorMessage getError() {
		return myError;
	}
}
