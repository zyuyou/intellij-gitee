package org.intellij.gitosc.exceptions;

import java.io.IOException;

/**
 * Created by zyuyou on 16/5/25.
 */
public class GitoscRateLimitExceededException extends IOException {
	public GitoscRateLimitExceededException(String message) {
		super(message);
	}
}
