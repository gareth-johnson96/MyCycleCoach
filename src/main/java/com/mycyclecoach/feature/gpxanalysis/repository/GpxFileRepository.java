package com.mycyclecoach.feature.gpxanalysis.repository;

import com.mycyclecoach.feature.gpxanalysis.domain.GpxFile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GpxFileRepository extends JpaRepository<GpxFile, Long> {

    List<GpxFile> findByUserId(Long userId);
}
