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
package com.gitee.api.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.mandatory.Mandatory;
import org.jetbrains.io.mandatory.RestModel;

import java.util.Collections;
import java.util.List;

@RestModel
@SuppressWarnings("UnusedDeclaration")
public class GiteeErrorMessage {
  @Nullable
  public String message;

  @Nullable
  public String error;
  @Nullable
  public String errorDescription;

  private List<Error> errors;

//  public static class Error {
//    @Nullable
//    public String resource;
//    @Nullable
//    public String field;
//    @Nullable
//    public String code;
//    @Nullable
//    public String message;
//  }

  @Nullable
  public String getMessage() {
    return message;
  }

  @NotNull
  public List<Error> getErrors() {
    if (errors == null) return Collections.emptyList();
    return errors;
  }

  @Nullable
  public String getPresentableError() {
    if (errors == null) {
      return message;
    } else {
      StringBuilder s = new StringBuilder();
      s.append(message);
      for (Error e : errors) {
        s.append(String.format("<br/>[%s; %s]%s: %s", e.resource, e.field, e.code, e.message));
      }
      return s.toString();
    }
  }

  @RestModel
  public static class Error {
    @Mandatory
    private String resource;
    private String field;
    @Mandatory
    private String code;
    private String message;

    @NotNull
    public String getResource() {
      return resource;
    }

    @Nullable
    public String getField() {
      return field;
    }

    @NotNull
    public String getCode() {
      return code;
    }

    @Nullable
    public String getMessage() {
      return message;
    }
  }

  public boolean containsReasonMessage(@NotNull String reason) {
    if (message == null) return false;
    return message.contains(reason);
  }

  public boolean containsErrorCode(@NotNull String code) {
    if (errors == null) return false;
    for (Error error : errors) {
      if (error.code != null && error.code.contains(code)) return true;
    }
    return false;
  }

  public boolean containsErrorMessage(@NotNull String message) {
    if (errors == null) {
      if (error == null) return false;

      if (error.contains(message)) return true;

      if (errorDescription == null) return false;

      if (errorDescription.contains(message)) return true;
    } else {
      for (Error error : errors) {
        if (error.code != null && error.code.contains(message)) return true;
      }
    }

    return false;
  }
}
