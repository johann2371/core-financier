package com.corefi.service.impl;

import com.corefi.dto.request.auth.LoginRequest;
import com.corefi.dto.response.auth.AuthResponse;
import com.corefi.service.interfaces.IAuthService;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements IAuthService {

    @Override
    public AuthResponse login(LoginRequest request) {
        // TODO: Implémenter la logique d'authentification
        return null;
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        // TODO: Implémenter le renouvellement de token
        return null;
    }
}
