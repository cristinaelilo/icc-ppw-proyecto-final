package ec.edu.ups.icc.proyecto.domain.registration.controllers;

import ec.edu.ups.icc.proyecto.domain.registration.dto.RegistrationResponse;
import ec.edu.ups.icc.proyecto.domain.registration.model.RegistrationStatus;
import ec.edu.ups.icc.proyecto.domain.registration.services.RegistrationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/events/{eventId}")
    @PreAuthorize("hasRole('PARTICIPANT')")
    public ResponseEntity<RegistrationResponse> register(@PathVariable Long eventId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationService.register(eventId));
    }

    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public RegistrationResponse confirm(@PathVariable Long id) {
        return registrationService.confirm(id);
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public RegistrationResponse reject(@PathVariable Long id) {
        return registrationService.reject(id);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PARTICIPANT','ADMIN')")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        registrationService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PARTICIPANT')")
    public Page<RegistrationResponse> findMine(Pageable pageable) {
        return registrationService.findMine(pageable);
    }

    @GetMapping("/events/{eventId}")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public Page<RegistrationResponse> findByEvent(@PathVariable Long eventId,
                                                   @RequestParam(required = false) RegistrationStatus status,
                                                   Pageable pageable) {
        return registrationService.findByEvent(eventId, status, pageable);
    }
}
