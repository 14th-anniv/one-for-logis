package com.oneforlogis.hub.infrastructure.persistence;

import com.oneforlogis.hub.domain.model.Hub;
import com.oneforlogis.hub.domain.repository.HubRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class HubRepositoryImpl implements HubRepository {

    private final HubJpaRepository jpaRepository;

    @Override
    public void save(Hub hub) {
        jpaRepository.save(hub);
    }

    @Override
    public void flush() {
        jpaRepository.flush();
    }

    @Override
    public Optional<Hub> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Hub> findByIdAndDeletedFalse(UUID id) {
        return jpaRepository.findByIdAndDeletedFalse(id);
    }

    @Override
    public Optional<Hub> findByNameAndDeletedFalse(String hubName) {
        return jpaRepository.findByNameAndDeletedFalse(hubName);
    }

    @Override
    public List<Hub> findByDeletedFalse() {
        return jpaRepository.findByDeletedFalse();
    }

    @Override
    public Page<Hub> findByDeletedFalse(Pageable pageable) {
        return jpaRepository.findByDeletedFalse(pageable);
    }
}