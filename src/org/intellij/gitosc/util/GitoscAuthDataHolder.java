package org.intellij.gitosc.util;

import com.intellij.openapi.util.ThrowableComputable;
import org.jetbrains.annotations.NotNull;

/**
 * Created by zyuyou on 16/5/25.
 */
public class GitoscAuthDataHolder {
	@NotNull private GitoscAuthData myAuthData;

	public GitoscAuthDataHolder(@NotNull GitoscAuthData auth){
		myAuthData = auth;
	}

	@NotNull
	public synchronized GitoscAuthData getAuthData(){
		return myAuthData;
	}

	public synchronized <T extends Throwable> void runTransaction(@NotNull GitoscAuthData expected,
	                                                              @NotNull ThrowableComputable<GitoscAuthData, T> task) throws T {
		if(expected != myAuthData){
			return;
		}

		myAuthData = task.compute();
	}

	public static GitoscAuthDataHolder createFromSettings(){
		return new GitoscAuthDataHolder(GitoscSettings.getInstance().getAuthData());
	}
}
