package com.wellcare.wellcare.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wellcare.wellcare.Models.ERole;
import com.wellcare.wellcare.Models.Post;
import com.wellcare.wellcare.Models.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    public Optional<User> findByEmail(String email);

    public Optional<User> findByUsername(String username);

    public Optional<User> findById(Long id);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    List<User> findByRole(ERole role);

    @Query("SELECT u FROM User u WHERE u.id IN :user")
    public List<User> findByUserId(@Param("user") List<Long> userId);

    @Query("SELECT DISTINCT u FROM User u WHERE u.username LIKE %:query% OR u.email LIKE %:query%")
    public List<User> findBySearch(@Param("query") String query);

    @Query("SELECT u FROM User u WHERE u.role = :role")
    List<User> findAllUsersByRole(@Param("role") ERole role);

    @Query("SELECT CASE WHEN COUNT(up.id) > 0 THEN true ELSE false END FROM User u JOIN u.savedPost up WHERE u.id = :userId AND up.id = :postId")
    boolean existsUserSavedPostByUserIdAndPostId(Long userId, Long postId);

    List<User> findBySavedPost(Post post);

    @Query("SELECT DISTINCT u FROM User u WHERE u.username LIKE %:query% OR u.email LIKE %:query%")
    public List<User> findByQuery(@Param("query") String query);

    // Add methods for managing stories

}
