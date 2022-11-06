package com.paperless.service.service

import com.paperless.service.repo.GenericTransactionRepo
import com.paperless.service.repo.TransactionTypeRepo
import com.paperless.service.repo.TranscactionSummaryRepo
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service

@Service
class ExpenseServiceImpl(
    databaseClient : DatabaseClient,
    val genericTransactionRepo: GenericTransactionRepo,
    val transactionTypeRepo: TransactionTypeRepo,
    val transcactionSummaryRepo: TranscactionSummaryRepo
) : BaseService(databaseClient),ExpenseService {

}