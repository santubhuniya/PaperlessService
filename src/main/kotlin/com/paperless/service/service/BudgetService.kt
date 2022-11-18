package com.paperless.service.service

import com.paperless.service.response.*
import kotlinx.coroutines.flow.Flow

interface BudgetService {
    suspend fun addNewGoal(newGoalRequest : NewGoalRequest) : Flow<Response<out Long>>
    suspend fun addBudget(newBudgetRequest : NewBudgetRequest) : Flow<Response<out Long>>

    suspend fun getListOfGoals(userId : Long) : Flow<Response<out GoalSummaryResponse>>
    suspend fun getBudgetList(userId: Long,monthYear : String) : Flow<Response<out List<ExpenseBudgetSummary>>>
    suspend fun getGoalDetails(goalId : Long)

}
