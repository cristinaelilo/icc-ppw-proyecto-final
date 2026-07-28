package ec.edu.ups.icc.proyecto.domain.user.services;

import ec.edu.ups.icc.proyecto.domain.user.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    Page<UserResponse> findAll(Pageable pageable, String search);
    UserResponse findById(Long id);
    UserResponse block(Long id);
    UserResponse unblock(Long id);
    UserResponse assignRole(Long id, String roleName);
    UserResponse removeRole(Long id, String roleName);
}
