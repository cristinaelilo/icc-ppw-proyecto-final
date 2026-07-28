package ec.edu.ups.icc.proyecto.domain.user.controllers;

import ec.edu.ups.icc.proyecto.domain.user.dto.AssignRoleRequest;
import ec.edu.ups.icc.proyecto.domain.user.dto.UserResponse;
import ec.edu.ups.icc.proyecto.domain.user.services.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Page<UserResponse> findAll(@RequestParam(required = false) String search, Pageable pageable) {
        return userService.findAll(pageable, search);
    }

    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @PatchMapping("/{id}/block")
    public UserResponse block(@PathVariable Long id) {
        return userService.block(id);
    }

    @PatchMapping("/{id}/unblock")
    public UserResponse unblock(@PathVariable Long id) {
        return userService.unblock(id);
    }

    @PostMapping("/{id}/roles")
    public UserResponse assignRole(@PathVariable Long id, @Valid @RequestBody AssignRoleRequest request) {
        return userService.assignRole(id, request.role());
    }

    @DeleteMapping("/{id}/roles/{role}")
    public UserResponse removeRole(@PathVariable Long id, @PathVariable String role) {
        return userService.removeRole(id, role);
    }
}
