package com.paperless.service.service

import com.paperless.service.db.UpcomingExpense
import com.paperless.service.repo.UpcomingExpenseRepo
import com.paperless.service.response.GoalDto
import kotlinx.coroutines.flow.toList
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitSingleOrNull
import org.springframework.r2dbc.core.flow
import org.springframework.stereotype.Service
import java.util.*

@Service
class BatchRepoServiceImpl(val databaseClient: DatabaseClient,
val upcomingExpenseRepo : UpcomingExpenseRepo
) : BatchRepoService {
    override suspend fun getGoals(userId: Long){
        databaseClient.sql(
            """
            select goal_seq,
            monthly_installment,
            goal_title
            from PERSONAL_GOAL_SUMMARY where
            user_id =:user_id
            and status <> 'COMPLETED'
        """.trimIndent()
        )
            .bind("user_id", userId)
            .map { row, _ ->
                GoalDto(
                    goalSeq = row.get("goal_seq", Long::class.java) ?: 0,
                    goalTitle = row.get("goal_title", String::class.java) ?: "",
                    installment = row.get("monthly_installment", Object::class.java)?.toString()?.toFloat() ?: 0.0F
                )
            }.flow().toList().map { goal ->
                // check if goal seq exists for
                databaseClient.sql(
                    """
                         select expense_seq from 
                         PERSONAL_UPCOMING_EXPENSE
                         where user_id =:user_id
                         and budget_seq =:budget_seq
                         and is_goal_based_expense =:goal_based
                    """.trimIndent()

                )
                    .bind("user_id", userId)
                    .bind("budget_seq", goal.goalSeq)
                    .bind("goal_based", true)
                    .map { row, _ ->
                        row.get("expense_seq", Long::class.java)
                    }.awaitSingleOrNull() ?: run{
                    upcomingExpenseRepo.save(
                        UpcomingExpense(
                            expenseSeq = 0,
                            userId = userId,
                            expenseTitle = goal.goalTitle,
                            source = "system",
                            amount = goal.installment,
                            transactionFor = getCurrentDate(),
                            dueDate = Date().time,
                            dateProduced = Date().time,
                            isPaid = false,
                            isValid = true,
                            isGoalBased = true,
                            budgetSeq = goal.goalSeq
                        )
                    )
                }
            }
    }
}

fun getCurrentDate() : String{
    val calendar = Calendar.getInstance()
    return "${calendar.get(Calendar.MONTH)}_${calendar.get(Calendar.YEAR)}"
}