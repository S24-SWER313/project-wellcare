package com.wellcare.wellcare.Controllers;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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
import com.wellcare.wellcare.Security.jwt.AuthTokenFilter;
import com.wellcare.wellcare.Security.jwt.JwtUtils;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;

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
      @Autowired
    JwtUtils jwtUtils;
    @Autowired
    AuthTokenFilter authTokenFilter;
    private static final Logger logger = LoggerFactory.getLogger(CommentController.class);

    @Autowired
    private EntityManager entityManager;


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
    @Transactional
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId) {
        Optional<Comment> optionalComment = commentRepository.findById(commentId);
        if (optionalComment.isEmpty()) {
            throw new ResourceNotFoundException("Comment", commentId);
        }
        Comment comment = optionalComment.get();
        commentRepository.deleteById(comment.getId());
        return ResponseEntity.noContent().build();
    }

    @Transactional
    @PutMapping("/like-switcher/{commentId}")
    public ResponseEntity<EntityModel<Comment>> toggleLikeComment(HttpServletRequest request, @PathVariable Long commentId)
            throws UserException, PostException {
    
        try {
            // Extract the JWT token from the request
            String jwtToken = authTokenFilter.parseJwt(request);
            System.out.println("Extracted JWT token: " + jwtToken);
    
            // Parse the JWT token to extract the userId
            Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);
            System.out.println("Extracted userId: " + userId);
    
            // Retrieve the user from the repository
            User user = userRepository.findById(userId)
                                     .orElseThrow(() -> new UserException("User not found"));
    
            // Retrieve the comment from the repository
            Comment comment = commentRepository.findById(commentId)
                                               .orElseThrow(() -> new PostException("Comment not found"));
    
            user = entityManager.merge(user);

            // Toggle like status
            Set<User> likes = comment.getCommentLikes();
            if (likes.contains(user)) {
                likes.remove(user);
                comment.setNoOfLikes(comment.getNoOfLikes() - 1);
            } else {
                likes.add(user);
                comment.setNoOfLikes(comment.getNoOfLikes() + 1);
            }
    
            // Save the updated comment
            Comment likedComment = commentRepository.save(comment);
            return ResponseEntity.ok(commentModelAssembler.toModel(likedComment));
    
        } catch (UserException | PostException ex) {
            // Handle exceptions
            return ResponseEntity.notFound().build();
        }
    }
}    