package com.wellcare.wellcare.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wellcare.wellcare.Models.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    

    
}
