package com.dani.loanservice.web

import com.dani.loanservice.dto.ErrorResponse
import com.dani.loanservice.exception.CatalogServiceUnavailableException
import com.dani.loanservice.exception.InsufficientPermissionsException
import com.dani.loanservice.exception.InvalidStatusTransitionException
import com.dani.loanservice.exception.LoanNotFoundException
import com.dani.loanservice.exception.TitleNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(LoanNotFoundException::class, TitleNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: RuntimeException): ErrorResponse =
        ErrorResponse(HttpStatus.NOT_FOUND, ex.message ?: "Not found")

    @ExceptionHandler(InsufficientPermissionsException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleForbidden(ex: InsufficientPermissionsException): ErrorResponse {
        log.warn("Forbidden: {}", ex.message)
        return ErrorResponse(HttpStatus.FORBIDDEN, ex.message ?: "Forbidden")
    }

    @ExceptionHandler(InvalidStatusTransitionException::class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    fun handleInvalidTransition(ex: InvalidStatusTransitionException): ErrorResponse {
        log.warn("Invalid status transition: {}", ex.message)
        return ErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.message ?: "Unprocessable entity")
    }

    @ExceptionHandler(CatalogServiceUnavailableException::class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    fun handleServiceUnavailable(ex: CatalogServiceUnavailableException): ErrorResponse {
        log.error("Catalog service unavailable: {}", ex.message)
        return ErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.message ?: "Service unavailable")
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidation(ex: MethodArgumentNotValidException): ErrorResponse {
        val message = ex.bindingResult.fieldErrors
            .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return ErrorResponse(HttpStatus.BAD_REQUEST, message)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleUnreadableMessage(ex: HttpMessageNotReadableException): ErrorResponse =
        ErrorResponse(HttpStatus.BAD_REQUEST, "Malformed request body")

    @ExceptionHandler(MethodArgumentTypeMismatchException::class, IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleBadRequest(ex: Exception): ErrorResponse {
        log.warn("Bad request: {}", ex.message)
        return ErrorResponse(HttpStatus.BAD_REQUEST, ex.message ?: "Bad request")
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGeneric(ex: Exception): ErrorResponse {
        log.error("Unhandled exception", ex)
        return ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred")
    }
}