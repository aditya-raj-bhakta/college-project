package com.library.notification;

/**
 * Delivers a message to a member. The default implementation writes to the log;
 * swap in an email or SMS sender by providing another bean of this type.
 */
public interface NotificationSender {

    void send(String toName, String toEmail, String subject, String body);
}
