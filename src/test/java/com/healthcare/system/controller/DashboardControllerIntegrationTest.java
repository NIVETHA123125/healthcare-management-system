package com.healthcare.system.controller;

import com.healthcare.system.entity.User;
import com.healthcare.system.service.AppointmentService;
import com.healthcare.system.service.DoctorService;
import com.healthcare.system.service.PatientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class DashboardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PatientService patientService;

    @MockBean
    private DoctorService doctorService;

    @MockBean
    private AppointmentService appointmentService;

    @Test
    @DisplayName("Should redirect unauthenticated user to /login")
    void testDashboard_Unauthenticated_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Should load dashboard view for authenticated ADMIN user")
    void testDashboard_AuthenticatedAdmin_ReturnsDashboardView() throws Exception {
        User adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setFullName("Admin User");
        adminUser.setRole("ROLE_ADMIN");

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUser", adminUser);

        when(patientService.getPatientCount()).thenReturn(10L);
        when(doctorService.getDoctorCount()).thenReturn(5L);
        when(appointmentService.getAppointmentCount()).thenReturn(20L);
        when(appointmentService.getTodayAppointments()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("patientCount", 10L))
                .andExpect(model().attribute("doctorCount", 5L))
                .andExpect(model().attribute("appointmentCount", 20L));
    }
}
