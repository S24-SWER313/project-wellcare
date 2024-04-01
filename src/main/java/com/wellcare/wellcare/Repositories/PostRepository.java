package com.wellcare.wellcare.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wellcare.wellcare.Models.ERole;
import com.wellcare.wellcare.Models.Post;
import com.wellcare.wellcare.Models.User;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("select p from Post p where p.user.id = :userId")
    public Page<Post> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT p FROM Post p JOIN p.user u JOIN u.role r WHERE r.name = :role ORDER BY p.createdAt DESC")
    Optional<List<Post>> findAllPostsByRole(@Param("role") ERole role);

    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.likes LEFT JOIN FETCH p.comments")
    Page<Post> findAllWithLikesAndComments(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user IN :users ORDER BY p.createdAt DESC")
    List<Post> findAllByUserInOrderByCreatedAtDesc(@Param("users") List<User> users);

    @Query("SELECT p FROM Post p WHERE p.user.id IN :userIds")
    Page<Post> findAllPostsByUserIds(List<Long> userIds, Pageable pageable);

}
