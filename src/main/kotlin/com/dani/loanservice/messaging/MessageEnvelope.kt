package com.dani.loanservice.messaging

import java.time.Instant
import java.util.UUID

data class MessageEnvelope<T>(
    val event: String,
    val data: T,
    val metadata: EventMetadata,
) {
    companion object {
        fun <T> of(event: String, data: T): MessageEnvelope<T> = MessageEnvelope(
            event = event,
            data = data,
            metadata = EventMetadata(
                timestamp = Instant.now().toString(),
                correlationId = UUID.randomUUID().toString(),
            ),
        )
    }
}