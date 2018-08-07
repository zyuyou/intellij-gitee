// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.exceptions;

import com.gitee.authentication.accounts.GiteeAccount;
import com.gitee.authentication.accounts.GiteeAccount;
import org.jetbrains.annotations.NotNull;

public class GiteeMissingTokenException extends GiteeAuthenticationException {
  public GiteeMissingTokenException(@NotNull String message) {
    super(message);
  }

  public GiteeMissingTokenException(@NotNull GiteeAccount account) {
    this("Missing access token for account " + account);
  }
}
