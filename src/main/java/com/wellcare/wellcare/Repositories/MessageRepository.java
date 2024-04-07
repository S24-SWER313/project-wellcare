package com.wellcare.wellcare.Repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wellcare.wellcare.Models.Message;

import jakarta.transaction.Transactional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

        @Query("SELECT m FROM Message m WHERE (m.fromUser.id = :userId1 AND m.toUser.id = :userId2) OR (m.fromUser.id = :userId2 AND m.toUser.id = :userId1) ORDER BY m.time DESC")
        List<Message> findAllMessagesBetweenTwoUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
        
    @Transactional
    @Modifying
    @Query(value = "UPDATE Message as m " +
            "SET m.status = 1 " +
            "WHERE m.toUser.id = :toUserId AND m.fromUser.id = :fromUserId " +
            "AND m.status = 0")
    void updateStatusFromReadMessages(@Param("toUserId") Long toUserId, @Param("fromUserId") Long fromUserId);

    @Query(value = "SELECT m.* " +
               "FROM messages AS m " +
               "WHERE m.to_user_id = :userId AND m.status = 0 " +
               "ORDER BY m.time DESC", nativeQuery = true)
Page<Message> getAllUnreadMessages(@Param("userId") Long loggedInUserId, Pageable pageable);

    @Query(value = "select m.from_user_id, count(*) as count " +
            "from messages AS m " +
            "where m.status = 0 " +
            "and m.to_user_id = :userId " +
            "GROUP BY m.from_user_id " +
            "ORDER BY m.time DESC;", nativeQuery = true)
    List<Object[]> getCountOfUnreadMessagesByFromUser(@Param("userId") Long loggedInUserId);

    @Query("SELECT m FROM Message m WHERE (m.fromUser.id = :userId OR m.toUser.id = :userId) " +
            "ORDER BY m.time DESC")
    Page<Message> findRecentConversations(@Param("userId") Long userId, Pageable pageable);

}
