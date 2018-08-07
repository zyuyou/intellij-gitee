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
package com.gitee.icons;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

/**
 * @author Yuyou Chow
 *
 */
public class GiteeIcons {
	private static Icon load(String path) {
		return IconLoader.getIcon(path, GiteeIcons.class);
	}

	private static Icon load(String path, Class<?> clazz) {
		return IconLoader.getIcon(path, clazz);
	}

	public static final Icon DefaultAvatar_40 = load("/icons/defaultAvatar_40.svg"); // 40x40

	public static final Icon Gitee_icon = load("/icons/gitee.svg");
}
