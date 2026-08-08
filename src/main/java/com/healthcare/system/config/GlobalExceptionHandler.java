package com.healthcare.system.config;

import com.healthcare.system.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    private NotificationService notificationService;

    @ExceptionHandler(Exception.class)
    public ModelAndView handleAllExceptions(Exception ex, HttpServletRequest request) {
        try {
            String url = request.getRequestURL().toString();
            String errMsg = ex.getMessage();
            if (errMsg == null || errMsg.isEmpty()) {
                errMsg = ex.getClass().getSimpleName();
            }
            if (errMsg.length() > 300) {
                errMsg = errMsg.substring(0, 300) + "...";
            }
            notificationService.createNotification(null, "ADMIN", "Critical System Error", 
                "An unhandled error occurred at " + url + ": " + errMsg, "SYSTEM", null);
        } catch (Exception e) {
            // Safe fallback: do not crash in exception handling
        }

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("error");
        modelAndView.addObject("message", ex.getMessage());
        return modelAndView;
    }
}
