package org.intellij.gitosc.exceptions;

import java.io.IOException;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/exceptions/GithubRateLimitExceededException.java
 * @author JetBrains s.r.o.
 * @author Aleksey Pivovarov
 */
public class GitoscRateLimitExceededException extends IOException {
	public GitoscRateLimitExceededException(String message) {
		super(message);
	}
}
