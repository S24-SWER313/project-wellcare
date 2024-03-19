package com.wellcare.wellcare.Controllers;

import java.time.LocalDateTime;
import java.util.Optional;

import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wellcare.wellcare.Assemblers.CommentModelAssembler;
import com.wellcare.wellcare.Exceptions.PostException;
import com.wellcare.wellcare.Exceptions.ResourceNotFoundException;
import com.wellcare.wellcare.Exceptions.UserException;
import com.wellcare.wellcare.Models.Comment;
import com.wellcare.wellcare.Models.Post;
import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Repositories.CommentRepository;
import com.wellcare.wellcare.Repositories.PostRepository;
import com.wellcare.wellcare.Repositories.UserRepository;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentModelAssembler commentModelAssembler;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommentRepository commentRepository;

    @PostMapping("/{postId}")
    public ResponseEntity<EntityModel<Comment>> createComment(@RequestBody Comment comment, @PathVariable Long postId,
            @PathVariable Long userId) throws UserException, PostException {

        try {
            Optional<User> optionalUser = userRepository.findById(userId);

            User user = optionalUser.get();

            Optional<Post> optionalPost = postRepository.findById(postId);

            Post post = optionalPost.get();

            comment.setAuthor(user);
            comment.setCreatedAt(LocalDateTime.now());

            Comment createdComment = commentRepository.save(comment);
            post.getComments().add(createdComment);

            postRepository.save(post);

            return ResponseEntity.ok(commentModelAssembler.toModel(createdComment));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.notFound().build();

        }
    }

    // to update a comment
    @PutMapping("/{commentId}")
    public ResponseEntity<EntityModel<Comment>> updateComment(@PathVariable Long commentId,
            @RequestBody Comment updatedComment) throws BadRequestException {
        Optional<Comment> optionalComment = commentRepository.findById(commentId);
        if (optionalComment.isEmpty()) {
            throw new ResourceNotFoundException("Comment", commentId);
        }

        Comment existingComment = optionalComment.get();

        existingComment.setContent(updatedComment.getContent());
        existingComment.setAttachment(updatedComment.getAttachment());
        commentRepository.save(existingComment);
        return ResponseEntity.ok(commentModelAssembler.toModel(existingComment));
    }

    // to delete a comment
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId) {

        Optional<Comment> optionalComment = commentRepository.findById(commentId);
        if (optionalComment.isEmpty()) {
            throw new ResourceNotFoundException("Comment", commentId);
        }
        Comment comment = optionalComment.get();
        commentRepository.delete(comment);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/like-switcher/{commentId}")
    public ResponseEntity<EntityModel<Comment>> toggleLikeComment(@PathVariable Long userId,
            @PathVariable Long commentId) {
        try {
            Optional<User> optionalUser = userRepository.findById(userId);
            User user = optionalUser.orElseThrow(() -> new ResourceNotFoundException("User", userId));

            Optional<Comment> optionalComment = commentRepository.findById(commentId);
            Comment comment = optionalComment.orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

            if (comment.getCommentLikes().contains(user)) {
                comment.getCommentLikes().remove(user);
            } else {
                comment.getCommentLikes().add(user);
            }

            Comment savedComment = commentRepository.save(comment);

            return ResponseEntity.ok(commentModelAssembler.toModel(savedComment));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
