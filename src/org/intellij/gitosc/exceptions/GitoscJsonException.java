package org.intellij.gitosc.exceptions;

import java.io.IOException;

/**
 * Created by zyuyou on 16/5/25.
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
