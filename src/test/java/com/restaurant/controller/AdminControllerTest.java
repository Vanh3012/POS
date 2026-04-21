package com.restaurant.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.dto.request.CreateUserRequest;
import com.restaurant.dto.response.AdminUserDetailDTO;
import com.restaurant.exception.GlobalExceptionHandler;
import com.restaurant.service.AdminService;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminController(adminService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void createUserIgnoresLegacyTimestampsInPayload() throws Exception {
        when(adminService.createUser(any(CreateUserRequest.class))).thenReturn(
                AdminUserDetailDTO.builder()
                        .userId(1L)
                        .name("Admin")
                        .email("admin@example.com")
                        .role("ADMIN")
                        .startTime(LocalTime.of(8, 0))
                        .endTime(LocalTime.of(17, 0))
                        .active(true)
                        .build());

        mockMvc.perform(post("/admin/create-user")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Admin",
                          "email": "admin@example.com",
                          "password": "123456",
                          "role": "ADMIN",
                          "startTime": "08:00",
                          "endTime": "17:00",
                          "active": true,
                          "createdAt": "2026-04-21T10:00:00",
                          "updatedAt": "2026-04-21T10:05:00"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        verify(adminService).createUser(any(CreateUserRequest.class));
    }

    @Test
    void createUserReturnsBadRequestWhenStartTimeIsInvalid() throws Exception {
        mockMvc.perform(post("/admin/create-user")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Admin",
                          "email": "admin@example.com",
                          "password": "123456",
                          "role": "ADMIN",
                          "startTime": "08h00"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("START_TIME_INVALID"))
                .andExpect(jsonPath("$.message").value("Start time must use HH:mm or HH:mm:ss"));

        verifyNoInteractions(adminService);
    }
}
