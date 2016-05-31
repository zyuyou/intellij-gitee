package org.intellij.gitosc.exceptions;

import java.io.IOException;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/exceptions/GithubOperationCanceledException.java
 * @author JetBrains s.r.o.
 * @author Aleksey Pivovarov
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
