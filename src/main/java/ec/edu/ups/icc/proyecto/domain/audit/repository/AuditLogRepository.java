package ec.edu.ups.icc.proyecto.domain.audit.repository;

import ec.edu.ups.icc.proyecto.domain.audit.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
