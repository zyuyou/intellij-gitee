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

package com.gitee.tasks;

import com.gitee.authentication.GECredentials;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.tasks.config.BaseRepositoryEditor;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.Consumer;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.GridBag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

import static com.gitee.i18n.GiteeBundle.message;

/**
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/tasks/GithubRepositoryEditor.java
 * @author Dennis.Ushakov
 */
public class GiteeRepositoryEditor extends BaseRepositoryEditor<GiteeRepository> {
	private MyTextField myRepoAuthor;
	private MyTextField myRepoName;

	private MyTextField myAccessToken;
	private MyTextField myRefreshToken;

	private JBCheckBox myShowNotAssignedIssues;
	private JButton myCredentialsButton;
	private JBLabel myHostLabel;
	private JBLabel myRepositoryLabel;
	private JBLabel myTokenLabel;

	private long myCrendentialsExpiresIn;
	private String myCrendentialsTokenType;
	private String myCrendentialsScope;
	private long myCrendentialsCreatedAt;

	public GiteeRepositoryEditor(final Project project, final GiteeRepository repository, Consumer<? super GiteeRepository> changeListener) {
		super(project, repository, changeListener);

		GECredentials credentials = repository.getDeserializeCredentials();

		myUrlLabel.setVisible(false);
		myUsernameLabel.setVisible(false);
		myUserNameText.setVisible(false);
		myPasswordLabel.setVisible(false);
		myPasswordText.setVisible(false);
		myUseHttpAuthenticationCheckBox.setVisible(false);

		myRepoAuthor.setText(repository.getRepoAuthor());
		myRepoName.setText(repository.getRepoName());
		myAccessToken.setEditable(false);
		myAccessToken.setText(credentials.getAccessToken());
		myRefreshToken.setEditable(false);
		myRefreshToken.setText(credentials.getRefreshToken());
		myShowNotAssignedIssues.setSelected(!repository.isAssignedIssuesOnly());

		DocumentListener buttonUpdater = new DocumentAdapter() {
			@Override
			protected void textChanged(@NotNull DocumentEvent e) {
				updateCredentialsButton();
			}
		};

		myURLText.getDocument().addDocumentListener(buttonUpdater);
		myRepoAuthor.getDocument().addDocumentListener(buttonUpdater);
		myRepoName.getDocument().addDocumentListener(buttonUpdater);

		myCrendentialsExpiresIn = credentials.getExpiresIn();
		myCrendentialsTokenType = credentials.getTokenType();
		myCrendentialsScope = credentials.getScope();
		myCrendentialsCreatedAt = credentials.getCreatedAt();
	}

	@Override
	protected void afterTestConnection(boolean connectionSuccessful) {
		super.afterTestConnection(connectionSuccessful);

		if (connectionSuccessful) {
			GECredentials credentials = myRepository.getDeserializeCredentials();

			myAccessToken.setText(credentials.getAccessToken());
			myRefreshToken.setText(credentials.getRefreshToken());
		}
	}

	@Nullable
	@Override
	protected JComponent createCustomPanel() {
		myHostLabel = new JBLabel(message("task.repo.host.field"), SwingConstants.RIGHT);

		JPanel myHostPanel = new JPanel(new BorderLayout(5, 0));
		myHostPanel.add(myURLText, BorderLayout.CENTER);
		myHostPanel.add(myShareUrlCheckBox, BorderLayout.EAST);

		myRepositoryLabel = new JBLabel(message("task.repo.repository.field"), SwingConstants.RIGHT);
		myRepoAuthor = new MyTextField(message("task.repo.owner.field.empty.hint"));
		myRepoName = new MyTextField(message("task.repo.name.field.empty.hint"));
		myRepoAuthor.setPreferredSize("SomelongNickname");
		myRepoName.setPreferredSize("SomelongReponame-with-suffixes");

		JPanel myRepoPanel = new JPanel(new GridBagLayout());
		GridBag bag = new GridBag().setDefaultWeightX(1).setDefaultFill(GridBagConstraints.HORIZONTAL);
		myRepoPanel.add(myRepoAuthor, bag.nextLine().next());
		myRepoPanel.add(new JLabel("/"), bag.next().fillCellNone().insets(0, 5, 0, 5).weightx(0));
		myRepoPanel.add(myRepoName, bag.next());

		myTokenLabel = new JBLabel(message("task.repo.token.field"), SwingConstants.RIGHT);
		myAccessToken = new MyTextField(message("task.repo.access.token.field.empty.hint"));
		myRefreshToken = new MyTextField(message("task.repo.refresh.token.field.empty.hint"));
		myCredentialsButton = new JButton(message("task.repo.credentials.create.button"));
		myCredentialsButton.addActionListener(e -> {
			generateCredentials();
			doApply();
		});

		JPanel myCredentialsPanel = new JPanel();
		myCredentialsPanel.setLayout(new GridLayout(1, 2, 5, 0));
		myCredentialsPanel.add(myAccessToken);
		myCredentialsPanel.add(myRefreshToken);

		myShowNotAssignedIssues = new JBCheckBox("Include issues not assigned to me");

		JPanel myOthersPanel = new JPanel();
		myOthersPanel.setLayout(new BorderLayout(5, 5));
		myOthersPanel.add(myShowNotAssignedIssues, BorderLayout.CENTER);
		myOthersPanel.add(myCredentialsButton, BorderLayout.EAST);

		installListener(myRepoAuthor);
		installListener(myRepoName);
		installListener(myAccessToken);
		installListener(myRefreshToken);
		installListener(myShowNotAssignedIssues);

		return FormBuilder.createFormBuilder()
        .setAlignLabelOnRight(true)
				.addLabeledComponent(myHostLabel, myHostPanel)
				.addLabeledComponent(myRepositoryLabel, myRepoPanel)
        .addLabeledComponent(myTokenLabel, myCredentialsPanel)
        .addComponentToRightColumn(myOthersPanel)
        .getPanel();
	}

	@Override
	public void apply() {
		super.apply();
		myRepository.setRepoName(getRepoName());
		myRepository.setRepoAuthor(getRepoAuthor());
		myRepository.setSerializeCredentials(
				new GECredentials(
						getAccessToken(),
						getRefreshToken(),
						myCrendentialsExpiresIn,
						myCrendentialsTokenType,
						myCrendentialsScope,
						myCrendentialsCreatedAt
				)
		);
		myRepository.storeCredentials();
		myRepository.setAssignedIssuesOnly(isAssignedIssuesOnly());
	}

	private void generateCredentials() {
		GECredentials credentials = GERepositoryEditorKt.INSTANCE.askCredentials(myProject, getHost());
		if(credentials != null) {
			myAccessToken.setText(credentials.getAccessToken());
			myRefreshToken.setText(credentials.getRefreshToken());

			myCrendentialsExpiresIn = credentials.getExpiresIn();
			myCrendentialsTokenType = credentials.getTokenType();
			myCrendentialsScope = credentials.getScope();
			myCrendentialsCreatedAt = credentials.getCreatedAt();
		}
	}

	@Override
	public void setAnchor(@Nullable final JComponent anchor) {
		super.setAnchor(anchor);
		myHostLabel.setAnchor(anchor);
		myRepositoryLabel.setAnchor(anchor);
		myTokenLabel.setAnchor(anchor);
	}

	private void updateCredentialsButton() {
		if (StringUtil.isEmptyOrSpaces(getHost()) ||
				StringUtil.isEmptyOrSpaces(getRepoAuthor()) ||
				StringUtil.isEmptyOrSpaces(getRepoName())) {

			myCredentialsButton.setEnabled(false);
		} else {
			myCredentialsButton.setEnabled(true);
		}
	}

	@NotNull
	private String getHost() {
		return myURLText.getText().trim();
	}

	@NotNull
	private String getRepoAuthor() {
		return myRepoAuthor.getText().trim();
	}

	@NotNull
	private String getRepoName() {
		return myRepoName.getText().trim();
	}

	@NotNull
	private String getAccessToken() {
		return myAccessToken.getText().trim();
	}

	@NotNull
	private String getRefreshToken() {
		return myRefreshToken.getText().trim();
	}

	private boolean isAssignedIssuesOnly() {
		return !myShowNotAssignedIssues.isSelected();
	}

	public static class MyTextField extends JBTextField {
		private int myWidth = -1;

		public MyTextField(@NotNull String hintCaption) {
			getEmptyText().setText(hintCaption);
		}

		public void setPreferredSize(@NotNull String sampleSizeString) {
			myWidth = getFontMetrics(getFont()).stringWidth(sampleSizeString);
		}

		@Override
		public Dimension getPreferredSize() {
			Dimension size = super.getPreferredSize();
			if (myWidth != -1) {
				size.width = myWidth;
			}
			return size;
		}
	}
}
