package com.paperless.service.repo

import com.paperless.service.db.GenericTransaction
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface GenericTransactionRepo : CoroutineCrudRepository<GenericTransaction,Long>{
}