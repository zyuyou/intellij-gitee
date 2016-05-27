package org.intellij.gitosc.api;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

@SuppressWarnings("UnusedDeclaration")
class GitoscRepoRaw implements DataConstructor {
	@Nullable
	public Long id;
	@Nullable
	public String name;
	@Nullable
	public String description;
	@Nullable
	public String defaultBranch;
	@Nullable
	public GitoscUserRaw owner;
	@SerializedName("public")
	@Nullable
	public Boolean isPublic;
	@Nullable
	public String path;
	@Nullable
	public String pathWithNamespace;
	@Nullable
	public Boolean issuesEnabled;
	@Nullable
	public Boolean pullRequestsEnabled;
	@Nullable
	public Boolean wikiEnabled;
	@Nullable
	public Date createdAt;
	@Nullable
	public GitoscNamespaceRaw namespace;
	@Nullable
	public Date lastPushAt;
	@Nullable
	public Long parentId;
	@SerializedName("fork?")
	@Nullable
	public Boolean isFork;
	@Nullable
	public Integer forksCount;
	@Nullable
	public Integer starsCount;
	@Nullable
	public Integer watchesCount;
	@Nullable
	public String language;
	@Nullable
	public String paas;
	@Nullable
	public Boolean stared;
	@Nullable
	public Boolean watched;
	@Nullable
	public String relation;
	@Nullable
	public Integer recomm;
	@Nullable
	public String parentPathWithNamespace;

	@SuppressWarnings("ConstantConditions")
	@NotNull
	public GitoscRepo createRepo() {
		return new GitoscRepo(name, description, isPublic, isFork, path, pathWithNamespace, defaultBranch, owner.createUser());
	}

//	@SuppressWarnings("ConstantConditions")
//	@NotNull
//	public GitoscRepoOrg createRepoOrg() {
//		return new GitoscRepoOrg(name, description, isPublic, isFork, "", "", defaultBranch, owner.createUser(), permissions.create());
//	}

	@SuppressWarnings("ConstantConditions")
	@NotNull
	public GitoscRepoDetailed createRepoDetailed() {
//		GitoscRepo parent = this.parent == null ? null : this.parent.createRepo();
//		GitoscRepo source = this.source == null ? null : this.source.createRepo();
//		return new GitoscRepoDetailed(name, description, isPublic, isFork, "", "", defaultBranch, owner.createUser(), parent, source);
		return new GitoscRepoDetailed(name, description, isPublic, isFork, path, pathWithNamespace, defaultBranch, owner.createUser(), parentId);
	}

	@SuppressWarnings("unchecked")
	@NotNull
	@Override
	public <T> T create(@NotNull Class<T> resultClass) {
		if (resultClass == GitoscRepo.class) {
			return (T) createRepo();
		}
//		if (resultClass == GitoscRepoOrg.class) {
//			return (T) createRepoOrg();
//		}
		if (resultClass == GitoscRepoDetailed.class) {
			return (T) createRepoDetailed();
		}

		throw new ClassCastException(this.getClass().getName() + ": bad class type: " + resultClass.getName());
	}
}
