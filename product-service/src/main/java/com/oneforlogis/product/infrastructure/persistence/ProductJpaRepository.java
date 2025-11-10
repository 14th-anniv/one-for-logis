package com.oneforlogis.product.infrastructure.persistence;

import com.oneforlogis.product.domain.model.Product;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductJpaRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByIdAndDeletedFalse(UUID id);
    Page<Product> findByDeletedFalse(Pageable pageable);
    Page<Product> findByNameContainingAndDeletedFalse(String name, Pageable pageable);
}
