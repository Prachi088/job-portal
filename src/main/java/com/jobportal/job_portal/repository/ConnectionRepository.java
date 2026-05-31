package com.jobportal.job_portal.repository;

import com.jobportal.job_portal.entity.Connection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ConnectionRepository extends JpaRepository<Connection, Long> {

    List<Connection> findByUser1IdOrUser2Id(Long user1Id, Long user2Id);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Connection c " +
            "WHERE (c.user1Id = :a AND c.user2Id = :b) " +
            "   OR (c.user1Id = :b AND c.user2Id = :a)")
    boolean existsBetweenUsers(@Param("a") Long a, @Param("b") Long b);

    // Uses user1Id/user2Id to match the actual Connection entity fields
    @Modifying
    @Transactional
    @Query("DELETE FROM Connection c WHERE (c.user1Id = :userId AND c.user2Id = :otherId) " +
            "OR (c.user1Id = :otherId AND c.user2Id = :userId)")
    void deleteBetweenUsers(@Param("userId") Long userId, @Param("otherId") Long otherId);
}