package com.dani.loanservice.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

enum class LoanStatus(@get:JsonValue val value: String) {
    PENDING("pending"),
    AWAITING_COPY("awaiting_copy"),
    APPROVED("approved"),
    REJECTED("rejected"),
    STARTED("started"),
    ENDED("ended"),
    CANCELLED("cancelled");

    companion object {
        @JsonCreator
        @JvmStatic
        fun fromValue(value: String): LoanStatus =
            entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown loan status: $value")
    }
}

@Converter(autoApply = true)
class LoanStatusConverter : AttributeConverter<LoanStatus, String> {
    override fun convertToDatabaseColumn(attribute: LoanStatus): String = attribute.value
    override fun convertToEntityAttribute(dbData: String): LoanStatus = LoanStatus.fromValue(dbData)
}