package org.intellij.gitosc.exceptions;

import java.io.IOException;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/exceptions/GithubJsonException.java
 * @author JetBrains s.r.o.
 * @author Aleksey Pivovarov
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
