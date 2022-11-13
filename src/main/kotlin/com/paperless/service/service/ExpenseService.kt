package com.paperless.service.service

import com.paperless.service.response.*
import kotlinx.coroutines.flow.Flow

interface ExpenseService {

    suspend fun addNewTransaction(newTransactionRequest : NewTransactionRequest) : Flow<Response<out Long>>
   // suspend fun removeTransaction(txnId : Long) : Flow<Response<Boolean>>
    suspend fun getTransactionSummary(userId : Long,monthYear: String) : Flow<Response<out TransactionSummary>>
//    suspend fun getTransactionByMonth(userId: Long,year : Int) : Flow<Response<out IncomeExpenseSummary>>
//    suspend fun getStatistics(userId: Long, startDate : Long, endDate : Long) : Flow<>
//    suspend fun getCurrentMonthSummaryDetails(userId: Long)
//    suspend fun getUpComingTransaction(userId: Long)
//    suspend fun getExpenseDetails(userId: Long,startDate: Long,endDate: Long)

    suspend fun addExpenseType(transactionTypeRequest : TransactionTypeRequest) : Flow<Response <out Long>>


}