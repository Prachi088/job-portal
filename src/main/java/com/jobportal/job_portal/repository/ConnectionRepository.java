package com.jobportal.job_portal.repository;

import com.jobportal.job_portal.entity.Connection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConnectionRepository extends JpaRepository<Connection, Long> {
    List<Connection> findByUser1IdOrUser2Id(Long user1Id, Long user2Id);
    boolean existsByUser1IdAndUser2Id(Long user1Id, Long user2Id);
}