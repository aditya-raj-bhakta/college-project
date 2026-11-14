package com.library.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/** Used until a mail server is configured: every message is written to the application log. */
@Component
@ConditionalOnMissingBean(value = NotificationSender.class, ignored = LoggingNotificationSender.class)
public class LoggingNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationSender.class);

    @Override
    public void send(String toName, String toEmail, String subject, String body) {
        log.info("[notification] to {} <{}>: {} | {}", toName, toEmail, subject, body);
    }
}
