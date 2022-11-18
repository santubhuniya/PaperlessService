package com.paperless.service.response


data class GoalDto(
    val goalSeq : Long,
    val goalTitle : String,
    val installment : Float
)