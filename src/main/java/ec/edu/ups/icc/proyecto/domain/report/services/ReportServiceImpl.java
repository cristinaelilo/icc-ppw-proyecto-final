package ec.edu.ups.icc.proyecto.domain.report.services;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import ec.edu.ups.icc.proyecto.common.exception.BusinessRuleException;
import ec.edu.ups.icc.proyecto.common.exception.ForbiddenOperationException;
import ec.edu.ups.icc.proyecto.common.exception.ResourceNotFoundException;
import ec.edu.ups.icc.proyecto.common.util.DateTimeUtil;
import ec.edu.ups.icc.proyecto.domain.event.model.Event;
import ec.edu.ups.icc.proyecto.domain.event.repository.EventRepository;
import ec.edu.ups.icc.proyecto.domain.registration.model.Registration;
import ec.edu.ups.icc.proyecto.domain.registration.model.RegistrationStatus;
import ec.edu.ups.icc.proyecto.domain.registration.repository.RegistrationRepository;
import ec.edu.ups.icc.proyecto.security.UserPrincipal;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private static final String DATE_PATTERN = "dd/MM/yyyy HH:mm";

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;

    public ReportServiceImpl(EventRepository eventRepository, RegistrationRepository registrationRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
    }

    @Override
    public byte[] eventRegistrationsPdf(Long eventId) {
        Event event = getEventAndCheckOwnership(eventId);
        List<Registration> registrations = registrationRepository
                .findByEvent_Id(eventId, Pageable.unpaged()).getContent();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font normalFont = new Font(Font.HELVETICA, 10);
            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD);

            document.add(new Paragraph("Listado de inscritos - " + event.getTitle(), titleFont));
            document.add(new Paragraph("Generado: " + DateTimeUtil.formatBusiness(DateTimeUtil.nowUtc(), DATE_PATTERN)
                    + " (America/Guayaquil)", normalFont));
            document.add(new Paragraph("Cupo: " + event.getCapacity() + " | Disponibles: " + event.getAvailableCapacity(),
                    normalFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.addCell(new PdfPCell(new Phrase("Participante", headerFont)));
            table.addCell(new PdfPCell(new Phrase("Estado", headerFont)));
            table.addCell(new PdfPCell(new Phrase("Fecha de solicitud", headerFont)));

            for (Registration r : registrations) {
                table.addCell(new PdfPCell(new Phrase(r.getParticipant().getFullName(), normalFont)));
                table.addCell(new PdfPCell(new Phrase(r.getStatus().name(), normalFont)));
                table.addCell(new PdfPCell(new Phrase(
                        DateTimeUtil.formatBusiness(r.getRegisteredAt(), DATE_PATTERN), normalFont)));
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessRuleException("No se pudo generar el reporte PDF: " + e.getMessage());
        }
    }

    @Override
    public byte[] eventRegistrationsExcel(Long eventId) {
        Event event = getEventAndCheckOwnership(eventId);
        List<Registration> registrations = registrationRepository
                .findByEvent_Id(eventId, Pageable.unpaged()).getContent();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Inscritos");
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);
            String[] columns = {"Participante", "Correo", "Estado", "Fecha de solicitud", "Fecha de confirmacion", "Fecha de cancelacion"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Registration r : registrations) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.getParticipant().getFullName());
                row.createCell(1).setCellValue(r.getParticipant().getEmail());
                row.createCell(2).setCellValue(r.getStatus().name());
                row.createCell(3).setCellValue(DateTimeUtil.formatBusiness(r.getRegisteredAt(), DATE_PATTERN));
                row.createCell(4).setCellValue(DateTimeUtil.formatBusiness(r.getConfirmedAt(), DATE_PATTERN));
                row.createCell(5).setCellValue(DateTimeUtil.formatBusiness(r.getCancelledAt(), DATE_PATTERN));
            }
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessRuleException("No se pudo generar el reporte Excel: " + e.getMessage());
        }
    }

    @Override
    public byte[] registrationCertificatePdf(Long registrationId) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripcion " + registrationId + " no encontrada"));

        Long currentUserId = currentUserId();
        if (!registration.getParticipant().getId().equals(currentUserId) && !isAdmin()) {
            throw new ForbiddenOperationException("Solo el participante propietario puede descargar este comprobante");
        }
        if (registration.getStatus() != RegistrationStatus.CONFIRMED) {
            throw new BusinessRuleException("Solo se puede emitir comprobante de una inscripcion CONFIRMED");
        }

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font normalFont = new Font(Font.HELVETICA, 12);

            document.add(new Paragraph("Comprobante de Inscripcion", titleFont));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Codigo: " + registration.getRegistrationCode(), normalFont));
            document.add(new Paragraph("Participante: " + registration.getParticipant().getFullName(), normalFont));
            document.add(new Paragraph("Evento: " + registration.getEvent().getTitle(), normalFont));
            document.add(new Paragraph("Estado: " + registration.getStatus(), normalFont));
            document.add(new Paragraph("Fecha de confirmacion: "
                    + DateTimeUtil.formatBusiness(registration.getConfirmedAt(), DATE_PATTERN)
                    + " (America/Guayaquil)", normalFont));
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessRuleException("No se pudo generar el comprobante: " + e.getMessage());
        }
    }

    // ---------------- Helpers ----------------

    private Event getEventAndCheckOwnership(Long eventId) {
        Event event = eventRepository.findByIdAndDeletedFalse(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento " + eventId + " no encontrado"));
        if (!isAdmin() && !event.getOrganizer().getId().equals(currentUserId())) {
            throw new ForbiddenOperationException("Solo el organizador propietario o un ADMIN pueden generar este reporte");
        }
        return event;
    }

    private Long currentUserId() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal.getId();
    }

    private boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
