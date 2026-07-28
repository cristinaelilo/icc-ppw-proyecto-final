package ec.edu.ups.icc.proyecto.domain.category.services;

import ec.edu.ups.icc.proyecto.domain.category.dto.CategoryRequest;
import ec.edu.ups.icc.proyecto.domain.category.dto.CategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoryService {
    Page<CategoryResponse> findAll(Pageable pageable, boolean onlyActive);
    CategoryResponse findById(Long id);
    CategoryResponse create(CategoryRequest request);
    CategoryResponse update(Long id, CategoryRequest request);
    CategoryResponse deactivate(Long id);
    CategoryResponse activate(Long id);
}
