package ec.edu.ups.icc.proyecto.domain.category.services;

import ec.edu.ups.icc.proyecto.common.exception.DuplicateResourceException;
import ec.edu.ups.icc.proyecto.common.exception.ResourceNotFoundException;
import ec.edu.ups.icc.proyecto.domain.category.dto.CategoryMapper;
import ec.edu.ups.icc.proyecto.domain.category.dto.CategoryRequest;
import ec.edu.ups.icc.proyecto.domain.category.dto.CategoryResponse;
import ec.edu.ups.icc.proyecto.domain.category.model.Category;
import ec.edu.ups.icc.proyecto.domain.category.repository.CategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryResponse> findAll(Pageable pageable, boolean onlyActive) {
        Page<Category> page = onlyActive
                ? categoryRepository.findByActiveTrue(pageable)
                : categoryRepository.findAll(pageable);
        return page.map(categoryMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        return categoryMapper.toResponse(getOrThrow(id));
    }

    @Override
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("Ya existe una categoria con el nombre '" + request.name() + "'");
        }
        Category saved = categoryRepository.save(categoryMapper.toEntity(request));
        return categoryMapper.toResponse(saved);
    }

    @Override
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getOrThrow(id);
        if (!category.getName().equalsIgnoreCase(request.name())
                && categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("Ya existe una categoria con el nombre '" + request.name() + "'");
        }
        category.setName(request.name());
        category.setDescription(request.description());
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    public CategoryResponse deactivate(Long id) {
        Category category = getOrThrow(id);
        category.setActive(false);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    public CategoryResponse activate(Long id) {
        Category category = getOrThrow(id);
        category.setActive(true);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    private Category getOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria " + id + " no encontrada"));
    }
}
