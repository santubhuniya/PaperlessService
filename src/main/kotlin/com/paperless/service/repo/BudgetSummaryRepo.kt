package com.paperless.service.repo

import com.paperless.service.db.BudgetSummary
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface BudgetSummaryRepo : CoroutineCrudRepository<BudgetSummary,Long> {
}