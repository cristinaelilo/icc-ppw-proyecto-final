package ec.edu.ups.icc.proyecto.domain.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    // uq_categories_name_lower es un indice unico sobre LOWER(name) en BD;
    // esta consulta lo respeta usando LOWER() tambien del lado de la app.
    @org.springframework.data.jpa.repository.Query(
            "select c from Category c where lower(c.name) = lower(:name)")
    Optional<Category> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    Page<Category> findByActiveTrue(Pageable pageable);
}
