package org.intellij.gitosc.exceptions;

import java.io.IOException;

/**
 * Created by zyuyou on 16/5/25.
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
