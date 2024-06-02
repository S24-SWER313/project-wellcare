package com.wellcare.wellcare.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.wellcare.wellcare.Models.ERole;
import com.wellcare.wellcare.Models.Post;
import com.wellcare.wellcare.Models.User;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("select p from Post p where p.user.id = :userId order by p.createdAt desc")
    public Page<Post> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT p FROM Post p JOIN User u WHERE u.role = :role ORDER BY p.createdAt DESC")
    Optional<List<Post>> findAllPostsByRole(@Param("role") ERole role);

    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.likes LEFT JOIN FETCH p.comments order by p.createdAt desc")
    Page<Post> findAllWithLikesAndComments(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user IN :users ORDER BY p.createdAt DESC")
    List<Post> findAllByUserInOrderByCreatedAtDesc(@Param("users") List<User> users);

    @Query("SELECT p FROM Post p WHERE p.user.id IN :userIds order by p.createdAt desc")
    Page<Post> findAllPostsByUserIds(List<Long> userIds, Pageable pageable);

    @Query("SELECT p FROM Post p JOIN p.comments c WHERE c.id = :commentId")
    Optional<Post> findPostByCommentId(@Param("commentId") Long commentId);

    @Transactional
    @Modifying
    @Query("DELETE FROM Post p WHERE p.id = :postId")
    void deletePostById(@Param("postId") Long postId);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.user.id = :userId")
    Long countPostsByUserId(@Param("userId") Long userId);

    Page<Post> findBySavedBy(User user, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Post p WHERE p.content LIKE %:query%")
    public List<Post> findByContent(@Param("query") String query);


}
