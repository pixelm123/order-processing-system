package com.orderprocessing.notification.repository;

import com.orderprocessing.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByOrderIdOrderByCreatedAtDesc(UUID orderId);
}
