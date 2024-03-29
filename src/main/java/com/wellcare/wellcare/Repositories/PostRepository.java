package com.wellcare.wellcare.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wellcare.wellcare.Models.ERole;
import com.wellcare.wellcare.Models.Post;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("select distinct p from Post p left join fetch p.attachment where p.user.id = :userId")
    public List<Post> findByUserIdWithAttachment(@Param("userId") Long userId);

    @Query("SELECT distinct p FROM Post p JOIN p.user u JOIN u.role r LEFT JOIN FETCH p.attachment WHERE r.name = :role ORDER BY p.createdAt DESC")
    Optional<List<Post>> findAllPostsByRoleWithAttachment(@Param("role") ERole role);

    @Query("SELECT distinct p FROM Post p LEFT JOIN FETCH p.attachment JOIN FETCH p.likes LEFT JOIN FETCH p.comments WHERE p.id = ?1")
    Optional<Post> findByIdWithLikesAndCommentsAndAttachment(Long postId);

    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.attachment LEFT JOIN FETCH p.likes LEFT JOIN FETCH p.comments")
    List<Post> findAllWithLikesAndCommentsAndAttachment();

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.attachment WHERE p.user.id IN :userIds")
    Optional<List<Post>> findAllPostsByUserIdsWithAttachment(@Param("userIds") List<Long> userIds);
}
