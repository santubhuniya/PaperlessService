package com.paperless.service.service

import com.paperless.service.repo.GoalSummaryRepo
import org.springframework.r2dbc.core.DatabaseClient

class BudgetServiceImpl(
    databaseClient: DatabaseClient,
    val goalSummaryRepo: GoalSummaryRepo,
    val budgetSummaryRepo: GoalSummaryRepo
) : BaseService(databaseClient), BudgetService {


}