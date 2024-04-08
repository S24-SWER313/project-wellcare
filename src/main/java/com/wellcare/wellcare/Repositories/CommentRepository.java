package com.wellcare.wellcare.Repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.wellcare.wellcare.Models.Comment;
import com.wellcare.wellcare.Models.Post;

public interface CommentRepository extends JpaRepository<Comment, Long> {

        @Query("SELECT c FROM Comment c WHERE c.id IN (SELECT cm.id FROM Post p JOIN p.comments cm WHERE p = ?1)")
        List<Comment> findAllByPost(Post post);

      

}
