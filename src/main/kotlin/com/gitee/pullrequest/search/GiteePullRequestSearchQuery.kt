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
package com.gitee.pullrequest.search

import com.gitee.api.data.GiteeIssueState
import com.gitee.api.search.GiteePullRequestsSearchSort
import com.gitee.api.util.GiteeApiSearchQueryBuilder
import com.gitee.api.util.GiteeApiUrlQueryBuilder
import java.text.ParseException
import java.text.SimpleDateFormat

/**
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/pullrequest/search/GithubPullRequestSearchQuery.kt
 * @author JetBrains s.r.o.
 */
internal class GiteePullRequestSearchQuery(private val terms: List<Term<*>>) {
  fun buildApiSearchQuery(searchQueryBuilder: GiteeApiSearchQueryBuilder) {
    for (term in terms) {
      when (term) {
        is Term.QueryPart -> {
          searchQueryBuilder.query(term.apiValue)
        }
        is Term.Qualifier -> {
          searchQueryBuilder.qualifier(term.apiName, term.apiValue)
        }
      }
    }
  }

  fun buildApiSearchQuery(searchQueryBuilder: GiteeApiUrlQueryBuilder) {
    for (term in terms) {
      when (term) {
        is Term.QueryPart -> {
          searchQueryBuilder.query(term.apiValue)
        }
        is Term.Qualifier -> {
          searchQueryBuilder.param(term.apiName, term.apiValue)
        }
      }
    }
  }

  fun isEmpty() = terms.isEmpty()

  companion object {
    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd")

    fun parseFromString(string: String): GiteePullRequestSearchQuery {
      val result = mutableListOf<Term<*>>()
      val terms = string.split(' ')
      for (term in terms) {
        if (term.isEmpty()) continue

        val colonIdx = term.indexOf(':')
        if (colonIdx < 0) {
          result.add(Term.QueryPart(term))
        }
        else {
          try {
            result.add(QualifierName.valueOf(term.substring(0, colonIdx)).createTerm(term.substring(colonIdx + 1)))
          }
          catch (e: IllegalArgumentException) {
            result.add(Term.QueryPart(term))
          }
        }
      }
      return GiteePullRequestSearchQuery(result)
    }
  }

  @Suppress("EnumEntryName")
  enum class QualifierName(val apiName: String) {
    state("state") {
      override fun createTerm(value: String) = Term.Qualifier.Enum.from<GiteeIssueState>(this, value)
    },
    assignee("assignee") {
      override fun createTerm(value: String) = Term.Qualifier.Simple(this, value)
    },
    author("author") {
      override fun createTerm(value: String) = Term.Qualifier.Simple(this, value)
    },
    after("created") {
      override fun createTerm(value: String) = Term.Qualifier.Date.After.from(this, value)
    },
    before("created") {
      override fun createTerm(value: String) = Term.Qualifier.Date.Before.from(this, value)
    },
    sortBy("sort") {
      override fun createTerm(value: String) = Term.Qualifier.Enum.from<GiteePullRequestsSearchSort>(this, value)
    };

    abstract fun createTerm(value: String): Term<*>
  }

  /**
   * Part of search query (search term)
   */
  sealed class Term<T : Any>(protected val value: T) {
    abstract val apiValue: String?

    class QueryPart(value: String) : Term<String>(value) {
      override val apiValue = this.value
    }

    sealed class Qualifier<T : Any>(name: QualifierName, value: T) : Term<T>(value) {
      val apiName: String = name.apiName

      class Simple(name: QualifierName, value: String) : Qualifier<String>(name, value) {
        override val apiValue = this.value
      }

      class Enum<T : kotlin.Enum<T>>(name: QualifierName, value: T) : Qualifier<kotlin.Enum<T>>(name, value) {
        override val apiValue = this.value.name

        companion object {
          inline fun <reified T : kotlin.Enum<T>> from(name: QualifierName, value: String): Term<*> {
            return try {
              Qualifier.Enum(name, enumValueOf<T>(value))
            }
            catch (e: IllegalArgumentException) {
              Qualifier.Simple(name, value)
            }
          }
        }
      }

      sealed class Date(name: QualifierName, value: java.util.Date) : Qualifier<java.util.Date>(name, value) {
        protected fun formatDate(): String = DATE_FORMAT.format(this.value)

        class Before(name: QualifierName, value: java.util.Date) : Date(name, value) {
          override val apiValue = "<${formatDate()}"

          companion object {
            fun from(name: QualifierName, value: String): Term<*> {
              val date = try {
                DATE_FORMAT.parse(value)
              }
              catch (e: ParseException) {
                return Qualifier.Simple(name, value)
              }
              return Qualifier.Date.Before(name, date)
            }
          }
        }

        class After(name: QualifierName, value: java.util.Date) : Date(name, value) {
          override val apiValue = ">${formatDate()}"

          companion object {
            fun from(name: QualifierName, value: String): Term<*> {
              return try {
                Qualifier.Date.After(name, DATE_FORMAT.parse(value))
              }
              catch (e: ParseException) {
                Qualifier.Simple(name, value)
              }
            }
          }
        }
      }
    }
  }
}