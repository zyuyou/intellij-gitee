package org.intellij.gitosc.exceptions;

import org.intellij.gitosc.api.GitoscErrorMessage;
import org.jetbrains.annotations.Nullable;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/exceptions/GithubStatusCodeException.java
 * @author JetBrains s.r.o.
 * @author Aleksey Pivovarov
 */
public class GitoscStatusCodeException extends GitoscConfusingException {
	private final int myStatusCode;
	private final GitoscErrorMessage myError;

	public GitoscStatusCodeException(String message, int statusCode) {
		this(message, null, statusCode);
	}

	public GitoscStatusCodeException(String message, GitoscErrorMessage error, int statusCode) {
		super(message);
		myStatusCode = statusCode;
		myError = error;
	}

	public int getStatusCode() {
		return myStatusCode;
	}

	@Nullable
	public GitoscErrorMessage getError() {
		return myError;
	}
}
