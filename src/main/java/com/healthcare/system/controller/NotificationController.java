package com.healthcare.system.controller;

import com.healthcare.system.entity.Notification;
import com.healthcare.system.entity.User;
import com.healthcare.system.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@Controller
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/notifications")
    public String viewNotifications(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }
        
        String role = user.getRole();
        if (role.startsWith("ROLE_")) {
            role = role.replace("ROLE_", "");
        }
        
        List<Notification> list = notificationService.getHistory(user, role);
        model.addAttribute("notifications", list);
        model.addAttribute("pageTitle", "Notifications");
        model.addAttribute("headerTitle", "Notifications History");
        return "notifications-list";
    }
}
