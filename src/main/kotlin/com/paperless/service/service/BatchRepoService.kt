package com.paperless.service.service

import com.paperless.service.response.GoalDto


interface BatchRepoService {
    suspend fun getGoals(userId : Long)
}