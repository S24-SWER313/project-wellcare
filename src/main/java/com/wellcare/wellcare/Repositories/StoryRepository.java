package com.wellcare.wellcare.Repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wellcare.wellcare.Models.Story;
import com.wellcare.wellcare.Models.User;

public interface StoryRepository extends JpaRepository<Story, Long> {

    @Query("SELECT s FROM Story s WHERE s.user.id = :userId")
    List<Story> findStoryByUserId(@Param("userId") Long userId);

    List<Story> findTop20ByUserOrderByCreatedAtDesc(User user);

    void deleteByUserAndCreatedAtBefore(User user, LocalDateTime createdAt);

    List<Story> findAllByExpiresAtAfter(LocalDateTime expiresAt);

    List<Story> findAllByExpiresAtBefore(LocalDateTime expiresAt);
}
