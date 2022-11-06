package com.paperless.service.repo

import com.paperless.service.db.GoalSummary
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface GoalSummaryRepo : CoroutineCrudRepository<GoalSummary,Long>{
}