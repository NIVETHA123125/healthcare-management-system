package com.healthcare.system.service;

import com.healthcare.system.entity.Notification;
import com.healthcare.system.entity.User;
import com.healthcare.system.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    public Notification createNotification(User user, String role, String title, String message, String type, Long referenceId) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setRole(role);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setReferenceId(referenceId);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    public List<Notification> getUnreadNotifications(User user, String role) {
        return notificationRepository.findByUserOrRoleAndReadOrderByCreatedAtDesc(user, role, false);
    }

    public long countUnread(User user, String role) {
        return notificationRepository.countByUserOrRoleAndRead(user, role, false);
    }

    public List<Notification> getHistory(User user, String role) {
        return notificationRepository.findByUserOrRoleOrderByCreatedAtDesc(user, role);
    }

    public boolean markAsRead(Long id, User user) {
        Optional<Notification> opt = notificationRepository.findById(id);
        if (opt.isPresent()) {
            Notification notification = opt.get();
            // Check ownership or role access
            if (notification.getUser() == null || notification.getUser().getId().equals(user.getId()) || notification.getRole().equals(user.getRole())) {
                notification.setRead(true);
                notificationRepository.save(notification);
                return true;
            }
        }
        return false;
    }

    public void markAllAsRead(User user, String role) {
        List<Notification> unread = notificationRepository.findByUserOrRoleAndReadOrderByCreatedAtDesc(user, role, false);
        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }

    public boolean deleteNotification(Long id, User user) {
        Optional<Notification> opt = notificationRepository.findById(id);
        if (opt.isPresent()) {
            Notification notification = opt.get();
            if (notification.getUser() == null || notification.getUser().getId().equals(user.getId()) || notification.getRole().equals(user.getRole())) {
                notificationRepository.delete(notification);
                return true;
            }
        }
        return false;
    }

    public void clearAllNotifications(User user, String role) {
        List<Notification> all = notificationRepository.findByUserOrRoleOrderByCreatedAtDesc(user, role);
        notificationRepository.deleteAll(all);
    }

    public boolean existsReminder(Long userId, String type, Long referenceId) {
        return notificationRepository.existsByUserIdAndTypeAndReferenceId(userId, type, referenceId);
    }
}
