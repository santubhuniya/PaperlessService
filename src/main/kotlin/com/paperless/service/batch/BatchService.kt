package com.paperless.service.batch

import com.paperless.service.service.BatchRepoService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.invoke
import kotlinx.coroutines.runBlocking
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component


@Component
class BatchService (val batchRepoService: BatchRepoService){

    @Scheduled(cron = "0 */5 * * * ?")
    fun addUpcommingExpenseBatch() {
        //add upcomming expense from goal
        runBlocking (Dispatchers.IO){
            batchRepoService.getGoals(3)
        }
    }
}