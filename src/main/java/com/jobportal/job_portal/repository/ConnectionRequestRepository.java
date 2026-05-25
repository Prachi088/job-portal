package com.jobportal.job_portal.repository;

import com.jobportal.job_portal.entity.ConnectionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConnectionRequestRepository extends JpaRepository<ConnectionRequest, Long> {
    List<ConnectionRequest> findByReceiverIdAndStatus(Long receiverId, String status);
    List<ConnectionRequest> findBySenderIdAndStatus(Long senderId, String status);
    Optional<ConnectionRequest> findBySenderIdAndReceiverId(Long senderId, Long receiverId);
    boolean existsBySenderIdAndReceiverId(Long senderId, Long receiverId);
}