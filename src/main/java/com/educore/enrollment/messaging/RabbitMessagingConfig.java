package com.educore.enrollment.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
@ConditionalOnProperty(prefix = "educore.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMessagingConfig {
    public static final String DELAY_EXCHANGE = "educore.order.delay";
    public static final String DELAY_QUEUE = "educore.order.delay.queue";
    public static final String DELAY_ROUTING_KEY = "order.timeout.delay";
    public static final String EVENT_EXCHANGE = "educore.events";
    public static final String TIMEOUT_QUEUE = "educore.order.timeout.queue";
    public static final String TIMEOUT_ROUTING_KEY = "order.timeout";
    public static final String ERROR_EXCHANGE = "educore.events.dlx";
    public static final String ERROR_QUEUE = "educore.order.timeout.errors";

    @Bean Jackson2JsonMessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
    @Bean DirectExchange delayExchange() { return new DirectExchange(DELAY_EXCHANGE, true, false); }
    @Bean DirectExchange eventExchange() { return new DirectExchange(EVENT_EXCHANGE, true, false); }
    @Bean DirectExchange errorExchange() { return new DirectExchange(ERROR_EXCHANGE, true, false); }
    @Bean Queue delayQueue(@Value("${educore.enrollment.order-ttl-minutes:30}") long ttlMinutes) {
        int ttlMillis = (int) Math.min(Integer.MAX_VALUE, Math.max(60_000L, ttlMinutes * 60_000L));
        return QueueBuilder.durable(DELAY_QUEUE).withArgument("x-message-ttl", ttlMillis)
                .withArgument("x-dead-letter-exchange", EVENT_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", TIMEOUT_ROUTING_KEY).build();
    }
    @Bean Queue timeoutQueue() {
        return QueueBuilder.durable(TIMEOUT_QUEUE).withArgument("x-dead-letter-exchange", ERROR_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "order.timeout.failed").build();
    }
    @Bean Queue errorQueue() { return QueueBuilder.durable(ERROR_QUEUE).build(); }
    @Bean Binding delayBinding(Queue delayQueue, DirectExchange delayExchange) { return BindingBuilder.bind(delayQueue).to(delayExchange).with(DELAY_ROUTING_KEY); }
    @Bean Binding timeoutBinding(Queue timeoutQueue, DirectExchange eventExchange) { return BindingBuilder.bind(timeoutQueue).to(eventExchange).with(TIMEOUT_ROUTING_KEY); }
    @Bean Binding errorBinding(Queue errorQueue, DirectExchange errorExchange) { return BindingBuilder.bind(errorQueue).to(errorExchange).with("order.timeout.failed"); }
}
