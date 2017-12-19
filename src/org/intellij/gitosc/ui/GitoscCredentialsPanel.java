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
package org.intellij.gitosc.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.intellij.gitosc.GitoscBundle;
import org.intellij.gitosc.GitoscConstants;
import org.intellij.gitosc.api.GitoscApiUtil;
import org.intellij.gitosc.api.data.GitoscUser;
import org.intellij.gitosc.exceptions.GitoscAuthenticationException;
import org.intellij.gitosc.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.intellij.gitosc.util.GitoscAuthData.AuthType;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.IOException;

import static org.intellij.gitosc.GitoscConstants.TITLE_ACCESS_TO_GITOSC;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/ui/GitoscCredentialsPanel.java
 * @author JetBrains s.r.o.
 */
public class GitoscCredentialsPanel extends JPanel {
  private static final Logger LOG = GitoscConstants.LOG;

  private JTextField myHostTextField;
  private JTextField myLoginTextField;
  private JPasswordField myPasswordField;
  private JPasswordField myTokenField;

  private ComboBox<Layout> myAuthTypeComboBox;
  private JButton myCreateTokenButton;
  private JButton myTestButton;

  private JBLabel myAuthTypeLabel;
  private JTextPane mySignupTextField;
  private JPanel myPane;
  private JPanel myCardPanel;

  public GitoscCredentialsPanel(@NotNull Project project) {
    super(new BorderLayout());
    add(myPane, BorderLayout.CENTER);

    mySignupTextField.setText("<html>Do not have an account at gitee.com? <a href=\"https://gitee.com\">" + "Sign up" + "</a></html>");
    mySignupTextField.setBackground(UIUtil.TRANSPARENT_COLOR);
    mySignupTextField.setCursor(new Cursor(Cursor.HAND_CURSOR));
    mySignupTextField.setMargin(JBUI.insetsTop(5));
    mySignupTextField.addHyperlinkListener(new HyperlinkAdapter() {
      @Override
      protected void hyperlinkActivated(HyperlinkEvent e) {
        BrowserUtil.browse(e.getURL());
      }
    });

    myAuthTypeLabel.setBorder(JBUI.Borders.emptyLeft(10));
    myAuthTypeComboBox.addItem(Layout.TOKEN);
    myAuthTypeComboBox.addItem(Layout.PASSWORD);

    myTestButton.addActionListener(e -> testAuthData(project));

    myCreateTokenButton.addActionListener(e -> generateToken(project));

    myAuthTypeComboBox.addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        Layout item = (Layout)e.getItem();
        CardLayout cardLayout = (CardLayout)myCardPanel.getLayout();
        cardLayout.show(myCardPanel, item.getCard());
      }
    });
  }

  @NotNull
  public String getHost() {
    return myHostTextField.getText().trim();
  }

  @NotNull
  public String getLogin() {
    return myLoginTextField.getText().trim();
  }

  @NotNull
  private String getPassword() {
    return String.valueOf(myPasswordField.getPassword());
  }

  @NotNull
  private String getToken() {
    return String.valueOf(myTokenField.getPassword());
  }

  @NotNull
  public AuthType getAuthType() {
    Layout selected = (Layout)myAuthTypeComboBox.getSelectedItem();

    if (selected == Layout.PASSWORD)
      return AuthType.SESSION;

    if (selected == Layout.TOKEN)
      return AuthType.TOKEN;

    LOG.error("GitoscSettingsPanel: illegal selection - " + selected);

    return AuthType.TOKEN;
  }

  @NotNull
  public GitoscAuthData getAuthData() {
    AuthType type = getAuthType();

    switch (type) {
      case SESSION:
        return GitoscAuthData.createSessionAuth(getHost(), getLogin(), getPassword());
//      case BASIC:
//        return GitoscAuthData.createBasicAuth(getHost(), getLogin(), getPassword());
      case TOKEN:
        return GitoscAuthData.createTokenAuth(getHost(), StringUtil.trim(getToken()));
      default:
        throw new IllegalStateException();
    }
  }

  public void setHost(@NotNull String host) {
    myHostTextField.setText(host);
  }

  public void setLogin(@Nullable String login) {
    myLoginTextField.setText(login);
  }

  public void setPassword(@NotNull String password) {
    myPasswordField.setText(password);
  }

  public void setToken(@NotNull String token) {
    myTokenField.setText(token);
  }

  public void setAuthType(@NotNull GitoscAuthData.AuthType type) {
    if (type == GitoscAuthData.AuthType.SESSION) {
      myAuthTypeComboBox.setSelectedItem(Layout.PASSWORD);
    }else {
      myAuthTypeComboBox.setSelectedItem(Layout.TOKEN);
    }
  }

  public void setAuthData(@NotNull GitoscAuthData authData) {
    AuthType type = authData.getAuthType();
    setAuthType(type);
    setHost(authData.getHost());
    if(type == AuthType.SESSION) {
      GitoscAuthData.SessionAuth sessionAuth = authData.getSessionAuth();
      assert sessionAuth != null;
      setLogin(sessionAuth.getLogin());
      setPassword(sessionAuth.getPassword());
    }
    if (type == AuthType.BASIC) {
      GitoscAuthData.BasicAuth basicAuth = authData.getBasicAuth();
      assert basicAuth != null;
      setLogin(basicAuth.getLogin());
      setPassword(basicAuth.getPassword());
    }
    if (type == AuthType.TOKEN) {
      GitoscAuthData.TokenAuth tokenAuth = authData.getTokenAuth();
      assert tokenAuth != null;
      setToken(tokenAuth.getToken());
    }
  }

  public void lockAuthType(@NotNull AuthType type) {
    setAuthType(type);
    myAuthTypeComboBox.setEnabled(false);
  }

  public void lockHost(@NotNull String host) {
    setHost(host);
    myHostTextField.setEnabled(false);
  }

  public void setTestButtonVisible(boolean visible) {
    myTestButton.setVisible(visible);
  }

  private void testAuthData(@NotNull Project project) {
    try {
      GitoscAuthData auth = getAuthData();
      GitoscUser user = GitoscUtil.computeValueInModalIO(project, TITLE_ACCESS_TO_GITOSC, indicator ->
        GitoscUtil.checkAuthData(project, new GitoscAuthDataHolder(auth), indicator));

      if (AuthType.TOKEN.equals(auth.getAuthType())) {
        GitoscNotifications.showInfoDialog(myPane, "Success", "Connection successful for user " + user.getLogin());
      }
      else {
        GitoscNotifications.showInfoDialog(myPane, "Success", "Connection successful");
      }
    }
    catch (GitoscAuthenticationException ex) {
      GitoscNotifications.showErrorDialog(myPane, "Login Failure", "Can't login using given credentials: ", ex);
    }
    catch (IOException ex) {
      GitoscNotifications.showErrorDialog(myPane, "Login Failure", "Can't login: ", ex);
    }
  }

  private void generateToken(@NotNull Project project) {
    try {
      String newToken = GitoscUtil.computeValueInModalIO(project, TITLE_ACCESS_TO_GITOSC, indicator ->
        GitoscUtil.runTask(project, GitoscAuthDataHolder.createFromSettings(), indicator, AuthLevel.basicOnetime(getHost()), connection ->
          GitoscApiUtil.getMasterToken(connection, "IntelliJ plugin")));

      myTokenField.setText(newToken);
    }
    catch (IOException ex) {
      GitoscNotifications.showErrorDialog(myPane, "Can't Create API Token", ex);
    }
  }

  private enum Layout {
    PASSWORD("Password"),
    TOKEN("Token");

    @NotNull
    private final String myCard;

    Layout(@NotNull String card) {
      myCard = card;
    }

    @NotNull
    public String getCard() {
      return myCard;
    }

    @Override
    public String toString() {
      return myCard;
    }
  }
}
