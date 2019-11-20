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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.mandatory.RestModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestModel
@SuppressWarnings("UnusedDeclaration")
public class GiteeGist {
	private String id;
	private String description;

	@JsonProperty("public")
	private Boolean isPublic;

	private String url;

	private String htmlUrl;
	private String gitPullUrl;
	private String gitPushUrl;

	private Map<String, GistFile> files;

	private GiteeUser owner;

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", locale = "zh", timezone = "GMT+8")
	private Date createdAt;

	@NotNull
	public String getId() {
		return id;
	}

	@NotNull
	public String getDescription() {
		return StringUtil.notNullize(description);
	}

	public boolean isPublic() {
		return isPublic;
	}

	@NotNull
	public String getHtmlUrl() {
		return htmlUrl;
	}

	@NotNull
	public List<GistFile> getFiles() {
		return new ArrayList<>(files.values());
	}

	@Nullable
	public GiteeUser getUser() {
		return owner;
	}

	public static class GistFile {
		private Long size;

		private String content;

		private String raw_url;

		@NotNull
		public String getContent() {
			return content;
		}

		@NotNull
		public String getRawUrl() {
			return raw_url;
		}
	}
}
