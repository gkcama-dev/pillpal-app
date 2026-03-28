package com.pillpal.app.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationItem {
    private String id, title, body, userId;
    private boolean isRead;
}
