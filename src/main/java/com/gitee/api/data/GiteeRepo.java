/*
 * Copyright 2016-2017 码云
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
 *
 */
package com.gitee.api.data;

import com.gitee.api.GiteeFullPath;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.mandatory.Mandatory;
import org.jetbrains.io.mandatory.RestModel;

import java.util.Date;

@RestModel
@SuppressWarnings("UnusedDeclaration")
public class GiteeRepo {
	private Long id;
	@Mandatory private String name;
	private String fullName;
	private String description;

	@SerializedName("private")
	@Mandatory private Boolean isPrivate;
	@SerializedName("fork")
	@Mandatory private Boolean isFork;

	private String path;
	private String url;

	@Mandatory private String htmlUrl;

	private String forksUrl;
	private String keysUrl;
	private String collaboratorsUrl;
	private String hooksUrl;
	private String branchesUrl;
	private String tagsUrl;
	private String blobsUrl;
	private String stargazersUrl;
	private String contributorsUrl;
	private String commitsUrl;
	private String commentsUrl;
	private String issueCommentUrl;
	private String issuesUrl;
	private String pullsUrl;
	private String milestonesUrl;
	private String notificationsUrl;
	private String labelsUrl;
	private String releasesUrl;

	private Boolean recommand;

	private String homepage;
	private String language;

	private Integer forksCount;
	private Integer stargazersCount;
	private Integer watchersCount;
	private Integer openIssuesCount;

	private String masterBranch;
	private String defaultBranch;

	private Boolean hasIssues;
	private Boolean hasWiki;
	private Boolean hasDownloads;
	private Boolean hasPage;

	private Boolean pullRequestsEnabled;
	private String license;

	@Mandatory private GiteeUser owner;

	private String pass;

	private Boolean stared;
	private Boolean watched;

	private Permission permisson;

	private Date pushedAt;
	private Date createdAt;
	private Date updatedAt;

	@NotNull
	public String getName() {
		return name;
	}

	@NotNull
	public String getDescription() {
		return StringUtil.notNullize(description);
	}

	public boolean isPrivate() {
		return isPrivate;
	}

	public boolean isFork() {
		return isFork;
	}

	@NotNull
	public String getHtmlUrl() {
		return htmlUrl;
	}

	@Nullable
	public String getDefaultBranch() {
		return defaultBranch;
	}

	@NotNull
	public GiteeUser getOwner() {
		return owner;
	}

	@NotNull
	public String getUserName() {
		return getOwner().getLogin();
	}

	@NotNull
	public String getFullName() {
		return StringUtil.isEmptyOrSpaces(fullName) ? getUserName() + "/" + getName() : fullName;
	}

	@NotNull
	public GiteeFullPath getFullPath() {
		return new GiteeFullPath(getUserName(), getName(), getFullName());
	}


	@RestModel
	public static class Permission{
		private Boolean pull;
		private Boolean push;
		private Boolean admin;
	}

	// v3 session api
	@NotNull private final String myName;
	@NotNull private final String myDesc;

	private final boolean myIsPublic;
	private final boolean myIsFork;

	@NotNull private final String myPath;
	@NotNull private final String myPathWithNamespace;

	@Nullable private final String myDefaultBranch;

	@NotNull private final GiteeUser myOwner;

	public GiteeRepo(@NotNull String myName,
	                 @NotNull String myDesc,
	                 boolean myIsPublic,
	                 boolean myIsFork,
	                 @NotNull String myPath,
	                 @NotNull String myPathWithNamespace,
	                 @Nullable String myDefaultBranch,
	                 @NotNull GiteeUser myOwner) {

		this.myName = myName;
		this.myDesc = myDesc;
		this.myIsPublic = myIsPublic;
		this.myIsFork = myIsFork;
		this.myPath = myPath;
		this.myPathWithNamespace = myPathWithNamespace;
		this.myDefaultBranch = myDefaultBranch;
		this.myOwner = myOwner;
	}

//	@NotNull
//	public String getName() {
//		return myName;
//	}

//	@NotNull
//	public String getDesc() {
//		return myDesc;
//	}

	public boolean isPublic() {
		return myIsPublic;
	}

//	public boolean isFork() {
//		return myIsFork;
//	}

	@NotNull
	public String getPath() {
		return myPath;
	}

	@NotNull
	public String getPathWithNamespace() {
		return myPathWithNamespace;
	}

//	@Nullable
//	public String getDefaultBranch() {
//		return myDefaultBranch;
//	}

//	@NotNull
//	public GiteeUser getOwner() {
//		return myOwner;
//	}

//	@NotNull
//	public String getUserName(){
//		return getOwner().getLogin();
//	}

//	@NotNull
//	public GiteeFullPath getFullPath(){
//		String[] paths = getPathWithNamespace().split("/");
//		return new GiteeFullPath(paths[0], paths[1]);
//	}

//	@NotNull
//	public String getHtmlUrl(){
//		String[] paths = getPathWithNamespace().split("/");
//		if(paths.length == 2){
//			return GiteeUrlUtil.getApiProtocol() + GiteeUrlUtil.getGitHostWithoutProtocol() + "/" + paths[0] + "/" + paths[1] + ".git";
//		}else{
//			return GiteeUrlUtil.getApiProtocol() + GiteeUrlUtil.getGitHostWithoutProtocol() + "/" + getUserName() + "/" + paths[0] + ".git";
//		}
//	}
}
