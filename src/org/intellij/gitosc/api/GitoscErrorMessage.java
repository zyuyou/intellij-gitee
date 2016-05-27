package org.intellij.gitosc.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Created by zyuyou on 16/5/25.
 */
public class GitoscErrorMessage {
	@Nullable
	public String message;
	@Nullable public List<Error> errors;

	public static class Error {
		@Nullable public String resource;
		@Nullable public String field;
		@Nullable public String code;
		@Nullable public String message;
	}

	@Nullable
	public String getMessage() {
		if (errors == null) {
			return message;
		}
		else {
			StringBuilder s = new StringBuilder();
			s.append(message);
			for (Error e : errors) {
				s.append(String.format("<br/>[%s; %s]%s: %s", e.resource, e.field, e.code, e.message));
			}
			return s.toString();
		}
	}

	public boolean containsReasonMessage(@NotNull String reason) {
		if (message == null) return false;
		return message.contains(reason);
	}

	public boolean containsErrorCode(@NotNull String code) {
		if (errors == null) return false;
		for (Error error : errors) {
			if (error.code != null && error.code.contains(code)) return true;
		}
		return false;
	}

	public boolean containsErrorMessage(@NotNull String message) {
		if (errors == null) return false;
		for (Error error : errors) {
			if (error.code != null && error.code.contains(message)) return true;
		}
		return false;
	}
}
