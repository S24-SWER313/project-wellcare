package com.wellcare.wellcare.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

import com.wellcare.wellcare.Models.ERole;
import com.wellcare.wellcare.Models.Post;

public interface PostRepository extends JpaRepository<Post, Long>{

    @Query("select p from Post p where p.user.id= ?1")
    public List<Post> findByUserId(Long userId);

    @Query("select p from Post p WHERE p.user.id IN : users ORDER BY p.createdAt DESC")
    public List<Post> findAllPostByUserIds(@Param("users") List<Long> userIds);

    @Query("SELECT p FROM Post p JOIN p.User u JOIN u.Role r WHERE r.name = :role AND p.id = :postId")
    public Optional<Post> findPostByRole(@Param("postId") Long postId, @Param("role") ERole role);

    @Query("SELECT p FROM Post p JOIN p.User u JOIN u.Role r WHERE r.name = 'PATIENT' AND u.id IN :userIds ORDER BY p.createdAt DESC")
    public List<Post> findAllPostsForPatients(@Param("userIds") List<Long> userIds);
    
    @Query("SELECT p FROM Post p JOIN p.User u JOIN u.Role r WHERE r.name = 'DOCTOR' AND u.id IN :userIds ORDER BY p.createdAt DESC")
    List<Post> findAllPostsForDoctors(@Param("userIds") List<Long> userIds);
}
