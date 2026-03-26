package com.corefi.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String refreshToken;
    private String nom;
    private String prenom;
    private String email;
    private String role;
    private String photoUrl;
}
