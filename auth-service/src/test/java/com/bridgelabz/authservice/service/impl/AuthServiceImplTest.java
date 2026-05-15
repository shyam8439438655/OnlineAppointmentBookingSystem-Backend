package com.bridgelabz.authservice.service.impl;

import com.bridgelabz.authservice.dto.*;
import com.bridgelabz.authservice.model.User;
import com.bridgelabz.authservice.repository.UserRepository;
import com.bridgelabz.authservice.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .email("test@example.com")
                .password("password123")
                .fullName("Test User")
                .role("PATIENT")
                .build();

        loginRequest = new LoginRequest("test@example.com", "password123");

        user = User.builder()
                .userId("user123")
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .fullName("Test User")
                .role("PATIENT")
                .isActive(true)
                .build();
    }

    @Test
    void register_Success() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(jwtUtil.generateToken(any(), any(), any(), any())).thenReturn("mockToken");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response.getToken());
        assertEquals("User registered successfully", response.getMessage());
        verify(userRepository, times(1)).save(any());
    }

    @Test
    void register_EmailExists() {
        when(userRepository.existsByEmail(any())).thenReturn(true);

        AuthResponse response = authService.register(registerRequest);

        assertNull(response.getToken());
        assertEquals("Email already exists", response.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_Success() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(jwtUtil.generateToken(any(), any(), any(), any())).thenReturn("mockToken");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response.getToken());
        assertEquals("Login successful", response.getMessage());
    }

    @Test
    void login_InvalidPassword() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        AuthResponse response = authService.login(loginRequest);

        assertNull(response.getToken());
        assertEquals("Invalid password", response.getMessage());
    }

    @Test
    void getUserById_Success() {
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));

        UserResponse response = authService.getUserById("user123");

        assertEquals("user123", response.getUserId());
        assertEquals("test@example.com", response.getEmail());
    }
}
