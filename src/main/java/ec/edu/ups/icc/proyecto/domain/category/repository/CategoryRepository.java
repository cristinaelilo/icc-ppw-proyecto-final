package ec.edu.ups.icc.proyecto.domain.category.repository;

import ec.edu.ups.icc.proyecto.domain.category.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    @org.springframework.data.jpa.repository.Query(
            "select c from Category c where lower(c.name) = lower(:name)")
    Optional<Category> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    Page<Category> findByActiveTrue(Pageable pageable);
}
