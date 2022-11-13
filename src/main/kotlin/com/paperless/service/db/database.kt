package com.paperless.service.db

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("PERSONAL_GENERIC_TRANSACTION")
data class GenericTransaction(
    @Id val txnSeq : Long,
    @Column("user_id") val userId : Long,
    @Column("amount") val amount : Float,
    @Column("txn_date") val transactionDate : Long,
    @Column("txn_type") val transactionType : Long,
    @Column("txn_type_level") val transactionTypeLevel : Int,
    @Column("txn_mode") val transactionMode : String, //.debit , credit
    @Column("txn_source") val transactionSource : String, //sms manual
    @Column("paid_by") val paidBy : String, // cash, online
    @Column("is_source_correct") val isSourceCorrect : Boolean,
    @Column("txn_title") val transactionTitle : String,
    @Column("budget_id") val budgetId : Int = 0
)

@Table("PERSONAL_TRANSACTION_TYPE")
data class TransactionType(
    @Id val typeSeq : Long,
    @Column("expense_type_title") var expenseTypeTitle : String,
    @Column("txn_level") var transactionLevel : Int,
    @Column("parent_level") var parentLevel: Int,
    @Column("icon") var icon : String ?,
    @Column("added_by") val addedBy : Long = 0L
)

@Table("PERSONAL_GOAL_SUMMARY")
data class GoalSummary(
    @Id val goalSeq : Long,
    @Column("goal_title") var goalTitle : String,
    @Column("goal_desc") var goalDesc : String,
    @Column("user_id") val userId : Long,
    @Column("target_amt") var targetAmt : Float,
    @Column("monthly_installment") var monthlyInstallment : Float,
    @Column("start_month_year") var startMonthYear : String,
    @Column("total_month") var totalMonth : Int,
    @Column("date_added") var dateAdded : Long,
    @Column("last_payment") val lastPayment : Int,
    @Column("status") val status :String = "NEW", //new , in-progress, completed
    @Column("nbr_paid") val nbrOfPaymentReceived : Int = 0
)

@Table("PERSONAL_BUDGET_SUMMARY")
data class BudgetSummary(
    @Id val budgetSeq : Long,
    @Column("budget_for") val budgetFor : String, // mm_yyyy
    @Column("budget_title") var budgetTitle : String,
    @Column("user_id") val userId : Long,
    @Column("budget_amount") var budgetAmount : Float,
    @Column("date_added") val dateAdded : Long,
    @Column("txn_type") val transactionType : Long,
    @Column("txn_type_level") val transactionTypeLevel : Int
)

@Table("PERSONAL_UPCOMING_EXPENSE")
data class UpcomingExpense(
    @Id val expenseSeq : Long,
    @Column("user_id") val userId: Long,
    @Column("expense_title") val expenseTitle : String,
    @Column("source") val source : String, //
    @Column("amount") val amount : Float,
    @Column("txn_for") val transactionFor : String,
    @Column("due_date") val dueDate : Long,
    @Column("date_produced") val dateProduced : Long,
    @Column("is_paid") val isPaid : Boolean,
    @Column("is_valid") val isValid : Boolean,
    @Column("is_goal_based_expense") val isGoalBased : Boolean,
    @Column("budget_seq") val budgetSeq: Long?
)

@Table("PERSONAL_TRANSACTION_SUMMARY")
data class TranscactionSummary(
    @Id val summarySeq : Long,
    @Column("summary_for") val summaryFor : String,
    @Column("user_id")  val userId: Long,
    @Column("txn_type") val transactionType: Long,
    @Column("txn_type_level") val transactionTypeLevel: Int,
    @Column("total_amount") val amount : Float
)
