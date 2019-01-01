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
package com.gitee.authentication.accounts;

import com.gitee.api.GiteeServerPath;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

@Tag("account")
public class GiteeAccount {
	@Attribute("id")
	@NotNull
	private final String myId;

	@Attribute("name")
	@NotNull
	private String myName;

	@Property(style = Property.Style.ATTRIBUTE, surroundWithTag = false)
	@NotNull
	private final GiteeServerPath myServer;

	// serialization
	@SuppressWarnings("unused")
	private GiteeAccount() {
		myId = "";
		myName = "";
		myServer = new GiteeServerPath();
	}

	GiteeAccount(@NotNull String name, @NotNull GiteeServerPath server) {
		myId = UUID.randomUUID().toString();
		myName = name;
		myServer = server;
	}

	@Override
	public String toString() {
		return myServer + "/" + myName;
	}

	@NotNull
	String getId() {
		return myId;
	}

	@NotNull
	public String getName() {
		return myName;
	}

	@Transient
	public void setName(@NotNull String name) {
		myName = name;
	}

	@NotNull
	public GiteeServerPath getServer() {
		return myServer;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof GiteeAccount)) return false;
		GiteeAccount account = (GiteeAccount) o;
		return Objects.equals(myId, account.myId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(myId);
	}
}