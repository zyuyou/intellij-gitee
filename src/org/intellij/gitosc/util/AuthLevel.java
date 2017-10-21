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
package org.intellij.gitosc.util;

import com.google.common.base.MoreObjects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/util/AuthLevel.java
 * @author JetBrains s.r.o.
 */
public class AuthLevel {
  public static final AuthLevel ANY = new AuthLevel(null, null);
  public static final AuthLevel TOKEN = new AuthLevel(null, GitoscAuthData.AuthType.TOKEN);
  public static final AuthLevel BASIC = new AuthLevel(null, GitoscAuthData.AuthType.BASIC);

  public static final AuthLevel LOGGED = new AuthLevel(null, null) {
    @Override
    public boolean accepts(@NotNull GitoscAuthData auth) {
      return auth.getAuthType() != GitoscAuthData.AuthType.ANONYMOUS;
    }

    @Override
    public String toString() {
      return "Not anonymous";
    }
  };

  @NotNull
  public static AuthLevel basicOnetime(@NotNull String host) {
    return new AuthLevel(host, GitoscAuthData.AuthType.SESSION) {
      @Override
      public boolean isOnetime() {
        return true;
      }
    };
  }


  @Nullable
  private final String myHost;
  @Nullable
  private final GitoscAuthData.AuthType myAuthType;

  private AuthLevel(@Nullable String host, @Nullable GitoscAuthData.AuthType authType) {
    myHost = host;
    myAuthType = authType;
  }

  @Nullable
  public String getHost() {
    return myHost;
  }

  @Nullable
  public GitoscAuthData.AuthType getAuthType() {
    return myAuthType;
  }

  public boolean accepts(@NotNull GitoscAuthData auth) {
    if (myHost != null && !myHost.equals(auth.getHost())) return false;
    if (myAuthType != null && !myAuthType.equals(auth.getAuthType())) return false;

    return true;
  }

  public boolean isOnetime() {
    return false;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("authType", myAuthType).add("host", myHost).toString();
  }
}
