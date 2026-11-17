package bw.gov.dtef.notification;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.UUID;

@Service
public class NotificationQueueService {
    private final RabbitTemplate rabbit;
    public NotificationQueueService(RabbitTemplate rabbit) { this.rabbit = rabbit; }
    public String enqueue(String recipientSubject, String templateId, String channel) {
        String messageId = UUID.randomUUID().toString();
        rabbit.convertAndSend(NotificationQueueConfig.QUEUE,
            new NotificationMessage(messageId, recipientSubject, templateId, channel, Instant.now().toString()),
            message -> {
                message.getMessageProperties().setDeliveryMode(org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT);
                return message;
            });
        return messageId;
    }
    public record NotificationMessage(String messageId, String recipientSubject, String templateId, String channel, String createdAt) {}
}
