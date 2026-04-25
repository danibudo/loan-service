package com.dani.loanservice.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {

    // --- Exchanges ---
    @Bean
    fun loanServiceExchange(): TopicExchange =
        TopicExchange(EXCHANGE, true, false)

    @Bean
    fun deadLetterExchange(): DirectExchange =
        DirectExchange(DLX, true, false)

    @Bean
    fun catalogServiceExchange(): TopicExchange =
        TopicExchange(CATALOG_EXCHANGE, true, false)

    // --- Notification queues ---
    @Bean fun loanRequestedQueue()   = buildQueue(QUEUE_LOAN_REQUESTED)
    @Bean fun loanApprovedQueue()    = buildQueue(QUEUE_LOAN_APPROVED)
    @Bean fun loanRejectedQueue()    = buildQueue(QUEUE_LOAN_REJECTED)
    @Bean fun loanStartedQueue()     = buildQueue(QUEUE_LOAN_STARTED)
    @Bean fun loanEndedQueue()       = buildQueue(QUEUE_LOAN_ENDED)
    @Bean fun loanCancelledQueue()   = buildQueue(QUEUE_LOAN_CANCELLED)
    @Bean fun loanDueReminderQueue() = buildQueue(QUEUE_LOAN_DUE_REMINDER)

    // --- Notification DLQs ---
    @Bean fun loanRequestedDlq()   = buildDlq(QUEUE_LOAN_REQUESTED)
    @Bean fun loanApprovedDlq()    = buildDlq(QUEUE_LOAN_APPROVED)
    @Bean fun loanRejectedDlq()    = buildDlq(QUEUE_LOAN_REJECTED)
    @Bean fun loanStartedDlq()     = buildDlq(QUEUE_LOAN_STARTED)
    @Bean fun loanEndedDlq()       = buildDlq(QUEUE_LOAN_ENDED)
    @Bean fun loanCancelledDlq()   = buildDlq(QUEUE_LOAN_CANCELLED)
    @Bean fun loanDueReminderDlq() = buildDlq(QUEUE_LOAN_DUE_REMINDER)

    // --- Bindings: notification queues → loan-service.events ---
    @Bean fun loanRequestedBinding()   = bind(loanRequestedQueue(),   ROUTING_KEY_LOAN_REQUESTED)
    @Bean fun loanApprovedBinding()    = bind(loanApprovedQueue(),    ROUTING_KEY_LOAN_APPROVED)
    @Bean fun loanRejectedBinding()    = bind(loanRejectedQueue(),    ROUTING_KEY_LOAN_REJECTED)
    @Bean fun loanStartedBinding()     = bind(loanStartedQueue(),     ROUTING_KEY_LOAN_STARTED)
    @Bean fun loanEndedBinding()       = bind(loanEndedQueue(),       ROUTING_KEY_LOAN_ENDED)
    @Bean fun loanCancelledBinding()   = bind(loanCancelledQueue(),   ROUTING_KEY_LOAN_CANCELLED)
    @Bean fun loanDueReminderBinding() = bind(loanDueReminderQueue(), ROUTING_KEY_LOAN_DUE_REMINDER)

    // --- Bindings: notification DLQs → DLX ---
    @Bean fun loanRequestedDlqBinding()   = bindDlq(QUEUE_LOAN_REQUESTED)
    @Bean fun loanApprovedDlqBinding()    = bindDlq(QUEUE_LOAN_APPROVED)
    @Bean fun loanRejectedDlqBinding()    = bindDlq(QUEUE_LOAN_REJECTED)
    @Bean fun loanStartedDlqBinding()     = bindDlq(QUEUE_LOAN_STARTED)
    @Bean fun loanEndedDlqBinding()       = bindDlq(QUEUE_LOAN_ENDED)
    @Bean fun loanCancelledDlqBinding()   = bindDlq(QUEUE_LOAN_CANCELLED)
    @Bean fun loanDueReminderDlqBinding() = bindDlq(QUEUE_LOAN_DUE_REMINDER)

    // --- Message converter ---
    @Bean
    fun messageConverter(objectMapper: ObjectMapper): MessageConverter =
        Jackson2JsonMessageConverter(objectMapper)

    // --- Listener container factory ---
    @Bean
    fun rabbitListenerContainerFactory(
        connectionFactory: ConnectionFactory,
        objectMapper: ObjectMapper,
        @Value("\${rabbitmq.prefetch:10}") prefetch: Int,
    ): SimpleRabbitListenerContainerFactory {
        val converter = Jackson2JsonMessageConverter(objectMapper)
        converter.setAlwaysConvertToInferredType(true)
        return SimpleRabbitListenerContainerFactory().apply {
            setConnectionFactory(connectionFactory)
            setMessageConverter(converter)
            setPrefetchCount(prefetch)
        }
    }

    // --- Helpers ---
    private fun buildQueue(name: String): Queue =
        QueueBuilder.durable(name).deadLetterExchange(DLX).deadLetterRoutingKey(name).build()

    private fun buildDlq(queueName: String): Queue =
        QueueBuilder.durable("$queueName.dlq").build()

    private fun bind(queue: Queue, routingKey: String): Binding =
        BindingBuilder.bind(queue).to(loanServiceExchange()).with(routingKey)

    private fun bindCatalog(queue: Queue, routingKey: String): Binding =
        BindingBuilder.bind(queue).to(catalogServiceExchange()).with(routingKey)

    private fun bindDlq(queueName: String): Binding =
        BindingBuilder.bind(buildDlq(queueName)).to(deadLetterExchange()).with(queueName)

    // --- Saga trigger queues (loan-service publishes) ---
    @Bean fun copyReservationRequestedQueue() = buildQueue(QUEUE_COPY_RESERVATION_REQUESTED)
    @Bean fun copyReleaseRequestedQueue()     = buildQueue(QUEUE_COPY_RELEASE_REQUESTED)

    // --- Saga trigger DLQs ---
    @Bean fun copyReservationRequestedDlq() = buildDlq(QUEUE_COPY_RESERVATION_REQUESTED)
    @Bean fun copyReleaseRequestedDlq()     = buildDlq(QUEUE_COPY_RELEASE_REQUESTED)

    // --- Bindings: saga trigger queues → loan-service.events ---
    @Bean fun copyReservationRequestedBinding() = bind(copyReservationRequestedQueue(), ROUTING_KEY_COPY_RESERVATION_REQUESTED)
    @Bean fun copyReleaseRequestedBinding()     = bind(copyReleaseRequestedQueue(),     ROUTING_KEY_COPY_RELEASE_REQUESTED)

    // --- Bindings: saga trigger DLQs → DLX ---
    @Bean fun copyReservationRequestedDlqBinding() = bindDlq(QUEUE_COPY_RESERVATION_REQUESTED)
    @Bean fun copyReleaseRequestedDlqBinding()     = bindDlq(QUEUE_COPY_RELEASE_REQUESTED)

    // --- Saga response queues (catalog-service publishes, loan-service consumes) ---
    @Bean fun copyReservedQueue()           = buildQueue(QUEUE_COPY_RESERVED)
    @Bean fun copyReservationFailedQueue()  = buildQueue(QUEUE_COPY_RESERVATION_FAILED)
    @Bean fun copyReleasedQueue()           = buildQueue(QUEUE_COPY_RELEASED)

    // --- Saga response DLQs ---
    @Bean fun copyReservedDlq()           = buildDlq(QUEUE_COPY_RESERVED)
    @Bean fun copyReservationFailedDlq()  = buildDlq(QUEUE_COPY_RESERVATION_FAILED)
    @Bean fun copyReleasedDlq()           = buildDlq(QUEUE_COPY_RELEASED)

    // --- Bindings: saga response queues → catalog-service.events ---
    @Bean fun copyReservedBinding()          = bindCatalog(copyReservedQueue(),          ROUTING_KEY_COPY_RESERVED)
    @Bean fun copyReservationFailedBinding() = bindCatalog(copyReservationFailedQueue(), ROUTING_KEY_COPY_RESERVATION_FAILED)
    @Bean fun copyReleasedBinding()          = bindCatalog(copyReleasedQueue(),           ROUTING_KEY_COPY_RELEASED)

    // --- Bindings: saga response DLQs → DLX ---
    @Bean fun copyReservedDlqBinding()          = bindDlq(QUEUE_COPY_RESERVED)
    @Bean fun copyReservationFailedDlqBinding() = bindDlq(QUEUE_COPY_RESERVATION_FAILED)
    @Bean fun copyReleasedDlqBinding()          = bindDlq(QUEUE_COPY_RELEASED)

    companion object {
        const val EXCHANGE         = "loan-service.events"
        const val CATALOG_EXCHANGE = "catalog-service.events"
        const val DLX              = "dlx.loan-service"

        // Notification queues
        const val QUEUE_LOAN_REQUESTED    = "loan-service.loan.loan_requested"
        const val QUEUE_LOAN_APPROVED     = "loan-service.loan.loan_approved"
        const val QUEUE_LOAN_REJECTED     = "loan-service.loan.loan_rejected"
        const val QUEUE_LOAN_STARTED      = "loan-service.loan.loan_started"
        const val QUEUE_LOAN_ENDED        = "loan-service.loan.loan_ended"
        const val QUEUE_LOAN_CANCELLED    = "loan-service.loan.loan_cancelled"
        const val QUEUE_LOAN_DUE_REMINDER = "loan-service.loan.loan_due_reminder"

        const val ROUTING_KEY_LOAN_REQUESTED    = "loan.loan_requested"
        const val ROUTING_KEY_LOAN_APPROVED     = "loan.loan_approved"
        const val ROUTING_KEY_LOAN_REJECTED     = "loan.loan_rejected"
        const val ROUTING_KEY_LOAN_STARTED      = "loan.loan_started"
        const val ROUTING_KEY_LOAN_ENDED        = "loan.loan_ended"
        const val ROUTING_KEY_LOAN_CANCELLED    = "loan.loan_cancelled"
        const val ROUTING_KEY_LOAN_DUE_REMINDER = "loan.loan_due_reminder"

        // Saga trigger queues
        const val QUEUE_COPY_RESERVATION_REQUESTED = "loan-service.loan.copy_reservation_requested"
        const val QUEUE_COPY_RELEASE_REQUESTED     = "loan-service.loan.copy_release_requested"

        const val ROUTING_KEY_COPY_RESERVATION_REQUESTED = "loan.copy_reservation_requested"
        const val ROUTING_KEY_COPY_RELEASE_REQUESTED     = "loan.copy_release_requested"

        // Saga response queues
        const val QUEUE_COPY_RESERVED           = "loan-service.catalog.copy_reserved"
        const val QUEUE_COPY_RESERVATION_FAILED = "loan-service.catalog.copy_reservation_failed"
        const val QUEUE_COPY_RELEASED           = "loan-service.catalog.copy_released"

        const val ROUTING_KEY_COPY_RESERVED           = "catalog.copy_reserved"
        const val ROUTING_KEY_COPY_RESERVATION_FAILED = "catalog.copy_reservation_failed"
        const val ROUTING_KEY_COPY_RELEASED           = "catalog.copy_released"
    }
}