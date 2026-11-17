package bw.gov.dtef.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
public class DemoNotificationConsumer {
    private static final Logger log = LoggerFactory.getLogger(DemoNotificationConsumer.class);
    @RabbitListener(queues = NotificationQueueConfig.QUEUE)
    public void consume(NotificationQueueService.NotificationMessage message) {
        // Demonstration consumer acknowledges queue processing but does not send external SMS/email/WhatsApp.
        log.info("DEMO notification processed: id={}, template={}, channel={}, recipient={}",
            message.messageId(), message.templateId(), message.channel(), message.recipientSubject());
    }
}
