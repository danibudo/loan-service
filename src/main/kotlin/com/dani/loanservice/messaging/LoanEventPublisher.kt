package com.dani.loanservice.messaging

import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
class LoanEventPublisher(private val rabbitTemplate: RabbitTemplate) {

    private val log = LoggerFactory.getLogger(LoanEventPublisher::class.java)

    fun publishLoanRequested(loanId: UUID, memberId: UUID, titleId: UUID) {
        publish(RabbitMQConfig.ROUTING_KEY_LOAN_REQUESTED, LoanRequestedPayload(loanId, memberId, titleId))
    }

    fun publishLoanApproved(loanId: UUID, memberId: UUID, titleId: UUID, copyId: UUID) {
        publish(RabbitMQConfig.ROUTING_KEY_LOAN_APPROVED, LoanApprovedPayload(loanId, memberId, titleId, copyId))
    }

    fun publishLoanRejected(loanId: UUID, memberId: UUID, titleId: UUID, reason: String) {
        publish(RabbitMQConfig.ROUTING_KEY_LOAN_REJECTED, LoanRejectedPayload(loanId, memberId, titleId, reason))
    }

    fun publishLoanStarted(loanId: UUID, memberId: UUID, titleId: UUID, copyId: UUID, dueDate: LocalDate) {
        publish(RabbitMQConfig.ROUTING_KEY_LOAN_STARTED, LoanStartedPayload(loanId, memberId, titleId, copyId, dueDate))
    }

    fun publishLoanEnded(loanId: UUID, memberId: UUID, titleId: UUID) {
        publish(RabbitMQConfig.ROUTING_KEY_LOAN_ENDED, LoanEndedPayload(loanId, memberId, titleId))
    }

    fun publishLoanCancelled(loanId: UUID, memberId: UUID, titleId: UUID) {
        publish(RabbitMQConfig.ROUTING_KEY_LOAN_CANCELLED, LoanCancelledPayload(loanId, memberId, titleId))
    }

    fun publishLoanDueReminder(loanId: UUID, memberId: UUID, titleId: UUID, dueDate: LocalDate) {
        publish(RabbitMQConfig.ROUTING_KEY_LOAN_DUE_REMINDER, LoanDueReminderPayload(loanId, memberId, titleId, dueDate))
    }

    fun publishCopyReservationRequested(loanId: UUID, titleId: UUID) {
        publish(RabbitMQConfig.ROUTING_KEY_COPY_RESERVATION_REQUESTED, CopyReservationRequestedPayload(loanId, titleId))
    }

    fun publishCopyReleaseRequested(loanId: UUID, copyId: UUID) {
        publish(RabbitMQConfig.ROUTING_KEY_COPY_RELEASE_REQUESTED, CopyReleaseRequestedPayload(loanId, copyId))
    }

    private fun <T : Any> publish(routingKey: String, payload: T) {
        val envelope = MessageEnvelope.of(routingKey, payload)
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, routingKey, envelope)
        log.debug("Published {}: {}", routingKey, payload)
    }

    // --- Payload types ---

    private data class LoanRequestedPayload(val loanId: UUID, val memberId: UUID, val titleId: UUID)
    private data class LoanApprovedPayload(val loanId: UUID, val memberId: UUID, val titleId: UUID, val copyId: UUID)
    private data class LoanRejectedPayload(val loanId: UUID, val memberId: UUID, val titleId: UUID, val reason: String)
    private data class LoanStartedPayload(val loanId: UUID, val memberId: UUID, val titleId: UUID, val copyId: UUID, val dueDate: LocalDate)
    private data class LoanEndedPayload(val loanId: UUID, val memberId: UUID, val titleId: UUID)
    private data class LoanCancelledPayload(val loanId: UUID, val memberId: UUID, val titleId: UUID)
    private data class LoanDueReminderPayload(val loanId: UUID, val memberId: UUID, val titleId: UUID, val dueDate: LocalDate)
    private data class CopyReservationRequestedPayload(val loanId: UUID, val titleId: UUID)
    private data class CopyReleaseRequestedPayload(val loanId: UUID, val copyId: UUID)
}