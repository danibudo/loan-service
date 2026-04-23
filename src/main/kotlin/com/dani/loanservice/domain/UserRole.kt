package com.dani.loanservice.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class UserRole(@get:JsonValue val value: String) {
    MEMBER("member"),
    LIBRARIAN("librarian"),
    ACCESS_ADMIN("access-admin"),
    SUPER_ADMIN("super-admin");

    companion object {
        @JsonCreator
        @JvmStatic
        fun fromValue(value: String): UserRole =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown role: $value")
    }
}