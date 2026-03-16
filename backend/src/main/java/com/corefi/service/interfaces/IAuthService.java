package com.corefi.service.interfaces;

import com.corefi.dto.request.auth.LoginRequest;
import com.corefi.dto.response.auth.AuthResponse;

public interface IAuthService {
    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(String refreshToken);
}
