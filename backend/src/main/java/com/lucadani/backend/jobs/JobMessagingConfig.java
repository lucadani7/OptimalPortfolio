package com.lucadani.backend.jobs;

import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

/**
 * RabbitMQ topology for optimization jobs, declared on startup:
 * <ul>
 *   <li>job ids go to the {@value #JOBS_QUEUE} queue, which every consuming instance shares;</li>
 *   <li>messages that keep failing move to the {@value #DEAD_LETTER_QUEUE} queue, to inspect by hand;</li>
 *   <li>job events go to the {@value #EVENTS_EXCHANGE} fanout exchange, and every web instance
 *       reads them from a queue of its own, so each can update its own SSE subscribers.</li>
 * </ul>
 */
@Configuration
public class JobMessagingConfig {

    public static final String JOBS_EXCHANGE = "optimization.jobs";
    public static final String JOBS_QUEUE = "optimization.jobs";
    public static final String DEAD_LETTER_EXCHANGE = "optimization.jobs.dlx";
    public static final String DEAD_LETTER_QUEUE = "optimization.jobs.dead";
    public static final String EVENTS_EXCHANGE = "optimization.events";

    /** Header with the id of the instance that sent an event. */
    public static final String ORIGIN_HEADER = "x-origin";

    /** Identifies this instance, so it can skip the events it sent itself. */
    public static final String INSTANCE_ID = UUID.randomUUID().toString();

    @Bean
    public DirectExchange jobsExchange() {
        return new DirectExchange(JOBS_EXCHANGE);
    }

    @Bean
    public Queue jobsQueue() {
        return QueueBuilder.durable(JOBS_QUEUE).deadLetterExchange(DEAD_LETTER_EXCHANGE).build();
    }

    @Bean
    public Binding jobsBinding(Queue jobsQueue, DirectExchange jobsExchange) {
        return BindingBuilder.bind(jobsQueue).to(jobsExchange).with(JOBS_QUEUE);
    }

    @Bean
    public FanoutExchange deadLetterExchange() {
        return new FanoutExchange(DEAD_LETTER_EXCHANGE);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, FanoutExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange);
    }

    @Bean
    public FanoutExchange eventsExchange() {
        return new FanoutExchange(EVENTS_EXCHANGE);
    }

    /** Exclusive to this instance and deleted when it stops; events only matter while someone listens. */
    @Bean
    @ConditionalOnWebApplication
    public Queue jobEventsQueue() {
        return new AnonymousQueue();
    }

    @Bean
    @ConditionalOnWebApplication
    public Binding jobEventsBinding(Queue jobEventsQueue, FanoutExchange eventsExchange) {
        return BindingBuilder.bind(jobEventsQueue).to(eventsExchange);
    }
}
