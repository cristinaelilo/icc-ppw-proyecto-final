package ec.edu.ups.icc.proyecto.domain.auth.services;

import ec.edu.ups.icc.proyecto.domain.auth.dto.LoginRequest;
import ec.edu.ups.icc.proyecto.domain.auth.dto.RefreshRequest;
import ec.edu.ups.icc.proyecto.domain.auth.dto.RegisterRequest;
import ec.edu.ups.icc.proyecto.domain.auth.dto.TokenResponse;
import ec.edu.ups.icc.proyecto.domain.user.dto.UserResponse;

public interface AuthService {
    UserResponse register(RegisterRequest request, String clientIp);
    TokenResponse login(LoginRequest request, String clientIp);
    TokenResponse refresh(RefreshRequest request, String clientIp);
    void logout(RefreshRequest request);
    UserResponse me(Long userId);
}
