package com.jobportal.job_portal.repository;

import com.jobportal.job_portal.entity.Connection;
import com.jobportal.job_portal.entity.ConnectionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConnectionRepository extends JpaRepository<Connection, Long> {
    List<ConnectionRequest> findBySenderIdAndStatus(Long senderId, String status);

    List<Connection> findByUser1IdOrUser2Id(Long user1Id, Long user2Id);

    // OLD METHOD REMOVED: existsByUser1IdAndUser2Id(A, B) only checked one direction.
    // If a Connection was stored as (user1Id=B, user2Id=A), the check for (A, B)
    // returned false, so the "already connected" guard in the controller was being
    // bypassed and duplicate Connection rows could be inserted.
    //
    // This query checks BOTH orderings in one SQL call.
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Connection c " +
            "WHERE (c.user1Id = :a AND c.user2Id = :b) " +
            "   OR (c.user1Id = :b AND c.user2Id = :a)")
    boolean existsBetweenUsers(@Param("a") Long a, @Param("b") Long b);
}