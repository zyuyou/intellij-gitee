package com.gitee.api.data

data class GiteeSearchResult<T> constructor(var items: List<T>,
                                            private val totalCount: Int,
                                            private val incompleteResults: Boolean) {
}