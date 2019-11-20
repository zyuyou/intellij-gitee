// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "__typename", visible = false,
              defaultImpl = GEActor::class)
@JsonSubTypes(
  JsonSubTypes.Type(name = "User", value = GEUser::class),
  JsonSubTypes.Type(name = "Bot", value = GEBot::class),
  JsonSubTypes.Type(name = "Mannequin", value = GEMannequin::class),
  JsonSubTypes.Type(name = "Organization", value = GEOrganization::class)
)
interface GEActor {
  val login: String
  val url: String
  val avatarUrl: String
}