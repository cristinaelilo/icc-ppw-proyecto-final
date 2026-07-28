package ec.edu.ups.icc.proyecto.domain.report.services;

public interface ReportService {
    byte[] eventRegistrationsPdf(Long eventId);
    byte[] eventRegistrationsExcel(Long eventId);
    byte[] registrationCertificatePdf(Long registrationId);
}
