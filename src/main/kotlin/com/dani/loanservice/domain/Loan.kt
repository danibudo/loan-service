package com.dani.loanservice.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "loans")
class Loan(
    @Id
    val id: UUID,

    @Column(nullable = false)
    val memberId: UUID,

    @Column(nullable = false)
    val titleId: UUID,

    var copyId: UUID? = null,

    @Column(nullable = false)
    var status: LoanStatus,

    @Column(nullable = false)
    val desiredPickupFrom: LocalDate,

    @Column(nullable = false)
    val desiredPickupTo: LocalDate,

    var rejectionReason: String? = null,

    var dueDate: LocalDate? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(nullable = false)
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
) {
    init {
        status = LoanStatus.PENDING
    }
}