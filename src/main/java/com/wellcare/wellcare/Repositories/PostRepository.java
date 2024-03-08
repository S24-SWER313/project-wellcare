package com.wellcare.wellcare.Repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.wellcare.wellcare.Models.Post;
import com.wellcare.wellcare.Models.User;
import java.util.List;


public interface PostRepository extends JpaRepository<Post, Long>{

      @Query("SELECT p FROM Post p LEFT JOIN FETCH p.likedBy LEFT JOIN FETCH p.comments WHERE p.id = ?1")
    Optional<Post> findByIdWithLikesAndComments(Long postId);

       public Optional<Post> findById(Long id);

}
