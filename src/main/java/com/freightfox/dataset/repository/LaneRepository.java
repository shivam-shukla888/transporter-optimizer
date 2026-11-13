package com.freightfox.dataset.repository;

import com.freightfox.dataset.entity.LaneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LaneRepository extends JpaRepository<LaneEntity, Long> {

    List<LaneEntity> findBySnapshotIdOrderByLaneIdAsc(Long snapshotId);
}
