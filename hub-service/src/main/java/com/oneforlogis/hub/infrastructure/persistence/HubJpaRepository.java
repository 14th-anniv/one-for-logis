package com.oneforlogis.hub.infrastructure.persistence;

import com.oneforlogis.hub.domain.model.Hub;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HubJpaRepository extends JpaRepository<Hub, UUID> {
    Optional<Hub> findByIdAndDeletedFalse(UUID id);
    Optional<Hub> findByNameAndDeletedFalse(String hubName);
    List<Hub> findByDeletedFalse();
}