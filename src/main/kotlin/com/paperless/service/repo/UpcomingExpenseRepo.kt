package com.paperless.service.repo

import com.paperless.service.db.UpcomingExpense
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UpcomingExpenseRepo  : CoroutineCrudRepository<UpcomingExpense,Long>{
}