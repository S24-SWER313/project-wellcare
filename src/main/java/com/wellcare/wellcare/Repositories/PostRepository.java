package com.wellcare.wellcare.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

import com.wellcare.wellcare.Models.ERole;
import com.wellcare.wellcare.Models.Post;

public interface PostRepository extends JpaRepository<Post, Long>{

    @Query("select p from Post p where p.author.id = ?1")
    public List<Post> findByUserId(Long userId);
    
    @Query("select p from Post p WHERE p.author.id IN : user ORDER BY p.createdAt DESC")
    public List<Post> findAllPostByUserIds(@Param("user") List<Long> userIds);

    @Query("SELECT p FROM Post p JOIN p.author u JOIN u.roles r WHERE r.name = :role AND p.id = :postId")
    public Optional<Post> findAllPostsByRole(@Param("postId") Long postId, @Param("role") ERole role);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.likes LEFT JOIN FETCH p.comments WHERE p.id = ?1")
    Optional<Post> findByIdWithLikesAndComments(Long postId);
    
}
