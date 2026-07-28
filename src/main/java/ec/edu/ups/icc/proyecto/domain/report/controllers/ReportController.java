package ec.edu.ups.icc.proyecto.domain.report.controllers;

import ec.edu.ups.icc.proyecto.common.exception.TooManyRequestsException;
import ec.edu.ups.icc.proyecto.common.ratelimit.RateLimitProperties;
import ec.edu.ups.icc.proyecto.common.ratelimit.RateLimitService;
import ec.edu.ups.icc.proyecto.domain.report.services.ReportService;
import ec.edu.ups.icc.proyecto.security.UserPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final RateLimitService rateLimitService;
    private final RateLimitProperties rateLimitProperties;

    public ReportController(ReportService reportService, RateLimitService rateLimitService,
                             RateLimitProperties rateLimitProperties) {
        this.reportService = reportService;
        this.rateLimitService = rateLimitService;
        this.rateLimitProperties = rateLimitProperties;
    }

    @GetMapping("/events/{eventId}/registrations.pdf")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ResponseEntity<byte[]> eventRegistrationsPdf(@PathVariable Long eventId,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        enforceReportRateLimit(principal.getId());
        byte[] pdf = reportService.eventRegistrationsPdf(eventId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"registrations-event-" + eventId + ".pdf\"")
                .body(pdf);
    }

    @GetMapping("/events/{eventId}/registrations.xlsx")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ResponseEntity<byte[]> eventRegistrationsExcel(@PathVariable Long eventId,
                                                           @AuthenticationPrincipal UserPrincipal principal) {
        enforceReportRateLimit(principal.getId());
        byte[] xlsx = reportService.eventRegistrationsExcel(eventId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"registrations-event-" + eventId + ".xlsx\"")
                .body(xlsx);
    }

    @GetMapping("/registrations/{id}/certificate.pdf")
    @PreAuthorize("hasRole('PARTICIPANT')")
    public ResponseEntity<byte[]> registrationCertificate(@PathVariable Long id,
                                                           @AuthenticationPrincipal UserPrincipal principal) {
        enforceReportRateLimit(principal.getId());
        byte[] pdf = reportService.registrationCertificatePdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"certificate-registration-" + id + ".pdf\"")
                .body(pdf);
    }

    private void enforceReportRateLimit(Long userId) {
        var rule = rateLimitProperties.getReports();
        var result = rateLimitService.increment("rate:reports:" + userId, rule.getLimit(), rule.getWindowSeconds());
        if (!result.allowed()) {
            throw new TooManyRequestsException(
                    "Ha excedido el limite de generacion de reportes. Intente nuevamente mas tarde.",
                    result.retryAfterSeconds());
        }
    }
}
