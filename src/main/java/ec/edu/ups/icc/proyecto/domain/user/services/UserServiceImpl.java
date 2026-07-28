package ec.edu.ups.icc.proyecto.domain.user.services;

import ec.edu.ups.icc.proyecto.common.exception.BusinessRuleException;
import ec.edu.ups.icc.proyecto.common.exception.ResourceNotFoundException;
import ec.edu.ups.icc.proyecto.domain.user.dto.UserMapper;
import ec.edu.ups.icc.proyecto.domain.user.dto.UserResponse;
import ec.edu.ups.icc.proyecto.domain.user.model.Role;
import ec.edu.ups.icc.proyecto.domain.user.model.User;
import ec.edu.ups.icc.proyecto.domain.user.model.UserStatus;
import ec.edu.ups.icc.proyecto.domain.user.repository.RoleRepository;
import ec.edu.ups.icc.proyecto.domain.user.repository.UserRepository;
import ec.edu.ups.icc.proyecto.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> findAll(Pageable pageable, String search) {
        Specification<User> spec = (root, query, cb) -> {
            if (search == null || search.isBlank()) return cb.conjunction();
            String like = "%" + search.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("firstName")), like),
                    cb.like(cb.lower(root.get("lastName")), like),
                    cb.like(cb.lower(root.get("email")), like)
            );
        };
        return userRepository.findAll(spec, pageable).map(userMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userMapper.toResponse(getOrThrow(id));
    }

    @Override
    public UserResponse block(Long id) {
        if (id.equals(currentUserId())) {
            throw new BusinessRuleException("No puede bloquear su propia cuenta");
        }
        User user = getOrThrow(id);
        user.setStatus(UserStatus.BLOCKED);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse unblock(Long id) {
        User user = getOrThrow(id);
        user.setStatus(UserStatus.ACTIVE);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse assignRole(Long id, String roleName) {
        User user = getOrThrow(id);
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new BusinessRuleException("El rol '" + roleName + "' no existe"));
        user.getRoles().add(role);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse removeRole(Long id, String roleName) {
        User user = getOrThrow(id);

        if (id.equals(currentUserId()) && Role.ADMIN.equals(roleName)) {
            throw new BusinessRuleException("No puede quitarse a si mismo el rol ADMIN");
        }
        if (user.getRoles().size() <= 1) {
            throw new BusinessRuleException("El usuario debe conservar al menos un rol");
        }

        user.getRoles().removeIf(r -> r.getName().equals(roleName));
        return userMapper.toResponse(userRepository.save(user));
    }

    private User getOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario " + id + " no encontrado"));
    }

    private Long currentUserId() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal.getId();
    }
}
