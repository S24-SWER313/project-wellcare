package com.wellcare.wellcare.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wellcare.wellcare.Models.ERole;
import com.wellcare.wellcare.Models.Post;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("select p from Post p where p.user.id = :userId")
    public List<Post> findByUserId(@Param("userId") Long userId);

    @Query("SELECT p FROM Post p JOIN p.user u JOIN u.role r WHERE r.name = :role ORDER BY p.createdAt DESC")
    Optional<List<Post>> findAllPostsByRole(@Param("role") ERole role);

    @Query("SELECT p FROM Post p JOIN FETCH p.likes LEFT JOIN FETCH p.comments WHERE p.id = ?1")
    Optional<Post> findByIdWithLikesAndComments(Long postId);

}
