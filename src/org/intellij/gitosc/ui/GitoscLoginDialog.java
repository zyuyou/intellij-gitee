/*
 * Copyright 2013-2016 Yuyou Chow
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
package org.intellij.gitosc.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.intellij.gitosc.GitoscConstants;
import org.intellij.gitosc.util.GitoscAuthData;
import org.intellij.gitosc.util.GitoscAuthDataHolder;
import org.intellij.gitosc.util.GitoscSettings;
import org.intellij.gitosc.util.GitoscUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;

import static org.intellij.gitosc.GitoscConstants.LOG;

/**
 * https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/ui/GithubLoginDialog.java
 */
public class GitoscLoginDialog extends DialogWrapper {
	protected final GitoscLoginPanel myGitoscLoginPanel;
	protected final GitoscSettings mySettings;

	protected final Project myProject;

	protected GitoscAuthData myAuthData;

	public GitoscLoginDialog(@Nullable Project project, @NotNull GitoscAuthData oldAuthData) {
		super(project, true);

		myProject = project;

		myGitoscLoginPanel = new GitoscLoginPanel(this);

		myGitoscLoginPanel.setHost(oldAuthData.getHost());
		myGitoscLoginPanel.setAuthType(oldAuthData.getAuthType());

		GitoscAuthData.SessionAuth sessionAuth = oldAuthData.getSessionAuth();
		if(sessionAuth != null){
			myGitoscLoginPanel.setLogin(sessionAuth.getLogin());
		}

		mySettings = GitoscSettings.getInstance();
		if(mySettings.isSavePasswordMakesSense()){
			myGitoscLoginPanel.setSavePasswordSelected(mySettings.isSavePassword());
		}else{
			myGitoscLoginPanel.setSavePasswordVisibleEnabled(false);
		}

		setTitle("Login to GitOSC");
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
		return myGitoscLoginPanel.getPanel();
	}

	@Nullable
	@Override
	protected String getHelpId() {
		return "login_to_gitosc";
	}

	@Nullable
	@Override
	public JComponent getPreferredFocusedComponent() {
		return myGitoscLoginPanel.getPreferableFocusComponent();
	}

	@Override
	protected void doOKAction() {
		final GitoscAuthDataHolder authDataHolder = new GitoscAuthDataHolder(myGitoscLoginPanel.getAuthData());
		try{
			GitoscUtil.computeValueInModalIO(myProject, GitoscConstants.TITLE_ACCESS_TO_GITOSC, indicator ->
				GitoscUtil.checkAuthData(myProject, authDataHolder, indicator));

			myAuthData = authDataHolder.getAuthData();

			if(mySettings.isSavePasswordMakesSense()){
				mySettings.setSavePassword(myGitoscLoginPanel.isSavePasswordSelected());
			}
			super.doOKAction();
		}catch(IOException e){
			LOG.info(e);
			setErrorText("Can't login: " + GitoscUtil.getErrorTextFromException(e));
		}
	}

	public boolean isSavePasswordSelected(){
		return myGitoscLoginPanel.isSavePasswordSelected();
	}

	@NotNull
	public GitoscAuthData getAuthData(){
		if(myAuthData == null){
			throw new IllegalStateException("AuthData is not set");
		}
		return myAuthData;
	}

	public void clearErrors(){
		setErrorText(null);
	}
}
