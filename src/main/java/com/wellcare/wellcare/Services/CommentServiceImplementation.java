package com.wellcare.wellcare.Services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.wellcare.wellcare.Exceptions.CommentException;
import com.wellcare.wellcare.Exceptions.PostException;
import com.wellcare.wellcare.Exceptions.UserException;
import com.wellcare.wellcare.Models.Comment;
import com.wellcare.wellcare.Models.Post;
import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Repositories.CommentRepository;
import com.wellcare.wellcare.Repositories.PostRepository;

public class CommentServiceImplementation implements CommentService{

     @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Override
    public Comment createComment(Comment comment, Long postId, Long userId) throws UserException, PostException {
        User user = userService.findUserById(userId);
        Post post = postService.findPostById(postId);
        
        
        comment.setAuthor(user);
        comment.setCreatedAt(LocalDateTime.now());

        Comment createdComment = commentRepository.save(comment);


        post.getComments().add(createdComment);

        postRepository.save(post);

        return createdComment;
    }

    @SuppressWarnings("null")
    @Override
    public Comment findCommentById(Long commentId) throws CommentException {
        Optional<Comment> opt = commentRepository.findById(commentId);

        if(opt.isPresent()) return opt.get();
        
        throw new CommentException("Comment with id "+ commentId + " does not exist.");
    }

    @Override
    public Comment likComment(Long commentId, Long userId) throws CommentException, UserException {
        
        User user = userService.findUserById(userId);
        Comment comment = findCommentById(commentId);

        comment.getCommentLikes().add(user);

        
        return commentRepository.save(comment);
    }

    @Override
    public Comment unlikComment(Long commentId, Long userId) throws CommentException, UserException {
        User user = userService.findUserById(userId);
        Comment comment = findCommentById(commentId);

        comment.getCommentLikes().remove(user);

        
        return commentRepository.save(comment);
    }

    @Override
    public List<Comment> findAllCommentsByPost(Long postId) throws PostException {
            Post post = postService.findPostById(postId);
        List<Comment> allComments = commentRepository.findAll();
        return allComments.stream()
                .filter(comment -> comment.getPost().equals(post))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteComment(Long commentId) throws CommentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteComment'");
    }

    @Override
    public int countLikes(Long commentId) throws CommentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'countLikes'");
    }
    
}
