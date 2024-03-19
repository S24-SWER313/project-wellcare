package com.wellcare.wellcare.Repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wellcare.wellcare.Models.Comment;
import com.wellcare.wellcare.Models.Post;

public interface CommentRepository extends JpaRepository<Comment, Long> {

        List<Comment> findAllByPost(Post post);

}
