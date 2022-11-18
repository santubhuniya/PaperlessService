package com.paperless.service.api

import com.paperless.service.response.*
import com.paperless.service.service.BudgetService
import kotlinx.coroutines.flow.first
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/budget")
class BudgetController(val budgetService : BudgetService) {
    @PostMapping("/goal/new",produces = [MediaType.APPLICATION_JSON_VALUE],consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun addNewGoal(@RequestBody newGoalRequest : NewGoalRequest) : Response<out Long>
    = budgetService.addNewGoal(newGoalRequest).first()
    @PostMapping("/new",produces = [MediaType.APPLICATION_JSON_VALUE],consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun  addNewBudget(@RequestBody newBudgetRequest : NewBudgetRequest) : Response<out Long>
    = budgetService.addBudget(newBudgetRequest).first()

    @GetMapping("/goals/{userId}",produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun getAllGoals(@PathVariable("userId") userId : Long) : Response<out GoalSummaryResponse>
    = budgetService.getListOfGoals(userId).first()

    @GetMapping("/budget/all/{userId}/{monthYear}",produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun getBudgetForMonth(
        @PathVariable("userId") userId : Long,
        @PathVariable("monthYear") monthYear : String ) : Response<out List<ExpenseBudgetSummary>>
    = budgetService.getBudgetList(userId,monthYear).first()

}