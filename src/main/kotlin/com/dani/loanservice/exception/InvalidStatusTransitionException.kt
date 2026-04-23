package com.dani.loanservice.exception

import com.dani.loanservice.domain.LoanStatus

class InvalidStatusTransitionException(current: LoanStatus, target: LoanStatus) :
    RuntimeException("Cannot transition loan from '${current.value}' to '${target.value}'")