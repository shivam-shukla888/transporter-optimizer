package com.freightfox.dataset.repository;

import com.freightfox.dataset.entity.LaneQuoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LaneQuoteRepository extends JpaRepository<LaneQuoteEntity, Long> {

    List<LaneQuoteEntity> findBySnapshotId(Long snapshotId);

    List<LaneQuoteEntity> findBySnapshotIdOrderByLaneIdAscTransporterIdAsc(Long snapshotId);
}
