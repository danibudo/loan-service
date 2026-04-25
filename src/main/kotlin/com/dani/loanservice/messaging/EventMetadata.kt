package com.dani.loanservice.messaging

data class EventMetadata(
    val timestamp: String,
    val correlationId: String,
)