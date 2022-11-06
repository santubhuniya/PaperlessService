package com.paperless.service.response

sealed class Response<T>(val data: T? = null, val error: ErrorResponse? = null) {
    class Success<T>(data: T) : Response<T>(data)
    class Error<T>(data: T? = null, message: ErrorResponse?) : Response<T>(data, message)
}

data class ErrorResponse(
    val errorCode: Int,
    val errorMessage: String
)