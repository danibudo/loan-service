package com.dani.loanservice.web

import com.dani.loanservice.domain.UserRole
import com.dani.loanservice.exception.InsufficientPermissionsException
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import java.util.UUID

class CallerContextArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.parameterType == CallerContext::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): CallerContext {
        val userId = webRequest.getHeader("X-User-Id")
            ?: throw InsufficientPermissionsException("Missing required header: X-User-Id")
        val userRole = webRequest.getHeader("X-User-Role")
            ?: throw InsufficientPermissionsException("Missing required header: X-User-Role")

        return CallerContext(
            userId = UUID.fromString(userId),
            role = UserRole.fromValue(userRole)
        )
    }
}