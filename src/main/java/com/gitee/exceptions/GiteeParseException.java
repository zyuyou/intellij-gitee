// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.exceptions;

import org.jetbrains.annotations.NotNull;

public class GiteeParseException extends RuntimeException {
  public GiteeParseException(@NotNull String message) {
    super(message);
  }
}
