// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.intellij.collaboration.api.dto.GraphQLFragment

@GraphQLFragment("/graphql/fragment/repositoryOwnerName.graphql")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "__typename", visible = false)
@JsonSubTypes(
  JsonSubTypes.Type(name = "User", value = GERepositoryOwnerName.User::class),
  JsonSubTypes.Type(name = "Organization", value = GERepositoryOwnerName.Organization::class)
)
interface GERepositoryOwnerName {
  val login: String

  class User(override val login: String) : GERepositoryOwnerName
  class Organization(override val login: String) : GERepositoryOwnerName
}