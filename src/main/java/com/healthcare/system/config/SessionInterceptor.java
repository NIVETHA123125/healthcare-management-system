package com.healthcare.system.config;

import com.healthcare.system.entity.User;
import com.healthcare.system.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import java.util.Optional;

@Component
public class SessionInterceptor implements HandlerInterceptor {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Prevent back-button caching of protected pages
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0
        response.setDateHeader("Expires", 0); // Proxies

        HttpSession session = request.getSession(false);
        
        // If there's no session or no loggedInUser attribute, redirect to login
        if (session == null || session.getAttribute("loggedInUser") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }
        
        final User userVal = (User) session.getAttribute("loggedInUser");
        final Long userId = userVal.getId();
        
        // Reload user and force lazy initialization inside a transaction to prevent LazyInitializationException
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        User freshUser = transactionTemplate.execute(status -> {
            Optional<User> freshUserOpt = userRepository.findById(userId);
            if (freshUserOpt.isPresent()) {
                User fUser = freshUserOpt.get();
                if (fUser.getPatient() != null) {
                    fUser.getPatient().getEmail(); // Force lazy loading
                    fUser.getPatient().getFirstName(); // Ensure fields are initialized
                }
                if (fUser.getDoctor() != null) {
                    fUser.getDoctor().getEmail(); // Force lazy loading
                    fUser.getDoctor().getFirstName(); // Ensure fields are initialized
                }
                return fUser;
            }
            return null;
        });

        User user = userVal;
        if (freshUser != null) {
            session.setAttribute("loggedInUser", freshUser);
            user = freshUser;
        }

        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String relativeUri = uri.substring(contextPath.length());
        
        String role = user.getRole();
        
        // 1. Patient Portal protection: Only ROLE_PATIENT can access /patient/**
        if (relativeUri.startsWith("/patient/")) {
            if (!"ROLE_PATIENT".equals(role)) {
                response.sendRedirect(request.getContextPath() + "/dashboard");
                return false;
            }
        }
        
        // 2. Doctor Portal protection: Only ROLE_DOCTOR or ROLE_ADMIN can access /consultations/**
        if (relativeUri.startsWith("/consultations")) {
            if (!"ROLE_DOCTOR".equals(role) && !"ROLE_ADMIN".equals(role)) {
                response.sendRedirect(request.getContextPath() + "/dashboard");
                return false;
            }
        }

        // 3. Billing protection: Only ROLE_RECEPTIONIST or ROLE_ADMIN can access /payments/**
        if (relativeUri.startsWith("/payments")) {
            if (!"ROLE_RECEPTIONIST".equals(role) && !"ROLE_ADMIN".equals(role)) {
                response.sendRedirect(request.getContextPath() + "/dashboard");
                return false;
            }
        }

        // 4. Staff panel protection: Block ROLE_PATIENT from accessing staff panels
        if (relativeUri.startsWith("/patients") || relativeUri.startsWith("/doctors") || 
            relativeUri.startsWith("/appointments") || relativeUri.startsWith("/dashboard")) {
            if ("ROLE_PATIENT".equals(role)) {
                response.sendRedirect(request.getContextPath() + "/patient/dashboard");
                return false;
            }
        }
        
        // 5. Restrict Doctor modification (add, edit, delete, save) to ROLE_ADMIN only
        if (relativeUri.startsWith("/doctors/add") || relativeUri.startsWith("/doctors/edit") || 
            relativeUri.startsWith("/doctors/delete") || relativeUri.startsWith("/doctors/save")) {
            if (!"ROLE_ADMIN".equals(role)) {
                response.sendRedirect(request.getContextPath() + "/doctors");
                return false;
            }
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        if (modelAndView != null) {
            modelAndView.addObject("currentUri", request.getRequestURI());
        }
    }
}
