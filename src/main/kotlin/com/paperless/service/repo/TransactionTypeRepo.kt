package com.paperless.service.repo

import com.paperless.service.db.TransactionType
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TransactionTypeRepo  : CoroutineCrudRepository<TransactionType,Long>{
}