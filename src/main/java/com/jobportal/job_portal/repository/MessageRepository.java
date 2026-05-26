package com.jobportal.job_portal.repository;

import com.jobportal.job_portal.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByCreatedAtAsc(
            Long s1, Long r1, Long s2, Long r2);

    List<Message> findByReceiverIdAndIsReadFalse(Long receiverId);

    // FIX: added to support efficient markAsRead filtering at the database level.
    // The old MessageController fetched all unread messages for a receiver and
    // then filtered by senderId in Java — this query pushes that filter into SQL,
    // avoiding loading unrelated messages into memory.
    List<Message> findBySenderIdAndReceiverIdAndIsReadFalse(Long senderId, Long receiverId);
}