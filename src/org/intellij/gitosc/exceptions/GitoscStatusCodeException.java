package org.intellij.gitosc.exceptions;

import org.intellij.gitosc.api.GitoscErrorMessage;
import org.jetbrains.annotations.Nullable;

/**
 * https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/exceptions/GithubStatusCodeException.java
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
