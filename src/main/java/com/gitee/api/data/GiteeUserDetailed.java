/*
 *  Copyright 2016-2019 码云 - Gitee
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.gitee.api.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.intellij.collaboration.auth.AccountDetails;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.mandatory.RestModel;

import java.util.Date;

@RestModel
@SuppressWarnings("UnusedDeclaration")
public class GiteeUserDetailed extends GiteeUser implements AccountDetails {
	private String name;
	private String email;

	private String weibo;
	private String bio;
	private String blog;

	private Integer publicRepos;
	private Integer publicGists;
	private Integer followers;
	private Integer following;

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", locale = "zh", timezone = "GMT+8")
	private Date createdAt;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", locale = "zh", timezone = "GMT+8")
	private Date updatedAt;

	public @NotNull String getName() {
		return name;
	}

	@Nullable
	public String getEmail() {
		return email;
	}
}
