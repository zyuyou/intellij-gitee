package org.intellij.gitosc.exceptions;

import org.intellij.gitosc.api.GitoscErrorMessage;
import org.jetbrains.annotations.Nullable;

/**
 * Created by zyuyou on 16/5/25.
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
