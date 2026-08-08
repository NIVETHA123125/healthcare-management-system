package com.healthcare.system.config;

import com.healthcare.system.entity.User;
import com.healthcare.system.entity.Patient;
import com.healthcare.system.entity.UserSettings;
import com.healthcare.system.repository.UserRepository;
import com.healthcare.system.repository.PatientRepository;
import com.healthcare.system.repository.UserSettingsRepository;
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
import java.time.LocalDate;

@Component
public class SessionInterceptor implements HandlerInterceptor {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

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
                if (fUser.getPatient() == null && fUser.getRole() != null && fUser.getRole().endsWith("PATIENT")) {
                    Patient newPatient = new Patient();
                    String fullName = fUser.getFullName() != null ? fUser.getFullName() : "Patient";
                    String firstName = fullName;
                    String lastName = "";
                    int spaceIndex = fullName.trim().indexOf(' ');
                    if (spaceIndex > 0) {
                        firstName = fullName.substring(0, spaceIndex);
                        lastName = fullName.substring(spaceIndex + 1);
                    }
                    newPatient.setFirstName(firstName);
                    newPatient.setLastName(lastName);
                    newPatient.setEmail(fUser.getUsername());
                    newPatient.setPhone("");
                    newPatient.setGender("Other");
                    newPatient.setDob(LocalDate.now().minusYears(20));
                    newPatient.setAddress("");
                    newPatient = patientRepository.save(newPatient);
                    fUser.setPatient(newPatient);
                    userRepository.save(fUser);
                }
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

        if (session.getAttribute("userSettings") == null) {
            final User finalUser = user;
            UserSettings settings = transactionTemplate.execute(status -> {
                return userSettingsRepository.findByUserId(finalUser.getId())
                    .orElseGet(() -> {
                        UserSettings newSettings = new UserSettings();
                        newSettings.setUser(finalUser);
                        return userSettingsRepository.save(newSettings);
                    });
            });
            session.setAttribute("userSettings", settings);
        }

        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String relativeUri = uri.substring(contextPath.length());
        
        String role = user.getRole();
        if (role == null) {
            role = "";
        }
        
        boolean isAdmin = role.endsWith("ADMIN");
        boolean isPatient = role.endsWith("PATIENT");
        boolean isDoctor = role.endsWith("DOCTOR");
        boolean isReceptionist = role.endsWith("RECEPTIONIST");
        
        System.out.println("[DEBUG INTERCEPTOR] relativeUri: " + relativeUri + 
                           ", username: " + user.getUsername() +
                           ", role: " + role + 
                           ", isDoctor: " + isDoctor + 
                           ", isPatient: " + isPatient + 
                           ", isAdmin: " + isAdmin);

        // 1. Patient Portal protection: Only PATIENT can access /patient/**
        if (relativeUri.startsWith("/patient/")) {
            if (!isPatient) {
                if (isDoctor) {
                    response.sendRedirect(request.getContextPath() + "/doctor/dashboard");
                } else {
                    response.sendRedirect(request.getContextPath() + "/dashboard");
                }
                return false;
            }
        }
        
        // 2. Doctor Portal protection: Only DOCTOR can access /doctor/**
        // Exception: ADMIN is allowed to view-only patient records
        if (relativeUri.startsWith("/doctor/")) {
            if (!isDoctor) {
                if (isAdmin && relativeUri.startsWith("/doctor/patients/records/")) {
                    // Allow admin
                } else {
                    if (isPatient) {
                        response.sendRedirect(request.getContextPath() + "/patient/dashboard");
                    } else {
                        response.sendRedirect(request.getContextPath() + "/dashboard");
                    }
                    return false;
                }
            }
        }

        // 3. Consultation & Record management: Only DOCTOR can write or edit consultations
        if (relativeUri.startsWith("/consultations")) {
            if (!isDoctor) {
                if (isPatient) {
                    response.sendRedirect(request.getContextPath() + "/patient/dashboard");
                } else {
                    response.sendRedirect(request.getContextPath() + "/dashboard");
                }
                return false;
            }
        }

        // 4. Admin/Staff panel protection: Only ADMIN or RECEPTIONIST can access admin panels
        if (relativeUri.startsWith("/patients") || relativeUri.startsWith("/doctors") || 
            relativeUri.startsWith("/appointments") || relativeUri.startsWith("/dashboard") || relativeUri.startsWith("/payments")) {
            if (!isAdmin && !isReceptionist) {
                if (isPatient) {
                    response.sendRedirect(request.getContextPath() + "/patient/dashboard");
                } else if (isDoctor) {
                    response.sendRedirect(request.getContextPath() + "/doctor/dashboard");
                }
                return false;
            }
        }
        
        // 5. Restrict Doctor modification (add, edit, delete, save) to ADMIN only
        if (relativeUri.startsWith("/doctors/add") || relativeUri.startsWith("/doctors/edit") || 
            relativeUri.startsWith("/doctors/delete") || relativeUri.startsWith("/doctors/save")) {
            if (!isAdmin) {
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
