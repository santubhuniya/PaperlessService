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
            .bind("start_date", dateRangeForYear.first)
            .bind("end_date", dateRangeForYear.second)
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
            .bind("start_date", dateRangeForYear.first)
            .bind("end_date", dateRangeForYear.second)
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
            .bind("start_date", currentMonthDateRange.first)
            .bind("end_date", currentMonthDateRange.second)
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

    override suspend fun getStatistics(chartRequest: ChartRequest): Flow<Response<out ChartSummary>> = execute {


        when(chartRequest.chartType){

            "weekly" -> {
                val dateRange = getCurrentWeek()

                ChartSummary(
                    month = chartRequest.month,
                    year = chartRequest.year,
                    chartType = chartRequest.chartType,
                    categoryList = getExpneseByCategory(
                        userId = chartRequest.userId,
                        startDate = dateRange.first,
                        endDate = dateRange.second
                        ),
                    chartDataList = getWeeklyChart(chartRequest.userId)
                )
            }
            "monthly"->{
                val dateRange = getCurrentMonthDateRange("${chartRequest.month}_${chartRequest.year}")
                ChartSummary(
                    month = chartRequest.month,
                    year = chartRequest.year,
                    chartType = chartRequest.chartType,
                    categoryList = getExpneseByCategory(
                        userId = chartRequest.userId,
                        startDate = dateRange.first,
                        endDate = dateRange.second
                    ),
                    chartDataList = getWeeksOfMonth(userId = chartRequest.userId)
                )
            }
            "yearly"-> {
                val yearDateRange = getDateRangeByYear(chartRequest.year)
                ChartSummary(
                    chartDataList = getYearlyChart(
                        chartRequest.userId,
                        chartRequest.year
                    ),
                    month = chartRequest.month,
                    year = chartRequest.year,
                    chartType = chartRequest.chartType,
                    categoryList = getExpneseByCategory(
                        userId = chartRequest.userId,
                        startDate = yearDateRange.first,
                        endDate = yearDateRange.second

                    ),
                )
            }
            else -> ChartSummary(
                chartDataList = null,
                month = chartRequest.month,
                year = chartRequest.year,
                chartType = chartRequest.chartType,
                categoryList = null,
            )
        }



    }

    suspend fun getYearlyChart(
        userId: Long,
        year: Int
    ): List<ChartData> {
        val monthlyMap = mutableMapOf<String, Triple<Float, Float,Float>>()

        for (i in 0..11) {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, year)
            when (i) {
                Month.january.ordinal -> {
                    calendar.set(Calendar.MONTH, i)
                    val dateRange = Pair(
                        setDateTime(true, calendar, 1),
                        setDateTime(false, calendar, 31)
                    )
                    monthlyMap[Month.january.name] =
                        Triple(
                            getIncomeExpense(userId, dateRange.first, dateRange.second, TransactionMode.debit.name),
                            getIncomeExpense(userId, dateRange.first, dateRange.second, TransactionMode.credit.name),
                            getTotalBudget(userId,dateRange.first, dateRange.second)

                        )

                }
                Month.february.ordinal -> {
                    calendar.set(Calendar.MONTH, i)
                    val dateRange = Pair(
                        setDateTime(true, calendar, 1),
                        setDateTime(false, calendar, 28)
                    )
                    monthlyMap[Month.february.name] =
                        Triple(
                            getIncomeExpense(userId, dateRange.first, dateRange.second, TransactionMode.debit.name),
                            getIncomeExpense(userId, dateRange.first, dateRange.second, TransactionMode.credit.name)
                        , getTotalBudget(userId,dateRange.first, dateRange.second)
                        )
                }
                Month.march.ordinal -> {
                    calendar.set(Calendar.MONTH, i)
                    val dateRange = Pair(
                        setDateTime(true, calendar, 1),
                        setDateTime(false, calendar, 31)
                    )
                    monthlyMap[Month.march.name] =
                        Triple(
                            getIncomeExpense(userId, dateRange.first, dateRange.second, TransactionMode.debit.name),
                            getIncomeExpense(userId, dateRange.first, dateRange.second, TransactionMode.credit.name)
                        , getTotalBudget(userId,dateRange.first, dateRange.second)
                        )
                }
                Month.april.ordinal -> {
                    calendar.set(Calendar.MONTH, i)
                    val dateRange = Pair(
                        setDateTime(true, calendar, 1),
                        setDateTime(false, calendar, 30)
                    )
                    monthlyMap[Month.april.name] =
                        Triple(
                            getIncomeExpense(userId, dateRange.first, dateRange.second, TransactionMode.debit.name),
                            getIncomeExpense(userId, dateRange.first, dateRange.second, TransactionMode.credit.name)
                        , getTotalBudget(userId,dateRange.first, dateRange.second)
                        )
                }
                Month.may.ordinal -> {
                    calendar.set(Calendar.MONTH, i)
                    val dateRange = Pair(
                        setDateTime(true, calendar, 1),
                        setDateTime(false, calendar, 31)
                    )
                    monthlyMap[Month.may.name] =
                        Triple(
                            getIncomeExpense(userId, dateRange.first, dateRange.second, TransactionMode.debit.name),
                            getIncomeExpense(userId, dateRange.first, dateRange.second, TransactionMode.credit.name)
                        , getTotalBudget(userId,dateRange.first, dateRange.second)
                        )
                }
                Month.june.ordinal -> {
                    calendar.set(Calendar.MONTH, i)
                    val dateRange = Pair(
                        setDateTime(true, calendar, 1),
                        setDateTime(false, calendar, 30)
                    )
                    monthlyMap[Month.june.name] =
                        Triple(
                            getIncomeExpense(userId, dateRange.first, dateRange.second, TransactionMode.debit.name),
                            getIncomeExpense(userId, dateRange.first, dateRange.second, TransactionMode.credit.name)
                        , getTotalBudget(userId,dateRange.first, dateRange.second)
                        )
                }
                Month.july.ordinal -> {
                    calendar.set(Calendar.MONTH, i)
                    val dateRange = Pair(
                        setDateTime(true, calendar, 1),
                        setDateTime(false, calendar, 31)
                    )
                    monthlyMap[Month.july.name] =
                        Triple(
                            getIncomeExpense(userId, dateRange.first, dateRange.second, TransactionMode.debit.name),
                            getIncomeExpense(userId, dateRange.first, dateRange.second, TransactionMode.credit.name)
                        , getTotalBudget(userId,dateRange.first, dateRange.second)
                        )
                }
                Month.august.ordinal -> {
                    calendar.set(Calendar.MONTH, i)
                    val dateRange = Pair(
                        setDateTime(true, calendar, 1),
                        setDateTime(false, calendar, 31)
                    )
                    monthlyMap[Month.august.name] =
                        Triple(
                            getIncomeExpense(userId, dateRange.first, dateRange.second, TransactionMode.debit.name),
                            getIncomeExpense(userId, dateRange.first, dateRange.second, TransactionMode.credit.name)
                        , getTotalBudget(userId,dateRange.first, dateRange.second)
                        )
                }
                Month.september.ordinal -> {
                    calendar.set(Calendar.MONTH, i)
                    val dateRange = Pair(
                        setDateTime(true, calendar, 1),
                        setDateTime(false, calendar, 31)
                    )
                    monthlyMap[Month.september.name] =
                        Triple(
                            getIncomeExpense(userId, dateRange.first, dateRange.second, TransactionMode.debit.name),
                            getIncomeExpense(userId, dateRange.first, dateRange.second, TransactionMode.credit.name)
                        , getTotalBudget(userId,dateRange.first, dateRange.second)
                        )
                }
                Month.october.ordinal -> {
                    calendar.set(Calendar.MONTH, i)
                    val dateRange = Pair(
                        setDateTime(true, calendar, 1),
                        setDateTime(false, calendar, 31)
                    )
                    monthlyMap[Month.october.name] =
                        Triple(
                            getIncomeExpense(userId, dateRange.first, dateRange.second, TransactionMode.debit.name),
                            getIncomeExpense(userId, dateRange.first, dateRange.second, TransactionMode.credit.name)
                        , getTotalBudget(userId,dateRange.first, dateRange.second)
                        )
                }
                Month.november.ordinal -> {
                    calendar.set(Calendar.MONTH, i)
                    val dateRange = Pair(
                        setDateTime(true, calendar, 1),
                        setDateTime(false, calendar, 30)
                    )
                    monthlyMap[Month.november.name] =
                        Triple(
                            getIncomeExpense(userId, dateRange.first, dateRange.second, TransactionMode.debit.name),
                            getIncomeExpense(userId, dateRange.first, dateRange.second, TransactionMode.credit.name)
                        , getTotalBudget(userId,dateRange.first, dateRange.second)
                        )
                }
                Month.december.ordinal -> {
                    calendar.set(Calendar.MONTH, i)
                    val dateRange = Pair(
                        setDateTime(true, calendar, 1),
                        setDateTime(false, calendar, 31)
                    )
                    monthlyMap[Month.december.name] =
                        Triple(
                            getIncomeExpense(userId, dateRange.first, dateRange.second, TransactionMode.debit.name),
                            getIncomeExpense(userId, dateRange.first, dateRange.second, TransactionMode.credit.name)
                        , getTotalBudget(userId,dateRange.first, dateRange.second)
                        )
                }
                else -> Pair(0, 0)
            }


        }

        return monthlyMap.map {
            ChartData(
                xData = it.key,
                yIncome = it.value.second,
                yExpense = it.value.first,
                yBudget = it.value.third
            )
        }

    }

    suspend fun setDateTime(
        isStartDate: Boolean,
        calendar: Calendar,
        date: Int
    ): Long {
        return if (isStartDate) {
            calendar.set(Calendar.DATE, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.timeInMillis
        } else {
            calendar.set(Calendar.DATE, date)
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.timeInMillis
        }
    }

    suspend fun getCurrentWeek() : Pair<Long,Long>{
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.SUNDAY
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        val startDay = calendar.get(Calendar.DATE)
        calendar.set(Calendar.DATE, startDay)

        val calendarStart = calendar.apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.HOUR_OF_DAY, 0)
        }.timeInMillis
        calendar.set(Calendar.DATE,calendar.get(Calendar.DATE) + 7 )
        val calendarEnd = calendar.apply {
            set(Calendar.SECOND, 59)
            set(Calendar.MINUTE, 59)
            set(Calendar.HOUR_OF_DAY, 23)
        }.timeInMillis

        return Pair(calendarStart,calendarEnd)
    }

    suspend fun getWeeklyChart(userId: Long): List<ChartData> {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.SUNDAY
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        val startDay = calendar.get(Calendar.DATE)
        calendar.set(Calendar.DATE, startDay)

        val weekDataMap = mutableMapOf<String, Pair<Float, Float>>()

        for (i in 0..6) {

            val calendarStart = calendar.apply {
                set(Calendar.SECOND, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.HOUR_OF_DAY, 0)
            }.timeInMillis
            val calendarEnd = calendar.apply {
                set(Calendar.SECOND, 59)
                set(Calendar.MINUTE, 59)
                set(Calendar.HOUR_OF_DAY, 23)
            }.timeInMillis

            println("start - $calendarStart end- $calendarEnd")
            calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1)
            weekDataMap[when (i) {
                0 -> WeekDay.sunday.name
                1 -> WeekDay.monday.name
                2 -> WeekDay.tuesday.name
                3 -> WeekDay.wednesday.name
                4 -> WeekDay.thrusday.name
                5 -> WeekDay.friday.name
                6 -> WeekDay.saturday.name
                else -> WeekDay.sunday.name
            }] = Pair(
                getIncomeExpense(userId, calendarStart, calendarEnd, TransactionMode.debit.name),
                getIncomeExpense(userId, calendarStart, calendarEnd, TransactionMode.credit.name)
            )
        }

        return weekDataMap.map {
            ChartData(
                xData = it.key,
                yIncome = it.value.second,
                yExpense = it.value.first
            )
        }

    }

    suspend fun getIncomeExpense(
        userId: Long,
        startDate: Long,
        endDate: Long,
        transactionMode: String
    ): Float =
        databaseClient.sql(
            """
                select sum(amount) total_amt
                from PERSONAL_GENERIC_TRANSACTION
                where user_id =:user_id
                and txn_mode =:txn_mode
                and txn_date between :start_date and :end_date
            """.trimIndent()
        )
            .bind("user_id", userId)
            .bind("txn_mode", transactionMode)
            .bind("start_date", startDate)
            .bind("end_date", endDate)
            .map { row, _ ->
                row.get("total_amt", Object::class.java)?.toString()?.toFloat() ?: 0.0F
            }.awaitSingleOrNull() ?: 0.0F


    override suspend fun addExpenseType(
        transactionTypeRequest: TransactionTypeRequest
    ): Flow<Response<out Long>> = execute {
        transactionTypeRequest.typeSeq?.let { typeSeq ->
            transactionTypeRepo.findById(typeSeq)?.let { txnType ->
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

    suspend fun getTotalBudget(
        userId: Long,
        startDate: Long,
        endDate: Long
    ) : Float =
        databaseClient.sql(
            """
             select sum(budget_amount) amount
             from PERSONAL_BUDGET_SUMMARY
             where user_id =:user_id
             and date_added between :start_date and :end_date
        """.trimIndent()
        )
            .bind("user_id", userId)
            .bind("start_date", startDate)
            .bind("end_date", endDate)
            .map { row, _ ->
                row.get("amount", Object::class.java)?.toString()?.toFloat() ?: 0.0F
            }.awaitSingleOrNull() ?: 0.00F


    override suspend fun getMonthlyExpenseDetails(
        userId: Long,
        monthYear: String
    ): Flow<Response<out MonthlyExpenseDetails>> = execute {

        val dateRange = getCurrentMonthDateRange(monthYear)

        val totalBudget = getTotalBudget(userId,dateRange.first,dateRange.second)

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
            .bind("start_date", dateRange.first)
            .bind("end_date", dateRange.second)
            .map { row, _ ->
                row.get("total_amt", Object::class.java)?.toString()?.toFloat() ?: 0.0F
            }.awaitSingleOrNull() ?: 0.0F

        val listCategory = getExpneseByCategory(
            userId,
            dateRange.first,
            dateRange.second
        )

        val topSpendingList = databaseClient.sql(
            """
            select txn_seq,txn_title,
            amount,
            txn_type,
            txn_mode,
            txn_date from PERSONAL_GENERIC_TRANSACTION
            where user_id =:user_id
            order by amount desc
            limit 5 
        """.trimIndent()
        )
            .bind("user_id", userId)
            .map { row, _ ->
                TopSpending(
                    expenseId = row.get("txn_seq", Long::class.java) ?: 0L,
                    expenseName = row.get("txn_title", String::class.java) ?: "",
                    amount = row.get("amount", Object::class.java)?.toString()?.toFloat() ?: 0.0F,
                    icon = null,
                    date = row.get("txn_date", Long::class.java) ?: 0L,
                    categoryName = ""
                )
            }.flow().toList()

        MonthlyExpenseDetails(
            totalExpense = totalExpense,
            totalBudget = totalBudget,
            listExpenseCategory = listCategory,
            listTopSpending = topSpendingList
        )

    }

    suspend fun updateTransactionSummary() {

    }

    suspend fun getExpneseByCategory(
        userId: Long,
        startDate: Long,
        endDate: Long
    ) : List<ExpenseCategory> = databaseClient.sql(
        """
             select sum(amount) amount,txn_type from PERSONAL_GENERIC_TRANSACTION
             where user_id =:user_id
             and txn_mode =:txn_mode
             and txn_date between :start_date and :end_date
             group by txn_type
        """.trimIndent()
    )
        .bind("user_id", userId)
        .bind("txn_mode", TransactionMode.debit.name)
        .bind("start_date", startDate)
        .bind("end_date", endDate)
        .map { row, _ ->
            ExpenseCategory(
                categoryId = row.get("txn_type", Long::class.java) ?: 0,
                categoryName = "",
                amount = row.get("amount", Object::class.java)?.toString()?.toFloat() ?: 0.00F
            )
        }.flow().toList().map {
            it.categoryName = getTransactionCategory(it.categoryId)
            it
        }

    suspend fun getWeeksOfMonth(userId: Long) : List<ChartData>{
        // first day of month
        val startCal = Calendar.getInstance()
        startCal.set(Calendar.DAY_OF_MONTH,startCal.getActualMinimum(Calendar.DAY_OF_MONTH))

        val endCal = Calendar.getInstance()
        endCal.set(Calendar.DAY_OF_MONTH,startCal.getActualMaximum(Calendar.DAY_OF_MONTH))

        val calendar = Calendar.getInstance()
        val nbrOfWeeks = calendar.getActualMaximum(Calendar.WEEK_OF_MONTH) -1

        val weeklyDataForMonth = mutableMapOf<String, Pair<Float, Float>>()

        for (i in 0..nbrOfWeeks){

            when(i){
                0->{
                    val startDay = startCal.get(Calendar.DAY_OF_WEEK)
                    println(startCal.get(Calendar.DATE))
                    val startTime = startCal.timeInMillis

                   startCal.set(Calendar.DATE,startCal.get(Calendar.DATE)+(7-startDay))
                    println(startCal.get(Calendar.DATE))
                    weeklyDataForMonth["week$i"] =
                    Pair(
                        getIncomeExpense(userId, startTime, startCal.timeInMillis, TransactionMode.debit.name),
                        getIncomeExpense(userId, startTime, startCal.timeInMillis, TransactionMode.credit.name)
                    )
                }
                nbrOfWeeks->{
                    startCal.set(Calendar.DATE,startCal.get(Calendar.DATE)+1)
                    println(startCal.get(Calendar.DATE))
                    println(endCal.get(Calendar.DATE))

                    weeklyDataForMonth["week$i"] =
                        Pair(
                            getIncomeExpense(userId,  startCal.timeInMillis,endCal.timeInMillis, TransactionMode.debit.name),
                            getIncomeExpense(userId,  startCal.timeInMillis,endCal.timeInMillis, TransactionMode.credit.name)
                        )
                }
                else ->{
                    startCal.set(Calendar.DATE,startCal.get(Calendar.DATE)+1)
                    println(startCal.get(Calendar.DATE))
                    val startTime = startCal.timeInMillis
                    startCal.set(Calendar.DATE,startCal.get(Calendar.DATE)+6)
                    println(startCal.get(Calendar.DATE))
                    weeklyDataForMonth["week$i"] =
                        Pair(
                            getIncomeExpense(userId, startTime, startCal.timeInMillis, TransactionMode.debit.name),
                            getIncomeExpense(userId, startTime, startCal.timeInMillis, TransactionMode.credit.name)
                        )
                }
            }

        }

       return  weeklyDataForMonth.map {
            ChartData(
                xData = it.key,
                yIncome = it.value.second,
                yExpense = it.value.first
            )
        }

    }

    suspend fun getTransactionCategory(seq: Long): String = databaseClient.sql(
        """
         select expense_type_title from 
         PERSONAL_TRANSACTION_TYPE
         where type_seq =:seq
    """.trimIndent()
    )
        .bind("seq", seq)
        .map { row, _ ->
            row.get("expense_type_title", String::class.java) ?: ""
        }.awaitSingleOrNull() ?: ""

}

fun getCurrentMonthDateRange(monthYear: String): Pair<Long, Long> {
    return monthYear.split("_").let { monthList ->
        println("month - ${monthList[0]} year -${monthList[1]}")
        val calendarStart = Calendar.getInstance().apply {
            set(Calendar.DATE, 1)
            set(Calendar.MONTH, monthList[0].toInt())
            set(Calendar.YEAR, monthList[1].toInt())
        }

        val calendarEnd = Calendar.getInstance().apply {
            set(Calendar.DATE, calendarStart.getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.MONTH, monthList[0].toInt())
            set(Calendar.YEAR, monthList[1].toInt())
        }
        println("start time -${calendarStart.timeInMillis} end time -${calendarEnd.timeInMillis}")
        Pair(calendarStart.timeInMillis, calendarEnd.timeInMillis)

    }
}

fun getDateRangeByMonthYear(monthYear: String): Pair<Long, Long> {
    return monthYear.split("_").let { monthList ->

        val calendarStart = Calendar.getInstance().apply {
            set(Calendar.DATE, 1)
            set(Calendar.MONTH, 0)
            set(Calendar.YEAR, monthList[1].toInt())
        }

        val currentMonthCalendar = Calendar.getInstance().apply {
            set(Calendar.DATE, 1)
            set(Calendar.MONTH, monthList[0].toInt())
            set(Calendar.YEAR, monthList[1].toInt())
        }

        val calendarEnd = Calendar.getInstance().apply {
            set(Calendar.DATE, currentMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.MONTH, monthList[0].toInt())
            set(Calendar.YEAR, monthList[1].toInt())
        }

        Pair(calendarStart.timeInMillis, calendarEnd.timeInMillis)

    }
}

fun getDateRangeByYear(year: Int): Pair<Long, Long> {
    val calendarStart = Calendar.getInstance().apply {
        set(Calendar.DATE, 1)
        set(Calendar.MONTH, 0)
        set(Calendar.YEAR, year)
    }

    val calendarEnd = Calendar.getInstance().apply {
        set(Calendar.DATE, 31)
        set(Calendar.MONTH, 11)
        set(Calendar.YEAR, year)
    }
    return Pair(calendarStart.timeInMillis, calendarEnd.timeInMillis)
}


enum class TransactionMode {
    credit,
    debit
}

enum class WeekDay {
    sunday,
    monday,
    tuesday,
    wednesday,
    thrusday,
    friday,
    saturday
}

enum class Month {
    january,
    february,
    march,
    april,
    may,
    june,
    july,
    august,
    september,
    october,
    november,
    december
}