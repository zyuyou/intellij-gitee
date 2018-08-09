package com.gitee.tasks;

import com.gitee.api.GiteeApiRequestExecutor;
import com.gitee.authentication.ui.GiteeLoginDialog;
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

/**
 * @author Dennis.Ushakov
 */
public class GiteeRepositoryEditor extends BaseRepositoryEditor<GiteeRepository> {
	private MyTextField myRepoAuthor;
	private MyTextField myRepoName;
	private MyTextField myAccessToken;
	private MyTextField myRefreshToken;
	private JBCheckBox myShowNotAssignedIssues;
	private JButton myTokenButton;
	private JBLabel myHostLabel;
	private JBLabel myRepositoryLabel;
	private JBLabel myTokenLabel;

	public GiteeRepositoryEditor(final Project project, final GiteeRepository repository, Consumer<GiteeRepository> changeListener) {
		super(project, repository, changeListener);
		myUrlLabel.setVisible(false);
		myUsernameLabel.setVisible(false);
		myUserNameText.setVisible(false);
		myPasswordLabel.setVisible(false);
		myPasswordText.setVisible(false);
		myUseHttpAuthenticationCheckBox.setVisible(false);

		myRepoAuthor.setText(repository.getRepoAuthor());
		myRepoName.setText(repository.getRepoName());
		myAccessToken.setText(repository.getAccessToken());
		myRefreshToken.setText(repository.getRefreshToken());
		myShowNotAssignedIssues.setSelected(!repository.isAssignedIssuesOnly());

		DocumentListener buttonUpdater = new DocumentAdapter() {
			@Override
			protected void textChanged(DocumentEvent e) {
				updateTokenButton();
			}
		};

		myURLText.getDocument().addDocumentListener(buttonUpdater);
		myRepoAuthor.getDocument().addDocumentListener(buttonUpdater);
		myRepoName.getDocument().addDocumentListener(buttonUpdater);
	}

	@Nullable
	@Override
	protected JComponent createCustomPanel() {
		myHostLabel = new JBLabel("Host:", SwingConstants.RIGHT);

		JPanel myHostPanel = new JPanel(new BorderLayout(5, 0));
		myHostPanel.add(myURLText, BorderLayout.CENTER);
		myHostPanel.add(myShareUrlCheckBox, BorderLayout.EAST);

		myRepositoryLabel = new JBLabel("Repository:", SwingConstants.RIGHT);
		myRepoAuthor = new MyTextField("Repository Owner");
		myRepoName = new MyTextField("Repository Name");
		myRepoAuthor.setPreferredSize("SomelongNickname");
		myRepoName.setPreferredSize("SomelongReponame-with-suffixes");

		JPanel myRepoPanel = new JPanel(new GridBagLayout());
		GridBag bag = new GridBag().setDefaultWeightX(1).setDefaultFill(GridBagConstraints.HORIZONTAL);
		myRepoPanel.add(myRepoAuthor, bag.nextLine().next());
		myRepoPanel.add(new JLabel("/"), bag.next().fillCellNone().insets(0, 5, 0, 5).weightx(0));
		myRepoPanel.add(myRepoName, bag.next());

		myTokenLabel = new JBLabel("API Token:", SwingConstants.RIGHT);
		myAccessToken = new MyTextField("OAuth2 access token");
		myRefreshToken = new MyTextField("OAuth2 refresh token");
		myTokenButton = new JButton("Create API token");
		myTokenButton.addActionListener(e -> {
			generateToken();
			doApply();
		});

		JPanel myTokenPanel = new JPanel();
		myTokenPanel.setLayout(new BorderLayout(5, 5));
		myTokenPanel.add(myAccessToken, BorderLayout.CENTER);
//		myTokenPanel.add(myRefreshToken, BorderLayout.CENTER);
		myTokenPanel.add(myTokenButton, BorderLayout.EAST);

		myShowNotAssignedIssues = new JBCheckBox("Include issues not assigned to me");

		installListener(myRepoAuthor);
		installListener(myRepoName);
		installListener(myAccessToken);
		installListener(myRefreshToken);
		installListener(myShowNotAssignedIssues);

		return FormBuilder.createFormBuilder()
			.setAlignLabelOnRight(true)
			.addLabeledComponent(myHostLabel, myHostPanel)
			.addLabeledComponent(myRepositoryLabel, myRepoPanel)
			.addLabeledComponent(myTokenLabel, myTokenPanel)
			.addComponentToRightColumn(myShowNotAssignedIssues)
			.getPanel();
	}

	@Override
	public void apply() {
		myRepository.setRepoName(getRepoName());
		myRepository.setRepoAuthor(getRepoAuthor());
		myRepository.setTokens(getAccessToken(), getRefreshToken());
		myRepository.setAssignedIssuesOnly(isAssignedIssuesOnly());
		super.apply();
	}

	private void generateToken() {
		GiteeLoginDialog dialog = new GiteeLoginDialog(GiteeApiRequestExecutor.Factory.getInstance(), myProject);
		dialog.withServer(getHost(), false);
//    dialog.setClientName("Tasks Plugin");

		if (dialog.showAndGet()) {
			myAccessToken.setText(dialog.getAccessToken());
			myRefreshToken.setText(dialog.getRefreshToken());
		}
	}

	@Override
	public void setAnchor(@Nullable final JComponent anchor) {
		super.setAnchor(anchor);
		myHostLabel.setAnchor(anchor);
		myRepositoryLabel.setAnchor(anchor);
		myTokenLabel.setAnchor(anchor);
	}

	private void updateTokenButton() {
		if (StringUtil.isEmptyOrSpaces(getHost()) || StringUtil.isEmptyOrSpaces(getRepoAuthor()) || StringUtil.isEmptyOrSpaces(getRepoName())) {
			myTokenButton.setEnabled(false);
		} else {
			myTokenButton.setEnabled(true);
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
