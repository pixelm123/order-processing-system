package com.orderprocessing.notification.controller;

import com.orderprocessing.notification.dto.NotificationResponse;
import com.orderprocessing.notification.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/{orderId}")
    public List<NotificationResponse> getNotifications(@PathVariable UUID orderId) {
        return notificationService.getNotificationsForOrder(orderId);
    }
}
