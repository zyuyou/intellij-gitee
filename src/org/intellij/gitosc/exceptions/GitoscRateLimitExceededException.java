package org.intellij.gitosc.exceptions;

import java.io.IOException;

/**
 * https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/exceptions/GithubRateLimitExceededException.java
 */
public class GitoscRateLimitExceededException extends IOException {
	public GitoscRateLimitExceededException(String message) {
		super(message);
	}
}
