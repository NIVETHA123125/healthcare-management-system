package com.healthcare.system.controller;

import com.healthcare.system.entity.Notification;
import com.healthcare.system.entity.User;
import com.healthcare.system.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationApiController {

    @Autowired
    private NotificationService notificationService;

    private User getLoggedInUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }

    private String getNormalizedRole(User user) {
        String role = user.getRole();
        if (role.startsWith("ROLE_")) {
            return role.replace("ROLE_", "");
        }
        return role;
    }

    @GetMapping("/unread")
    public ResponseEntity<?> getUnread(HttpSession session) {
        User user = getLoggedInUser(session);
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        List<Notification> list = notificationService.getUnreadNotifications(user, getNormalizedRole(user));
        return ResponseEntity.ok(list);
    }

    @GetMapping("/unread/count")
    public ResponseEntity<?> getUnreadCount(HttpSession session) {
        User user = getLoggedInUser(session);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("count", 0));
        }
        long count = notificationService.countUnread(user, getNormalizedRole(user));
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/mark-read/{id}")
    public ResponseEntity<?> markRead(@PathVariable Long id, HttpSession session) {
        User user = getLoggedInUser(session);
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        boolean success = notificationService.markAsRead(id, user);
        return ResponseEntity.ok(Map.of("success", success));
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<?> markAllRead(HttpSession session) {
        User user = getLoggedInUser(session);
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        notificationService.markAllAsRead(user, getNormalizedRole(user));
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpSession session) {
        User user = getLoggedInUser(session);
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        boolean success = notificationService.deleteNotification(id, user);
        return ResponseEntity.ok(Map.of("success", success));
    }

    @PostMapping("/clear-all")
    public ResponseEntity<?> clearAll(HttpSession session) {
        User user = getLoggedInUser(session);
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        notificationService.clearAllNotifications(user, getNormalizedRole(user));
        return ResponseEntity.ok(Map.of("success", true));
    }
}
