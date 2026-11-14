package com.library.notification;

import java.time.LocalDateTime;

public record NotificationResponse(Long id, NotificationKind kind, String message, LocalDateTime sentAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getKind(), n.getMessage(), n.getSentAt());
    }
}
