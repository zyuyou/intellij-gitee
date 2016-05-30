/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.intellij.gitosc.api;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

/**
 *  https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/api/GithubRepoRaw.java
 */
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


	/**
	 * todo status和message 是创建远程项目失败的时候的返回;
	 * 目前Share on GitOSC的逻辑处理流程优先判断远程仓库是否存在,所以这两个字段暂时无用, 考虑是否要删除?
	 * */
	@Nullable
	public Integer status;
	@Nullable
	public String message;

	@SuppressWarnings("ConstantConditions")
	@NotNull
	public GitoscRepo createRepo() {
		return new GitoscRepo(name, description, isPublic, isFork, path, pathWithNamespace, defaultBranch, owner.createUser());
	}

	@SuppressWarnings("ConstantConditions")
	@NotNull
	public GitoscRepoDetailed createRepoDetailed() {
		return new GitoscRepoDetailed(name, description, isPublic, isFork, path, pathWithNamespace, defaultBranch, owner.createUser(), parentId);
	}

	@SuppressWarnings("unchecked")
	@NotNull
	@Override
	public <T> T create(@NotNull Class<T> resultClass) {
		if (resultClass == GitoscRepo.class) {
			return (T) createRepo();
		}
		if (resultClass == GitoscRepoDetailed.class) {
			return (T) createRepoDetailed();
		}

		throw new ClassCastException(this.getClass().getName() + ": bad class type: " + resultClass.getName());
	}
}
