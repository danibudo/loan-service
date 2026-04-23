package com.dani.loanservice.web

import com.dani.loanservice.domain.UserRole
import java.util.UUID

data class CallerContext(
    val userId: UUID,
    val role: UserRole
) {
    fun canWrite() = role == UserRole.LIBRARIAN || role == UserRole.SUPER_ADMIN
}