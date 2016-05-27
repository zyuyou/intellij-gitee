package org.intellij.gitosc.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

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
