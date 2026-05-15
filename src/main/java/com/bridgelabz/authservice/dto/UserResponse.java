package com.bridgelabz.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String userId;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String provider;
    private boolean isActive;
    private String profilePicUrl;
    private java.time.LocalDateTime createdAt;
}