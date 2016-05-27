package org.intellij.gitosc.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.intellij.gitosc.util.GitoscAuthData;
import org.intellij.gitosc.util.GitoscAuthDataHolder;
import org.intellij.gitosc.util.GitoscSettings;
import org.intellij.gitosc.util.GitoscUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;

/**
 * Created by zyuyou on 16/5/25.
 */
public class GitoscLoginDialog extends DialogWrapper {
	protected static final Logger LOG = GitoscUtil.LOG;

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

//		GitoscAuthData.BasicAuth basicAuth = oldAuthData.getBasicAuth();
//		if(basicAuth != null){
//			myGitoscLoginPanel.setLogin(basicAuth.getLogin());
//		}
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
			GitoscUtil.computeValueInModalIO(myProject, "Access to GitOSC", indicator ->
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
