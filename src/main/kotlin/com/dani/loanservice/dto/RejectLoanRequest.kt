package com.dani.loanservice.dto

import jakarta.validation.constraints.NotBlank

data class RejectLoanRequest(
    @field:NotBlank val reason: String
)