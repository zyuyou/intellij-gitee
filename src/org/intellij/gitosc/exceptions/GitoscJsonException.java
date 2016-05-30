package org.intellij.gitosc.exceptions;

import java.io.IOException;

/**
 * https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/exceptions/GithubJsonException.java
 */
public class GitoscJsonException extends IOException {
	public GitoscJsonException() {
		super();
	}

	public GitoscJsonException(String message) {
		super(message);
	}

	public GitoscJsonException(String message, Throwable cause) {
		super(message, cause);
	}

	public GitoscJsonException(Throwable cause) {
		super(cause);
	}
}
