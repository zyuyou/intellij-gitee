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
package org.intellij.gitosc.api.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.io.mandatory.Mandatory;
import org.jetbrains.io.mandatory.RestModel;

@RestModel
@SuppressWarnings("UnusedDeclaration")
public class GitoscAuthorization {
//  {"access_token":"27b5de5bc956688c5215f2650e953d56",
// "token_type":"bearer",
// "expires_in":86400,
// "refresh_token":"51e98d42938c435922a92ce011103e258549a651a8e0fe169ef772c05002d805",
// "scope":"user_info",
// "created_at":1504439241}

  @Mandatory
  private String accessToken;
  private String tokenType;
  private String expiresIn;

  @Mandatory
  private String refreshToken;
  private String scope;
  private Integer createdAt;

  @NotNull
  public String getAccessToken() {
    return accessToken;
  }

  @NotNull
  public String getRefreshToken() {
    return refreshToken;
  }

  @NotNull
  public String getScope(){
    return scope;
  }

}
