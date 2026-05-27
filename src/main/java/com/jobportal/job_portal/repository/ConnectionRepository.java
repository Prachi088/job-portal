package com.jobportal.job_portal.repository;

import com.jobportal.job_portal.entity.Connection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConnectionRepository extends JpaRepository<Connection, Long> {

    // FIX: removed the erroneous findBySenderIdAndStatus method that had
    // return type List<ConnectionRequest> — Connection has no senderId/status
    // fields so this method can never work and would throw a ClassCastException
    // at runtime. It was likely copied from ConnectionRequestRepository by
    // mistake and is not called anywhere in ConnectionController.

    List<Connection> findByUser1IdOrUser2Id(Long user1Id, Long user2Id);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Connection c " +
            "WHERE (c.user1Id = :a AND c.user2Id = :b) " +
            "   OR (c.user1Id = :b AND c.user2Id = :a)")
    boolean existsBetweenUsers(@Param("a") Long a, @Param("b") Long b);
}