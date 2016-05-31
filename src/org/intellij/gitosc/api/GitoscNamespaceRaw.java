/*
 * Copyright 2016 码云
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

/**
 * @author Yuyou Chow
 *
 */
public class GitoscNamespaceRaw implements DataConstructor {
	@Nullable public String address;
	@Nullable public String avatar;
	@Nullable public Date createdAt;
	@Nullable public String description;
	@Nullable public String email;
	@Nullable public Long id;
	@Nullable public String location;
	@Nullable public String name;
	@Nullable public Long ownerId;
	@Nullable public String path;
	@Nullable public Boolean isPublic;
	@Nullable public Date updatedAt;
	@Nullable public String url;

	@NotNull
	@Override
	public <T> T create(@NotNull Class<T> resultClass) throws IllegalArgumentException, NullPointerException, ClassCastException {
		return null;
	}
}
