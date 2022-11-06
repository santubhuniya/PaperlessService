package com.paperless.service.repo

import com.paperless.service.db.TranscactionSummary
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TranscactionSummaryRepo : CoroutineCrudRepository<TranscactionSummary,Long> {
}