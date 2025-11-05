package com.oneforlogis.hub.domain.repository;

import com.oneforlogis.hub.domain.model.Hub;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HubRepository {
    void save(Hub hub);
    void flush();
    Optional<Hub> findById(UUID id);
    Optional<Hub> findByIdAndDeletedFalse(UUID id);
    Optional<Hub> findByNameAndDeletedFalse(String hubName);
    List<Hub> findByDeletedFalse();
    Page<Hub> findByDeletedFalse(Pageable pageable);
}