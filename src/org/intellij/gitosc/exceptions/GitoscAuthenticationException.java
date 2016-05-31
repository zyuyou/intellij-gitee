package org.intellij.gitosc.exceptions;

import java.io.IOException;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/exceptions/GithubAuthenticationException.java
 * @author JetBrains s.r.o.
 * @author Aleksey Pivovarov
 */
public class GitoscAuthenticationException extends IOException {
	public GitoscAuthenticationException() {
		super();
	}

	public GitoscAuthenticationException(String message) {
		super(message);
	}

	public GitoscAuthenticationException(String message, Throwable cause) {
		super(message, cause);
	}

	public GitoscAuthenticationException(Throwable cause) {
		super(cause);
	}
}
