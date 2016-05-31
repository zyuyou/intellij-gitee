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

package org.intellij.gitosc;

import com.google.common.base.Joiner;
import com.intellij.openapi.diagnostic.Logger;

/**
 * @author Yuyou Chow
 * */
public class GitoscConstants {
	// Logger
	public static final Logger LOG = Logger.getInstance("gitosc");

	// Utils
	public static final Joiner JOINER = Joiner.on("&").skipNulls();

	// String
	public static final String TITLE_ACCESS_TO_GITOSC = "Access to GitOSC";

	public static final String DEFAULT_GITOSC_HOST = "git.oschina.net";
}
