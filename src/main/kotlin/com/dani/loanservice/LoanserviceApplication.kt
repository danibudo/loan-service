package com.dani.loanservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class LoanserviceApplication

fun main(args: Array<String>) {
    runApplication<LoanserviceApplication>(*args)
}
