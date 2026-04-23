package com.dani.loanservice.dto

import org.springframework.http.HttpStatus
import java.time.OffsetDateTime

data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val timestamp: OffsetDateTime = OffsetDateTime.now()
) {
    constructor(httpStatus: HttpStatus, message: String) : this(
        status = httpStatus.value(),
        error = httpStatus.reasonPhrase,
        message = message
    )
}