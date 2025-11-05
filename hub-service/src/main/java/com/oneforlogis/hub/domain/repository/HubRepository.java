package com.oneforlogis.hub.domain.repository;

import com.oneforlogis.hub.domain.model.Hub;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HubRepository {
    void save(Hub hub);
    void flush();
    Optional<Hub> findById(UUID id);
    List<Hub> findByDeletedFalse();
}