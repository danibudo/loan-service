package com.dani.loanservice.scheduler

import com.dani.loanservice.domain.LoanStatus
import com.dani.loanservice.messaging.LoanEventPublisher
import com.dani.loanservice.repository.LoanRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class LoanReminderScheduler(
    private val loanRepository: LoanRepository,
    private val eventPublisher: LoanEventPublisher,
    @Value("\${loan.reminder.days-before}") private val daysBefore: Int,
) {
    private val log = LoggerFactory.getLogger(LoanReminderScheduler::class.java)

    @Scheduled(cron = "0 0 8 * * *")
    fun sendDueReminders() {
        val targetDate = LocalDate.now().plusDays(daysBefore.toLong())
        val loans = loanRepository.findAllByStatusAndDueDate(LoanStatus.STARTED, targetDate)
        loans.forEach { loan ->
            eventPublisher.publishLoanDueReminder(loan.id, loan.memberId, loan.titleId, loan.dueDate!!)
            log.info("Due reminder sent for loan {}, due {}", loan.id, loan.dueDate)
        }
        log.info("Due reminder job completed: {} loan(s) notified for due date {}", loans.size, targetDate)
    }
}