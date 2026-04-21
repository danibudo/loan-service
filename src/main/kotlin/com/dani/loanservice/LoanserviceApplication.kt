package com.dani.loanservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LoanserviceApplication

fun main(args: Array<String>) {
    runApplication<LoanserviceApplication>(*args)
}
