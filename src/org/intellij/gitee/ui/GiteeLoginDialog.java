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
package org.intellij.gitee.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.intellij.gitee.GiteeConstants;
import org.intellij.gitee.GiteeBundle;
import org.intellij.gitee.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/ui/GithubLoginDialog.java
 * @author JetBrains s.r.o.
 * @author oleg
 * @date 10/20/10
 */
public class GiteeLoginDialog extends DialogWrapper {
	private static final Logger LOG = GiteeConstants.LOG;

	private final GiteeCredentialsPanel myCredentialsPanel;

	@NotNull private final Project myProject;
	@NotNull private final AuthLevel myAuthLevel;

	private GiteeAuthData myAuthData;
	private boolean mySavePassword;

	public GiteeLoginDialog(@NotNull Project project, @NotNull GiteeAuthData oldAuthData, @NotNull AuthLevel authLevel) {
		super(project, true);
		myProject = project;
		myAuthLevel = authLevel;

		myCredentialsPanel = new GiteeCredentialsPanel(project);
		myCredentialsPanel.setTestButtonVisible(false);

		myCredentialsPanel.setHost(oldAuthData.getHost());
		myCredentialsPanel.setAuthType(oldAuthData.getAuthType());

		GiteeAuthData.SessionAuth sessionAuth = oldAuthData.getSessionAuth();
		if(sessionAuth != null){
			myCredentialsPanel.setLogin(sessionAuth.getLogin());
		}

		if (authLevel.getHost() != null) myCredentialsPanel.lockHost(authLevel.getHost());
		if (authLevel.getAuthType() != null) myCredentialsPanel.lockAuthType(authLevel.getAuthType());

		if (!authLevel.isOnetime()) setDoNotAskOption(new MyRememberPasswordOption());

		setTitle(GiteeBundle.message2("gitee.login.dialog.title"));
		setOKButtonText("Login");

		init();
	}

	@NotNull
	protected Action[] createActions(){
		return new Action[]{
			getOKAction(),
			getCancelAction(),
			getHelpAction()
		};
	}

	@Nullable
	@Override
	protected JComponent createCenterPanel() {
		return myCredentialsPanel;
	}

	@Nullable
	@Override
	protected String getHelpId() {
		return "login_to_gitee";
	}

	@Override
	protected void doOKAction() {
		GiteeAuthDataHolder authHolder = new GiteeAuthDataHolder(myCredentialsPanel.getAuthData());
		try {
			if(authHolder.getAuthData().getAuthType() == GiteeAuthData.AuthType.SESSION){
				// Session for login
				GiteeUtil.computeValueInModalIO(myProject, GiteeConstants.TITLE_ACCESS_TO_GITEE, indicator ->
					GiteeUtil.loginAuthData(myProject, authHolder, indicator));
			}else{
				GiteeUtil.computeValueInModalIO(myProject, GiteeConstants.TITLE_ACCESS_TO_GITEE, indicator ->
					GiteeUtil.checkAuthData(myProject, authHolder, indicator));
			}

			myAuthData = authHolder.getAuthData();

			super.doOKAction();
		}
		catch (IOException e) {
			LOG.info(e);
			setErrorText("Can't login: " + GiteeUtil.getErrorTextFromException(e), myCredentialsPanel);
		}
	}

	public boolean isSavePasswordSelected() {
		return mySavePassword;
	}

	@NotNull
	public GiteeAuthData getAuthData(){
		if(myAuthData == null){
			throw new IllegalStateException("AuthData is not set");
		}
		return myAuthData;
	}

	private class MyRememberPasswordOption implements DoNotAskOption {
		@Override
		public boolean isToBeShown() {
			return !GiteeSettings.getInstance().isSavePassword();
		}

		@Override
		public void setToBeShown(boolean toBeShown, int exitCode) {
			mySavePassword = !toBeShown;
			GiteeSettings.getInstance().setSavePassword(!toBeShown);
		}

		@Override
		public boolean canBeHidden() {
			return GiteeSettings.getInstance().isSavePasswordMakesSense();
		}

		@Override
		public boolean shouldSaveOptionsOnCancel() {
			return false;
		}

		@NotNull
		@Override
		public String getDoNotShowMessage() {
			return "Save credentials";
		}
	}
}
