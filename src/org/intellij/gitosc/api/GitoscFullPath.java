package org.intellij.gitosc.api;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by zyuyou on 16/5/25.
 */
public class GitoscFullPath {
	@NotNull private final String myUserName;
	@NotNull private final String myRepositoryName;

	public GitoscFullPath(@NotNull String myUserName, @NotNull String myRepositoryName) {
		this.myUserName = myUserName;
		this.myRepositoryName = myRepositoryName;
	}

	@NotNull
	public String getUser() {
		return myUserName;
	}

	@NotNull
	public String getRepository() {
		return myRepositoryName;
	}

	@NotNull
	public String getFullName(){
		return myUserName + "/" + myRepositoryName;
	}

	@Override
	public String toString() {
		return "'" + getFullName() + "'";
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		GitoscFullPath that = (GitoscFullPath)o;

		if (!StringUtil.equalsIgnoreCase(myRepositoryName, that.myRepositoryName)) return false;
		if (!StringUtil.equalsIgnoreCase(myUserName, that.myUserName)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = myUserName.hashCode();
		result = 31 * result + myRepositoryName.hashCode();
		return result;
	}

}
