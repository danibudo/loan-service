package com.dani.loanservice.dto

import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.util.UUID

data class CreateLoanRequest(
    @field:NotNull val titleId: UUID,
    @field:NotNull @field:FutureOrPresent val desiredPickupFrom: LocalDate,
    @field:NotNull val desiredPickupTo: LocalDate
) {
    @AssertTrue(message = "desired_pickup_to must be on or after desired_pickup_from")
    fun isPickupRangeValid(): Boolean =
        desiredPickupFrom == null || desiredPickupTo == null || !desiredPickupTo.isBefore(desiredPickupFrom)
}