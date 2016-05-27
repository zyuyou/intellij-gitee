package org.intellij.gitosc.exceptions;

import java.io.IOException;

/**
 * Created by zyuyou on 16/5/25.
 *
 * https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/exceptions/GithubOperationCanceledException.java
 */
public class GitoscOperationCanceledException extends IOException {

	public GitoscOperationCanceledException() {
		super();
	}

	public GitoscOperationCanceledException(String message) {
		super(message);
	}

	public GitoscOperationCanceledException(String message, Throwable cause) {
		super(message, cause);
	}

	public GitoscOperationCanceledException(Throwable cause) {
		super(cause);
	}
}
