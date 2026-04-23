package com.dani.loanservice.repository

import com.dani.loanservice.domain.Loan
import com.dani.loanservice.domain.LoanStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.time.LocalDate
import java.util.UUID

interface LoanRepository : JpaRepository<Loan, UUID>, JpaSpecificationExecutor<Loan> {
    fun findByIdAndMemberId(id: UUID, memberId: UUID): Loan?
    fun findAllByStatusAndDueDate(status: LoanStatus, dueDate: LocalDate): List<Loan>
}