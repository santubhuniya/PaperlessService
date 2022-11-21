package com.paperless.service.response

import org.springframework.data.relational.core.mapping.Column


data class NewTransactionRequest(
    val userId : Long,
    val amount : Float,
    val transactionDate : Long?,
    val transactionType : Long,
    val transactionTypeLevel : Int,
    val transactionMode : String, //.debit , credit
    val transactionSource : String, //sms manual
    val paidBy : String, // cash, online
    val isSourceCorrect : Boolean,
    val transactionTitle : String,
    val budgetId : Int = 0
)

data class NewBudgetRequest(
    val budgetSeq : Long?,
    val userId: Long,
    val budgetFor : String, //mm_yyyy
    val budgetTitle : String,
    val budgetAmount : Float,
    val dateAdded : Long?,
    val transactionType : Long,
    val transactionTypeLevel : Int
)

data class NewGoalRequest(
    val goalSeq : Long?,
    val goalTitle : String,
    val goalDesc : String,
    val userId : Long,
    val targetAmt : Float,
    val monthlyInstallment : Float,
    val startMonthYear : String,
    val totalMonth : Int,
    val dateAdded : Long?,
    val lastPayment : Int
)

data class TransactionTypeRequest(
    val typeSeq : Long?,
    val expenseTypeTitle : String,
    val level : Int,
    val parentLevel : Int,
    val icon : String?,
    val addedBy : Long
)

data class ChartRequest(
    val month : Int,
    val year : Int,
    val chartType : String, //weekly, monthly, yearly
    val userId: Long,
    val week : Int
)
