package com.gitee.exceptions

import java.io.IOException

sealed class GEAuthException(message: String) : IOException(message)

class GEAccessTokenExpiredException(message: String) : GEAuthException(message)

class GEAccessTokennRequiredException(message: String) : GEAuthException(message)

class GEInvalidGrantException(message: String) : GEAuthException(message)

class GEUnknownAuthException(message: String) : GEAuthException(message)
