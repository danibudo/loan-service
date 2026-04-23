package com.dani.loanservice.dto

import com.dani.loanservice.domain.Loan
import com.dani.loanservice.domain.LoanStatus
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class LoanResponse(
    val id: UUID,
    val memberId: UUID,
    val titleId: UUID,
    val copyId: UUID?,
    val status: LoanStatus,
    val desiredPickupFrom: LocalDate,
    val desiredPickupTo: LocalDate,
    val rejectionReason: String?,
    val dueDate: LocalDate?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime
) {
    companion object {
        fun from(loan: Loan) = LoanResponse(
            id = loan.id,
            memberId = loan.memberId,
            titleId = loan.titleId,
            copyId = loan.copyId,
            status = loan.status,
            desiredPickupFrom = loan.desiredPickupFrom,
            desiredPickupTo = loan.desiredPickupTo,
            rejectionReason = loan.rejectionReason,
            dueDate = loan.dueDate,
            createdAt = loan.createdAt,
            updatedAt = loan.updatedAt
        )
    }
}