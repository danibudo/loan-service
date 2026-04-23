package com.dani.loanservice.exception

import java.util.UUID

class LoanNotFoundException(id: UUID) : RuntimeException("Loan not found: $id")