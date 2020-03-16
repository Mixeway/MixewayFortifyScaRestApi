package io.mixeway.fortifyscaapi.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import io.mixeway.fortifyscaapi.db.entity.FortifyScan;

import java.util.List;
import java.util.Optional;

public interface FortifyScanRepository extends JpaRepository<FortifyScan,Long> {
    List<FortifyScan> findByGroupNameAndRunning(String groupName, Boolean running);
    Optional<FortifyScan> findByRequestId(String requestid);
}
