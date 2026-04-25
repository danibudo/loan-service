package com.dani.loanservice.messaging

import com.dani.loanservice.domain.LoanStatus
import com.dani.loanservice.repository.LoanRepository
import org.slf4j.LoggerFactory
import org.springframework.amqp.AmqpRejectAndDontRequeueException
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class CatalogEventConsumer(
    private val loanRepository: LoanRepository,
    private val eventPublisher: LoanEventPublisher,
) {
    private val log = LoggerFactory.getLogger(CatalogEventConsumer::class.java)

    @RabbitListener(queues = [RabbitMQConfig.QUEUE_COPY_RESERVED])
    @Transactional
    fun handleCopyReserved(message: CopyReservedMessage) {
        val (loanId, copyId) = message.data
        val loan = loanRepository.findById(loanId).orElseThrow {
            AmqpRejectAndDontRequeueException("Loan not found: $loanId")
        }
        if (loan.status != LoanStatus.AWAITING_COPY) {
            log.warn("Loan {} not in awaiting_copy ({}), discarding copy_reserved", loanId, loan.status)
            throw AmqpRejectAndDontRequeueException("Loan $loanId not in awaiting_copy: ${loan.status}")
        }
        loan.copyId = copyId
        loan.status = LoanStatus.APPROVED
        loanRepository.save(loan)
        eventPublisher.publishLoanApproved(loan.id, loan.memberId, loan.titleId, copyId)
        log.info("Loan {} approved, copy {} reserved", loanId, copyId)
    }

    @RabbitListener(queues = [RabbitMQConfig.QUEUE_COPY_RESERVATION_FAILED])
    @Transactional
    fun handleCopyReservationFailed(message: CopyReservationFailedMessage) {
        val (loanId, _, reason) = message.data
        val loan = loanRepository.findById(loanId).orElse(null) ?: run {
            log.warn("Loan not found for copy_reservation_failed: {}, acking", loanId)
            return
        }
        if (loan.status != LoanStatus.AWAITING_COPY) {
            log.warn("Loan {} not in awaiting_copy ({}), acking copy_reservation_failed", loanId, loan.status)
            return
        }
        loan.status = LoanStatus.PENDING
        loanRepository.save(loan)
        log.info("Loan {} returned to pending after copy reservation failed: {}", loanId, reason)
    }

    // --- Incoming message shapes ---

    data class CopyReservedMessage(val event: String, val data: CopyReservedData, val metadata: EventMetadata)
    data class CopyReservedData(val loanId: UUID, val copyId: UUID, val titleId: UUID)

    data class CopyReservationFailedMessage(val event: String, val data: CopyReservationFailedData, val metadata: EventMetadata)
    data class CopyReservationFailedData(val loanId: UUID, val titleId: UUID, val reason: String)
}