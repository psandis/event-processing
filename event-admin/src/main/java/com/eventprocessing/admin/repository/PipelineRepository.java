package com.eventprocessing.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PipelineRepository extends JpaRepository<PipelineEntity, Long> {

    Optional<PipelineEntity> findByName(String name);

    Optional<PipelineEntity> findByNameAndVersion(String name, int version);

    List<PipelineEntity> findByNameOrderByVersionDesc(String name);

    Optional<PipelineEntity> findByNameAndState(String name, String state);

    boolean existsByName(String name);

    void deleteByName(String name);

    void deleteByNameAndVersion(String name, int version);
}
