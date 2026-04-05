package com.bookapp.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String username;
    private String email;
    private String fullName;
    private String avatar;
    private String role;
    private String plan;
}
