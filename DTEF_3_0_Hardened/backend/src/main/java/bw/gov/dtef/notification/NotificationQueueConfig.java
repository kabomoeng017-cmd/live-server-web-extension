package bw.gov.dtef.notification;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationQueueConfig {
    public static final String QUEUE = "dtef.notifications.v1";
    public static final String DEAD_LETTER_QUEUE = "dtef.notifications.dlq";
    @Bean Queue notificationDeadLetterQueue() { return QueueBuilder.durable(DEAD_LETTER_QUEUE).build(); }
    @Bean Queue notificationQueue() {
        return QueueBuilder.durable(QUEUE).deadLetterExchange("").deadLetterRoutingKey(DEAD_LETTER_QUEUE).build();
    }
    @Bean Jackson2JsonMessageConverter jacksonMessageConverter() { return new Jackson2JsonMessageConverter(); }
}
