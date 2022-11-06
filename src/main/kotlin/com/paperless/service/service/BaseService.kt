package com.paperless.service.service

import com.paperless.service.response.ErrorResponse
import com.paperless.service.response.Response
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.r2dbc.core.DatabaseClient

abstract class BaseService (val databaseClient: DatabaseClient){
    suspend fun <T>execute(body : suspend ()-> T) : Flow<Response<out T>> = flow {
        kotlin.runCatching {
            body()
        }.fold({
            emit(Response.Success(it))
        },{
            it.printStackTrace()
            emit(Response.Error(data=null, message = ErrorResponse(200,"${it.message}")))
        })
    }
}