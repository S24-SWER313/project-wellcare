package com.wellcare.wellcare.Services;

import java.util.*;
import com.wellcare.wellcare.Exceptions.CommentException;
import com.wellcare.wellcare.Exceptions.PostException;
import com.wellcare.wellcare.Exceptions.UserException;
import com.wellcare.wellcare.Models.Comment;

public interface CommentService {
    
    public Comment createComment(Comment comment, Long postId, Long userId) throws UserException, PostException;
    
    public Comment findCommentById(Long commentId) throws CommentException;

    public List<Comment> findAllCommentsByPost(Long postId) throws PostException;

    public Comment likComment(Long commentId,Long userId) throws CommentException, UserException;

    public Comment unlikComment(Long commentId,Long userId) throws CommentException, UserException;

    public void deleteComment(Long commentId) throws CommentException;

    public int countLikes(Long commentId) throws CommentException;

}
