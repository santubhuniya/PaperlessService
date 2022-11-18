package com.paperless.service.service

import com.paperless.service.db.BudgetSummary
import com.paperless.service.db.GoalSummary
import com.paperless.service.repo.BudgetSummaryRepo
import com.paperless.service.repo.GoalSummaryRepo
import com.paperless.service.response.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitSingleOrNull
import org.springframework.r2dbc.core.flow
import org.springframework.stereotype.Service
import java.util.*

@Service
class BudgetServiceImpl(
    databaseClient: DatabaseClient,
    val goalSummaryRepo: GoalSummaryRepo,
    val budgetSummaryRepo: BudgetSummaryRepo
) : BaseService(databaseClient), BudgetService {

    override suspend fun addNewGoal(newGoalRequest: NewGoalRequest): Flow<Response<out Long>> = execute {

        newGoalRequest.goalSeq?.let { goalSeq->
            goalSummaryRepo.findById(goalSeq)
            ?.let { goalSummary->
                goalSummary.goalTitle = newGoalRequest.goalTitle
                goalSummary.goalDesc = newGoalRequest.goalDesc
                goalSummary.dateAdded = newGoalRequest.dateAdded ?: Date().time
                goalSummary.targetAmt = newGoalRequest.targetAmt
                goalSummary.monthlyInstallment = newGoalRequest.monthlyInstallment
                goalSummary.startMonthYear = newGoalRequest.startMonthYear
                goalSummary.totalMonth = newGoalRequest.totalMonth

                goalSummaryRepo.save(goalSummary)
            }?.goalSeq

        } ?: kotlin.run {
            goalSummaryRepo.save(
                GoalSummary(
                    goalSeq = 0,
                    goalTitle = newGoalRequest.goalTitle,
                    goalDesc = newGoalRequest.goalDesc,
                    userId = newGoalRequest.userId,
                    targetAmt = newGoalRequest.targetAmt,
                    monthlyInstallment = newGoalRequest.monthlyInstallment,
                    startMonthYear = newGoalRequest.startMonthYear,
                    totalMonth = newGoalRequest.totalMonth,
                    dateAdded = newGoalRequest.dateAdded ?: Date().time,
                    lastPayment = newGoalRequest.lastPayment
                )
            ).goalSeq
        }
    }


    override suspend fun addBudget(newBudgetRequest: NewBudgetRequest): Flow<Response<out Long>> = execute {

        newBudgetRequest.budgetSeq?.let {  budgetSeq->
            budgetSummaryRepo.findById(budgetSeq)?.let { budgetSummary->
                budgetSummary.budgetTitle = newBudgetRequest.budgetTitle
                budgetSummary.budgetAmount = newBudgetRequest.budgetAmount
                budgetSummaryRepo.save(budgetSummary)
            }?.budgetSeq

        } ?: run {

            budgetSummaryRepo.save(
                BudgetSummary(
                    budgetSeq = 0,
                    budgetFor = newBudgetRequest.budgetFor,
                    budgetTitle = newBudgetRequest.budgetTitle,
                    userId = newBudgetRequest.userId,
                    budgetAmount = newBudgetRequest.budgetAmount,
                    dateAdded = newBudgetRequest.dateAdded ?: Date().time,
                    transactionType = newBudgetRequest.transactionType,
                    transactionTypeLevel = newBudgetRequest.transactionTypeLevel
                )
            ).budgetSeq
        }
    }

    fun getPercentage(nbrPaid : Int, total:Int) : Float {
        return (nbrPaid / total.toFloat()) * 100
    }

    override suspend fun getListOfGoals(userId: Long) : Flow<Response<out GoalSummaryResponse>>
    = execute {
        // get details
      val goalList =  databaseClient.sql("""
            select goal_seq, 
            goal_title, 
            goal_desc, 
            target_amt, 
            total_month, 
            last_payment,
            status,
            nbr_paid
            from PERSONAL_GOAL_SUMMARY 
            where user_id =:user_id
        """.trimIndent())
            .bind("user_id",userId)
            .map {
                row,_->
                GoalSummary(
                    goalSeq = row.get("goal_seq",Long::class.java) ?: 0L,
                    goalName = row.get("goal_title",String::class.java) ?: "",
                    goalDesc = row.get("goal_desc" ,String::class.java) ?: "",
                    goalStatus = row.get("status", String::class.java) ?: "",
                    totalAmount = row.get("target_amt", Float::class.java) ?: 0.0F,
                    percentageComplete = getPercentage(
                        row.get("nbr_paid", Int::class.java) ?: 0,
                        row.get("total_month", Int::class.java) ?: 1
                    )
                )
            }.flow().toList()

        GoalSummaryResponse(
            totalGoalAmount =getTotalGoalAmount(userId),
            goalFor = getCurrentDate(),
            goalPaid = getPaidGaolAmount(userId,getCurrentDate()),
            listGoals = goalList
        )
    }

    suspend fun getPaidGaolAmount(userId: Long,monthYear: String) : Float =
        databaseClient.sql("""
             select sum(amount) as total_amount from 
             PERSONAL_UPCOMING_EXPENSE
             where user_id =:user_id
             and txn_for =:budget_for
             and is_paid =:is_paid
        """.trimIndent())
            .bind("user_id",userId)
            .bind("budget_for", getCurrentDate())
            .bind("is_paid",true)
            .map {
                    row,_->
                row.get("total_amount",Object::class.java)?.toString()?.toFloat() ?: 0.0F
            }.awaitSingleOrNull() ?: 0.0F

    suspend fun getTotalGoalAmount(userId: Long) : Float =
        databaseClient.sql("""
            select sum(amount) as total_amount from 
             PERSONAL_UPCOMING_EXPENSE
             where user_id =:user_id
             and txn_for =:budget_for
        """.trimIndent())
            .bind("user_id",userId)
            .bind("budget_for", getCurrentDate())
            .map {
                row,_->
                row.get("total_amount",Object::class.java)?.toString()?.toFloat() ?: 0.0F
            }.awaitSingleOrNull() ?: 0.0F


    override suspend fun getBudgetList(userId: Long, monthYear: String) : Flow<Response<out List<ExpenseBudgetSummary>>>
    = execute{
        // get expense budget summary
        databaseClient.sql("""
            select type_seq,
            expense_type_title,
            icon
            from 
            PERSONAL_TRANSACTION_TYPE
            where added_by in (0,:user_id)
            and txn_level =:txn_level
        """.trimIndent())
            .bind("user_id",userId)
            .bind("txn_level",1)
            .map {
                row,_->
                ExpenseBudgetSummary(
                    expenseTypeId = row.get("type_seq",Long::class.java) ?: 0L,
                    expenseTypeName = row.get("expense_type_title",String::class.java) ?: "",
                    budgetId = null,
                    budgetName = null,
                    budgetAmount = null,
                    expenseAmount = null,
                    monthYear = monthYear
                )
            }.flow()
            .toList()
            .map{ expenseSummary->
                // get budget details
                getBudgetDetails(monthYear,userId,expenseSummary.expenseTypeId)?.let {
                    expenseSummary.budgetId = it.budgetSeq
                    expenseSummary.budgetName = it.budgetTitle
                    expenseSummary.budgetAmount = it.budgetAmount
                }
                expenseSummary.expenseAmount = getExpense(userId,monthYear,expenseSummary.expenseTypeId)
                expenseSummary
            }
    }

    data class TemporaryBudget(
        val budgetSeq : Long,
        val budgetTitle : String,
        val budgetAmount : Float
    )

    suspend fun getBudgetDetails(
        monthYear: String,
        userId: Long,
        txnType : Long) : TemporaryBudget?{
       return databaseClient.sql("""
            select budget_amount, 
            budget_seq, 
            budget_title
            from PERSONAL_BUDGET_SUMMARY
            where user_id =:user_id
            and budget_for =:month_year
            and txn_type =:txn_type
        """.trimIndent())
            .bind("user_id", userId)
            .bind("month_year",monthYear)
            .bind("txn_type",txnType)
            .map {
                row,_->
                TemporaryBudget(
                    budgetSeq = row.get("budget_seq",Long::class.java) ?: 0L,
                    budgetTitle = row.get("budget_title",String::class.java) ?: "",
                    budgetAmount = row.get("budget_amount",Object::class.java)?.toString()?.toFloat() ?: 0.0F
                )
            }.awaitSingleOrNull()
    }

    suspend fun getExpense(userId: Long, monthYear: String,txnType : Long) : Float{
        val dateDuration = getCurrentMonthDateRange(monthYear)
        return databaseClient.sql("""
            select sum(amount) as total_amount from 
            PERSONAL_GENERIC_TRANSACTION
            where user_id =:user_id
            and txn_mode =:txn_mode
            and txn_type =:txn_type
            and txn_date between :start_date and :end_date
        """.trimIndent())
            .bind("user_id",userId)
            .bind("start_date",dateDuration.first)
            .bind("end_date",dateDuration.second)
            .bind("txn_type",txnType)
            .bind("txn_mode","debit")
            .map {
                row,_->
                row.get("total_amount",Object::class.java)?.toString()?.toFloat() ?: 0.0F
            }.awaitSingleOrNull() ?: 0.0F

    }

    override suspend fun getGoalDetails(goalId: Long) {
        TODO("Not yet implemented")
    }


}