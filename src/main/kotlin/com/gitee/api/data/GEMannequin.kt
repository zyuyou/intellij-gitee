// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data

class GEMannequin(id: String,
                  override val login: String,
                  override val url: String,
                  override val avatarUrl: String,
                  val name: String?)
  : GENode(id), GEActor
