package com.dani.loanservice.service

import com.dani.loanservice.domain.Loan
import com.dani.loanservice.domain.LoanStatus
import com.dani.loanservice.domain.UserRole
import com.dani.loanservice.exception.InsufficientPermissionsException
import com.dani.loanservice.exception.InvalidStatusTransitionException
import com.dani.loanservice.exception.LoanNotFoundException
import com.dani.loanservice.messaging.LoanEventPublisher
import com.dani.loanservice.repository.LoanRepository
import com.dani.loanservice.repository.LoanSpecification
import com.dani.loanservice.web.CallerContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class LoanService(
    private val loanRepository: LoanRepository,
    private val eventPublisher: LoanEventPublisher,
    @Value("\${loan.due-days}") private val dueDays: Int,
) {
    private val log = LoggerFactory.getLogger(LoanService::class.java)

    @Transactional
    fun createLoan(caller: CallerContext, titleId: UUID, desiredPickupFrom: LocalDate, desiredPickupTo: LocalDate): Loan {
        if (caller.role != UserRole.MEMBER) {
            throw InsufficientPermissionsException("Only members can submit loan requests")
        }
        val loan = Loan(
            id = UUID.randomUUID(),
            memberId = caller.userId,
            titleId = titleId,
            status = LoanStatus.PENDING,
            desiredPickupFrom = desiredPickupFrom,
            desiredPickupTo = desiredPickupTo,
        )
        val saved = loanRepository.save(loan)
        log.info("Loan {} created by member {} for title {}", saved.id, caller.userId, titleId)
        eventPublisher.publishLoanRequested(saved.id, saved.memberId, saved.titleId)
        return saved
    }

    @Transactional(readOnly = true)
    fun getLoans(caller: CallerContext, status: LoanStatus?, memberId: UUID?, titleId: UUID?): List<Loan> {
        if (caller.role == UserRole.ACCESS_ADMIN) {
            throw InsufficientPermissionsException("Access admins cannot view loans")
        }
        val specs = mutableListOf<Specification<Loan>>()
        if (caller.role == UserRole.MEMBER) {
            specs += LoanSpecification.hasMemberId(caller.userId)
        } else {
            memberId?.let { specs += LoanSpecification.hasMemberId(it) }
        }
        status?.let { specs += LoanSpecification.hasStatus(it) }
        titleId?.let { specs += LoanSpecification.hasTitleId(it) }
        if (specs.isEmpty()) return loanRepository.findAll()
        return loanRepository.findAll(specs.reduce { a, b -> a.and(b) })
    }

    @Transactional(readOnly = true)
    fun getLoanById(caller: CallerContext, loanId: UUID): Loan {
        if (caller.role == UserRole.ACCESS_ADMIN) {
            throw InsufficientPermissionsException("Access admins cannot view loans")
        }
        val loan = loanRepository.findById(loanId).orElseThrow { LoanNotFoundException(loanId) }
        if (caller.role == UserRole.MEMBER && loan.memberId != caller.userId) {
            throw InsufficientPermissionsException("Members can only view their own loans")
        }
        return loan
    }

    @Transactional
    fun approveLoan(caller: CallerContext, loanId: UUID): Loan {
        if (!caller.canWrite()) {
            throw InsufficientPermissionsException("Only librarians and super-admins can approve loans")
        }
        val loan = loanRepository.findById(loanId).orElseThrow { LoanNotFoundException(loanId) }
        if (loan.status != LoanStatus.PENDING) {
            throw InvalidStatusTransitionException(loan.status, LoanStatus.AWAITING_COPY)
        }
        loan.status = LoanStatus.AWAITING_COPY
        val saved = loanRepository.save(loan)
        log.info("Loan {} approved by {}, awaiting copy reservation", loanId, caller.userId)
        eventPublisher.publishCopyReservationRequested(saved.id, saved.titleId)
        return saved
    }

    @Transactional
    fun rejectLoan(caller: CallerContext, loanId: UUID, reason: String): Loan {
        if (!caller.canWrite()) {
            throw InsufficientPermissionsException("Only librarians and super-admins can reject loans")
        }
        val loan = loanRepository.findById(loanId).orElseThrow { LoanNotFoundException(loanId) }
        if (loan.status != LoanStatus.PENDING) {
            throw InvalidStatusTransitionException(loan.status, LoanStatus.REJECTED)
        }
        loan.status = LoanStatus.REJECTED
        loan.rejectionReason = reason
        val saved = loanRepository.save(loan)
        log.info("Loan {} rejected by {}", loanId, caller.userId)
        eventPublisher.publishLoanRejected(saved.id, saved.memberId, saved.titleId, reason)
        return saved
    }

    @Transactional
    fun startLoan(caller: CallerContext, loanId: UUID): Loan {
        if (!caller.canWrite()) {
            throw InsufficientPermissionsException("Only librarians and super-admins can start loans")
        }
        val loan = loanRepository.findById(loanId).orElseThrow { LoanNotFoundException(loanId) }
        if (loan.status != LoanStatus.APPROVED) {
            throw InvalidStatusTransitionException(loan.status, LoanStatus.STARTED)
        }
        loan.status = LoanStatus.STARTED
        loan.dueDate = LocalDate.now().plusDays(dueDays.toLong())
        val saved = loanRepository.save(loan)
        log.info("Loan {} started, due date set to {}", loanId, saved.dueDate)
        eventPublisher.publishLoanStarted(saved.id, saved.memberId, saved.titleId, saved.copyId!!, saved.dueDate!!)
        return saved
    }

    @Transactional
    fun endLoan(caller: CallerContext, loanId: UUID): Loan {
        if (!caller.canWrite()) {
            throw InsufficientPermissionsException("Only librarians and super-admins can end loans")
        }
        val loan = loanRepository.findById(loanId).orElseThrow { LoanNotFoundException(loanId) }
        if (loan.status != LoanStatus.STARTED) {
            throw InvalidStatusTransitionException(loan.status, LoanStatus.ENDED)
        }
        loan.status = LoanStatus.ENDED
        val saved = loanRepository.save(loan)
        log.info("Loan {} ended by {}", loanId, caller.userId)
        eventPublisher.publishLoanEnded(saved.id, saved.memberId, saved.titleId)
        eventPublisher.publishCopyReleaseRequested(saved.id, saved.copyId!!)
        return saved
    }

    @Transactional
    fun cancelLoan(caller: CallerContext, loanId: UUID): Loan {
        if (caller.role != UserRole.MEMBER) {
            throw InsufficientPermissionsException("Only members can cancel loans")
        }
        val loan = loanRepository.findById(loanId).orElseThrow { LoanNotFoundException(loanId) }
        if (loan.memberId != caller.userId) {
            throw InsufficientPermissionsException("Members can only cancel their own loans")
        }
        if (loan.status != LoanStatus.PENDING && loan.status != LoanStatus.APPROVED) {
            throw InvalidStatusTransitionException(loan.status, LoanStatus.CANCELLED)
        }
        val wasApproved = loan.status == LoanStatus.APPROVED
        loan.status = LoanStatus.CANCELLED
        val saved = loanRepository.save(loan)
        log.info("Loan {} cancelled by member {}", loanId, caller.userId)
        eventPublisher.publishLoanCancelled(saved.id, saved.memberId, saved.titleId)
        if (wasApproved) eventPublisher.publishCopyReleaseRequested(saved.id, saved.copyId!!)
        return saved
    }
}