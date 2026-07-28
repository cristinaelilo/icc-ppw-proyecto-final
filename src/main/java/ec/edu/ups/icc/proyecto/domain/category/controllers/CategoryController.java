package ec.edu.ups.icc.proyecto.domain.category.controllers;

import ec.edu.ups.icc.proyecto.domain.category.dto.CategoryRequest;
import ec.edu.ups.icc.proyecto.domain.category.dto.CategoryResponse;
import ec.edu.ups.icc.proyecto.domain.category.services.CategoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Lectura publica. Un usuario sin rol ADMIN solo ve categorias activas,
     * sin importar lo que envie en includeInactive (evita exponer categorias
     * desactivadas a quienes no administran el catalogo).
     */
    @GetMapping
    public Page<CategoryResponse> findAll(
            @RequestParam(defaultValue = "false") boolean includeInactive,
            Pageable pageable) {
        boolean onlyActive = !includeInactive || !isAdmin();
        return categoryService.findAll(pageable, onlyActive);
    }

    @GetMapping("/{id}")
    public CategoryResponse findById(@PathVariable Long id) {
        return categoryService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse deactivate(@PathVariable Long id) {
        return categoryService.deactivate(id);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse activate(@PathVariable Long id) {
        return categoryService.activate(id);
    }

    private boolean isAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
