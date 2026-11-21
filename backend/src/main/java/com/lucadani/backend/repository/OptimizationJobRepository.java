package com.lucadani.backend.repository;

import com.lucadani.backend.model.OptimizationJob;
import com.lucadani.backend.model.OptimizationJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OptimizationJobRepository extends JpaRepository<OptimizationJob, UUID> {

    /**
     * Raises the progress of a running job; never lowers it, as updates can arrive out of order.
     * A bulk update, so frequent progress reports don't load the entity or bump its version.
     *
     * @return 1 if the progress was raised, 0 otherwise
     */
    @Transactional
    @Modifying
    @Query("""
            update OptimizationJob j set j.progress = :progress
            where j.id = :id and j.status = com.lucadani.backend.model.OptimizationJobStatus.RUNNING
              and j.progress < :progress""")
    int raiseProgress(UUID id, int progress);

    /** Jobs in one of the given states, created before {@code before}. */
    List<OptimizationJob> findAllByStatusInAndCreatedAtBefore(Collection<OptimizationJobStatus> statuses,
                                                              Instant before);

    @Transactional
    @Modifying
    @Query("delete from OptimizationJob j where j.createdAt < :before")
    int deleteCreatedBefore(Instant before);
}
