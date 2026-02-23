package com.mycyclecoach.feature.gpxanalysis.repository;

import com.mycyclecoach.feature.gpxanalysis.domain.Climb;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClimbRepository extends JpaRepository<Climb, Long> {

    List<Climb> findByGpxFileId(Long gpxFileId);
}
