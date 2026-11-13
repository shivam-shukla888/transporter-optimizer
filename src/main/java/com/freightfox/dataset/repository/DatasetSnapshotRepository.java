package com.freightfox.dataset.repository;

import com.freightfox.dataset.entity.DatasetSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DatasetSnapshotRepository extends JpaRepository<DatasetSnapshotEntity, Long> {

    Optional<DatasetSnapshotEntity> findByActiveTrue();

    @Modifying
    @Query("UPDATE DatasetSnapshotEntity d SET d.active = false WHERE d.active = true")
    void deactivateAll();
}
