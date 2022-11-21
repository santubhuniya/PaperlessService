package com.paperless.service.api

import com.paperless.service.response.*
import com.paperless.service.service.ExpenseService
import kotlinx.coroutines.flow.first
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/transaction")
class TransactionApiController( val expenseService: ExpenseService) {
    // add new transaction
    @PostMapping("/add",produces = [MediaType.APPLICATION_JSON_VALUE],consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun addNewTransaction(
        @RequestBody newTransactionRequest: NewTransactionRequest
    ) : Response<out Long>
    = expenseService.addNewTransaction(newTransactionRequest).first()

//    //remove a transaction
//    @GetMapping("/remove/{txnId}",produces = [MediaType.APPLICATION_JSON_VALUE])
//    suspend fun removeTransaction(
//        @PathVariable("txnId") txnId : Long
//    ) : Response<out Boolean>
//    = expenseService.removeTransaction(txnId).first()

    @GetMapping("/summary/{userId}/{monthYear}",produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun getTransactionSummary(
        @PathVariable("userId") userId : Long,
        @PathVariable("monthYear") monthYear : String
    ) :Response<out TransactionSummary>
    = expenseService.getTransactionSummary(userId,monthYear).first()

    @PostMapping("/add/txnType",produces = [MediaType.APPLICATION_JSON_VALUE],consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun addTransaction(
        @RequestBody transactionTypeRequest : TransactionTypeRequest
    ) : Response<out Long>
    = expenseService.addExpenseType(transactionTypeRequest).first()

    @GetMapping("/details/{userId}/{monthYear}",produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun getMonthlyTransactionDetails(
        @PathVariable("userId") userId: Long,
        @PathVariable("monthYear") monthYear: String
    ) : Response<out MonthlyExpenseDetails>
    = expenseService.getMonthlyExpenseDetails(userId,monthYear).first()

    @PostMapping("/statistics")
    suspend fun getStatistics(@RequestBody chartRequest : ChartRequest) : Response<out ChartSummary>
    = expenseService.getStatistics(chartRequest).first()
}