package com.gitee.api.data;

/**
 * Created by zyuyou on 2018/8/1.
 */
public class GiteeAuthenticatedUser extends GiteeUserDetailed {
	private Integer totalPrivateRepos;
	private Integer ownedPrivateRepos;

	public boolean canCreatePrivateRepo() {
		return ownedPrivateRepos == null || ownedPrivateRepos < totalPrivateRepos;
	}

}
