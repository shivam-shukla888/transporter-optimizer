package com.freightfox.dataset.repository;

import com.freightfox.dataset.entity.TransporterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransporterRepository extends JpaRepository<TransporterEntity, Long> {

    List<TransporterEntity> findBySnapshotIdOrderByTransporterIdAsc(Long snapshotId);
}
