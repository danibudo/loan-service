package com.dani.loanservice.repository

import com.dani.loanservice.domain.Loan
import com.dani.loanservice.domain.LoanStatus
import org.springframework.data.jpa.domain.Specification
import java.util.UUID

object LoanSpecification {
    fun hasStatus(status: LoanStatus): Specification<Loan> =
        Specification { root, _, cb -> cb.equal(root.get<LoanStatus>("status"), status) }

    fun hasMemberId(memberId: UUID): Specification<Loan> =
        Specification { root, _, cb -> cb.equal(root.get<UUID>("memberId"), memberId) }

    fun hasTitleId(titleId: UUID): Specification<Loan> =
        Specification { root, _, cb -> cb.equal(root.get<UUID>("titleId"), titleId) }
}