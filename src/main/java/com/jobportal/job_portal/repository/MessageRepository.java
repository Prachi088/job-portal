package com.jobportal.job_portal.repository;

import com.jobportal.job_portal.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByCreatedAtAsc(
        Long s1, Long r1, Long s2, Long r2);
    List<Message> findByReceiverIdAndIsReadFalse(Long receiverId);
}