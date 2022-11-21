package com.paperless.service.response

import org.springframework.data.annotation.Id
import java.time.temporal.TemporalAmount

sealed class Response<T>(val data: T? = null, val error: ErrorResponse? = null) {
    class Success<T>(data: T) : Response<T>(data)
    class Error<T>(data: T? = null, message: ErrorResponse?) : Response<T>(data, message)
}

data class ErrorResponse(
    val errorCode: Int,
    val errorMessage: String
)

data class TransactionSummary(
    val totalIncome : Float,
    val totalExpense : Float,
    val monthlyExpense : Float,
    var monthlyBudget : Float = 0.0F,
    val lastFiveTransaction : List<Transaction>
)

data class Transaction(
    val txnId : Long,
    val txnTitle : String,
    val txnAmount : Float,
    val txnTypeName : String,
    val isExpense : Boolean,
    val txnDate : Long
)

data class IncomeExpenseSummary(
    val year : Int,
    val totalIncome: Float,
    val totalExpense: Float,
    val totalBudget : Float,
    val summaryList : List<MonthlySummary?>
)

data class MonthlySummary(
    val month : Int,
    val income : Float,
    val expense : Float,
    val budget : Float
)

data class GoalSummaryResponse(
    val totalGoalAmount : Float,
    val goalPaid : Float,
    val goalFor : String,
    val listGoals : List<GoalSummary?>
)

data class GoalSummary(
    val goalSeq : Long,
    val goalName : String,
    val goalDesc : String,
    val goalStatus : String,
    val totalAmount : Float,
    val percentageComplete: Float
)

data class ExpenseBudgetSummary(
    val expenseTypeId : Long,
    val expenseTypeName : String,
    var budgetId: Long?,
    var budgetName : String?,
    var expenseAmount : Float?,
    var budgetAmount: Float?,
    val monthYear : String
)

// monthly expense screen
data class MonthlyExpenseDetails(
    val totalExpense: Float,
    val totalBudget: Float,
    var listExpenseCategory: List<ExpenseCategory>?,
    var listTopSpending: List<TopSpending>?
)

data class ExpenseCategory(
    val categoryId : Long,
    var categoryName : String,
    val amount : Float
)

data class TopSpending(
    val expenseId: Long,
    val expenseName : String,
    val amount: Float,
    val icon : String?,
    val date : Long,
    val categoryName: String
)

// statistics page
data class ChartSummary(
    val month : Int,
    val year : Int,
    val chartType : String, //monthly, weekly, yearly
    var categoryList : List<ExpenseCategory>?, //pie chart for the chart type,
    var chartDataList : List<ChartData>?
)

data class ChartData(
    val xData : String,
    val yExpense : Float = 0f,
    val yIncome : Float = 0f,
    val yBudget : Float = 0f
)