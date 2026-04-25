package com.dani.loanservice.client

import com.dani.loanservice.exception.CatalogServiceUnavailableException
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.util.UUID

@Component
class CatalogClient(
    @Value("\${catalog.service.url}") baseUrl: String,
    private val circuitBreakerFactory: CircuitBreakerFactory<*, *>,
) {
    private val webClient = WebClient.builder().baseUrl(baseUrl).build()

    fun titleExists(titleId: UUID): Boolean {
        val cb = circuitBreakerFactory.create("catalog-client")
        return cb.run(
            {
                webClient.get()
                    .uri("/titles/{id}", titleId)
                    .exchangeToMono { response ->
                        when {
                            response.statusCode().is2xxSuccessful ->
                                response.releaseBody().thenReturn(true)
                            response.statusCode() == HttpStatus.NOT_FOUND ->
                                response.releaseBody().thenReturn(false)
                            else -> response.createError()
                        }
                    }
                    .block()!!
            },
            { throw CatalogServiceUnavailableException() },
        )
    }
}