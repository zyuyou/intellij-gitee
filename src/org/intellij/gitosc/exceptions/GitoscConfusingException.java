package org.intellij.gitosc.exceptions;

import java.io.IOException;

/**
 * https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/exceptions/GithubConfusingException.java
 */
public class GitoscConfusingException extends IOException {
	private String myDetails;

	public GitoscConfusingException() {
	}

	public GitoscConfusingException(String message) {
		super(message);
	}

	public GitoscConfusingException(String message, Throwable cause) {
		super(message, cause);
	}

	public GitoscConfusingException(Throwable cause) {
		super(cause);
	}

	public void setDetails(String myDetails) {
		this.myDetails = myDetails;
	}

	@Override
	public String getMessage() {
		if(myDetails == null){
			return super.getMessage();
		}else{
			return myDetails + "\n\n" + super.getMessage();
		}
	}
}
