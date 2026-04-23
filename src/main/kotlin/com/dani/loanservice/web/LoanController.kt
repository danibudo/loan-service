package com.dani.loanservice.web

import com.dani.loanservice.domain.LoanStatus
import com.dani.loanservice.dto.CreateLoanRequest
import com.dani.loanservice.dto.LoanResponse
import com.dani.loanservice.dto.RejectLoanRequest
import com.dani.loanservice.service.LoanService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/loans")
class LoanController(private val loanService: LoanService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createLoan(
        @RequestBody @Valid request: CreateLoanRequest,
        caller: CallerContext
    ): LoanResponse =
        LoanResponse.from(
            loanService.createLoan(
                caller = caller,
                titleId = request.titleId,
                desiredPickupFrom = request.desiredPickupFrom,
                desiredPickupTo = request.desiredPickupTo
            )
        )

    @GetMapping
    fun getLoans(
        caller: CallerContext,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) memberId: UUID?,
        @RequestParam(required = false) titleId: UUID?
    ): List<LoanResponse> =
        loanService.getLoans(
            caller = caller,
            status = status?.let { LoanStatus.fromValue(it) },
            memberId = memberId,
            titleId = titleId
        ).map { LoanResponse.from(it) }

    @GetMapping("/{id}")
    fun getLoanById(
        @PathVariable id: UUID,
        caller: CallerContext
    ): LoanResponse =
        LoanResponse.from(loanService.getLoanById(caller, id))

    @PostMapping("/{id}/approve")
    fun approveLoan(
        @PathVariable id: UUID,
        caller: CallerContext
    ): LoanResponse =
        LoanResponse.from(loanService.approveLoan(caller, id))

    @PostMapping("/{id}/reject")
    fun rejectLoan(
        @PathVariable id: UUID,
        @RequestBody @Valid request: RejectLoanRequest,
        caller: CallerContext
    ): LoanResponse =
        LoanResponse.from(loanService.rejectLoan(caller, id, request.reason))

    @PostMapping("/{id}/start")
    fun startLoan(
        @PathVariable id: UUID,
        caller: CallerContext
    ): LoanResponse =
        LoanResponse.from(loanService.startLoan(caller, id))

    @PostMapping("/{id}/end")
    fun endLoan(
        @PathVariable id: UUID,
        caller: CallerContext
    ): LoanResponse =
        LoanResponse.from(loanService.endLoan(caller, id))

    @PostMapping("/{id}/cancel")
    fun cancelLoan(
        @PathVariable id: UUID,
        caller: CallerContext
    ): LoanResponse =
        LoanResponse.from(loanService.cancelLoan(caller, id))
}