package com.paperless.service.service

import com.paperless.service.db.GenericTransaction
import com.paperless.service.db.TransactionType
import com.paperless.service.repo.GenericTransactionRepo
import com.paperless.service.repo.TransactionTypeRepo
import com.paperless.service.repo.TranscactionSummaryRepo
import com.paperless.service.response.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitSingleOrNull
import org.springframework.r2dbc.core.flow
import org.springframework.stereotype.Service
import java.util.*

@Service
class ExpenseServiceImpl(
    databaseClient: DatabaseClient,
    val genericTransactionRepo: GenericTransactionRepo,
    val transactionTypeRepo: TransactionTypeRepo,
    val transcactionSummaryRepo: TranscactionSummaryRepo
) : BaseService(databaseClient), ExpenseService {
    override suspend fun addNewTransaction(newTransactionRequest: NewTransactionRequest): Flow<Response<out Long>> =
        execute {
            genericTransactionRepo.save(
                GenericTransaction(
                    txnSeq = 0,
                    userId = newTransactionRequest.userId,
                    amount = newTransactionRequest.amount,
                    transactionDate = newTransactionRequest.transactionDate ?: Date().time,
                    transactionType = newTransactionRequest.transactionType,
                    transactionTypeLevel = newTransactionRequest.transactionTypeLevel,
                    transactionMode = newTransactionRequest.transactionMode,
                    transactionSource = newTransactionRequest.transactionSource,
                    paidBy = newTransactionRequest.paidBy,
                    isSourceCorrect = newTransactionRequest.isSourceCorrect,
                    transactionTitle = newTransactionRequest.transactionTitle
                )
            ).txnSeq
        }

    override suspend fun getTransactionSummary(
        userId: Long,
        monthYear: String
    ): Flow<Response<out TransactionSummary>> = execute {

        val dateRangeForYear = getDateRangeByMonthYear(monthYear)

        // get total expense for this year
        val totalExpense = databaseClient.sql(
            """
            select sum(amount) total_amt
            from PERSONAL_GENERIC_TRANSACTION
            where user_id =:user_id
            and txn_mode =:txn_mode
            and txn_date between :start_date and :end_date
        """.trimIndent()
        )
            .bind("user_id", userId)
            .bind("txn_mode", TransactionMode.debit.name)
            .bind("start_date",dateRangeForYear.first)
            .bind("end_date",dateRangeForYear.second)
            .map { row, _ ->
                row.get("total_amt", Object::class.java)?.toString()?.toFloat() ?: 0.0F
            }.awaitSingleOrNull() ?: 0.0F

        val totalIncome = databaseClient.sql(
            """
            select sum(amount) total_amt
            from PERSONAL_GENERIC_TRANSACTION
            where user_id =:user_id
            and txn_mode =:txn_mode
            and txn_date between :start_date and :end_date
        """.trimIndent()
        )
            .bind("user_id", userId)
            .bind("txn_mode", TransactionMode.credit.name)
            .bind("start_date",dateRangeForYear.first)
            .bind("end_date",dateRangeForYear.second)
            .map { row, _ ->
                row.get("total_amt", Object::class.java)?.toString()?.toFloat() ?: 0.0F
            }.awaitSingleOrNull() ?: 0.0F
        //get total income for this year

        //get monthly expense and budget for month year
        val currentMonthDateRange = getCurrentMonthDateRange(monthYear)
        val expenseForMonth = databaseClient.sql(
            """
                select sum(amount) total_amt
                from PERSONAL_GENERIC_TRANSACTION
                where user_id =:user_id
                and txn_mode =:txn_mode
                and txn_date between :start_date and :end_date
            """.trimIndent()
        )
            .bind("user_id", userId)
            .bind("txn_mode", TransactionMode.debit.name)
            .bind("start_date",currentMonthDateRange.first)
            .bind("end_date",currentMonthDateRange.second)
            .map { row, _ ->
                row.get("total_amt", Object::class.java)?.toString()?.toFloat() ?: 0.0F
            }.awaitSingleOrNull() ?: 0.0F


        // get expense by category
        val transaction = databaseClient.sql(
            """
            select txn_seq,txn_title,
            amount,
            txn_type,
            txn_mode,
            txn_date from PERSONAL_GENERIC_TRANSACTION
            where user_id =:user_id
            order by txn_date desc
            limit 5 
        """.trimIndent()
        )
            .bind("user_id", userId)
            .map { row, _ ->
                Transaction(
                    txnId = row.get("txn_seq", Long::class.java) ?: 0L,
                    txnTitle = row.get("txn_title", String::class.java) ?: "",
                    txnAmount = row.get("amount", Object::class.java)?.toString()?.toFloat() ?: 0.0F,
                    isExpense = row.get("txn_mode", String::class.java)?.equals("debit") ?: false,
                    txnDate = row.get("txn_date", Long::class.java) ?: 0L,
                    txnTypeName = ""
                )
            }.flow().toList()
        //get last 5 transaction

        TransactionSummary(
            totalExpense = totalExpense,
            totalIncome = totalIncome,
            monthlyExpense = expenseForMonth,
            monthlyBudget = 0.0F,
            lastFiveTransaction = transaction
        )
    }

    override suspend fun addExpenseType(
        transactionTypeRequest: TransactionTypeRequest
    ): Flow<Response<out Long>> = execute{
        transactionTypeRequest.typeSeq?.let { typeSeq->
            transactionTypeRepo.findById(typeSeq)?.let { txnType->
                transactionTypeRepo.save(
                    txnType.apply {
                        expenseTypeTitle = transactionTypeRequest.expenseTypeTitle
                        icon = transactionTypeRequest.icon
                        transactionLevel = transactionTypeRequest.level
                        parentLevel = transactionTypeRequest.parentLevel
                    }
                )

            }?.typeSeq
        } ?: kotlin.run {
            transactionTypeRepo.save(
                TransactionType(
                    typeSeq = 0,
                    expenseTypeTitle = transactionTypeRequest.expenseTypeTitle,
                    transactionLevel = transactionTypeRequest.level,
                    parentLevel = transactionTypeRequest.parentLevel,
                    icon = transactionTypeRequest.icon,
                    addedBy = transactionTypeRequest.addedBy
                )
            ).typeSeq
        }
    }

    suspend fun updateTransactionSummary(){

    }

}

fun getCurrentMonthDateRange(monthYear: String) : Pair<Long,Long>{
   return monthYear.split("_").let { monthList->
        println("month - ${monthList[0]} year -${monthList[1]}")
        val calendarStart = Calendar.getInstance().apply {
            set(Calendar.DATE,1)
            set(Calendar.MONTH,monthList[0].toInt())
            set(Calendar.YEAR,monthList[1].toInt())
        }

       val calendarEnd = Calendar.getInstance().apply {
           set(Calendar.DATE,calendarStart.getActualMaximum(Calendar.DAY_OF_MONTH))
           set(Calendar.MONTH,monthList[0].toInt())
           set(Calendar.YEAR,monthList[1].toInt())
       }
       println("start time -${calendarStart.timeInMillis} end time -${calendarEnd.timeInMillis}")
       Pair(calendarStart.timeInMillis,calendarEnd.timeInMillis)

    }
}

fun getDateRangeByMonthYear(monthYear: String) : Pair<Long,Long>{
    return monthYear.split("_").let { monthList->

        val calendarStart = Calendar.getInstance().apply {
            set(Calendar.DATE,1)
            set(Calendar.MONTH,0)
            set(Calendar.YEAR,monthList[1].toInt())
        }

        val currentMonthCalendar =  Calendar.getInstance().apply {
        set(Calendar.DATE,1)
        set(Calendar.MONTH,monthList[0].toInt())
        set(Calendar.YEAR,monthList[1].toInt())
    }

        val calendarEnd = Calendar.getInstance().apply {
            set(Calendar.DATE,currentMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.MONTH,monthList[0].toInt())
            set(Calendar.YEAR,monthList[1].toInt())
        }

        Pair(calendarStart.timeInMillis,calendarEnd.timeInMillis)

    }
}

fun getDateRangeByYear(year : Int) : Pair<Long,Long>{
    val calendarStart = Calendar.getInstance().apply {
        set(Calendar.DATE,1)
        set(Calendar.MONTH,0)
        set(Calendar.YEAR,year)
    }

    val calendarEnd = Calendar.getInstance().apply {
        set(Calendar.DATE,31)
        set(Calendar.MONTH,11)
        set(Calendar.YEAR,year)
    }
    return Pair(calendarStart.timeInMillis,calendarEnd.timeInMillis)
}


enum class TransactionMode {
    credit,
    debit
}