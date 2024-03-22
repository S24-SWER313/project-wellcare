package com.wellcare.wellcare.Controllers;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.Authentication;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
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
import com.wellcare.wellcare.Exceptions.CommentException;
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
import com.wellcare.wellcare.payload.response.MessageResponse;

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
        HttpServletRequest request) throws UserException, PostException {

    try {
        String jwtToken = authTokenFilter.parseJwt(request);
        System.out.println("Extracted JWT token: " + jwtToken);

        Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);
        System.out.println("Extracted userId: " + userId);

        Optional<User> existingUserOptional = userRepository.findById(userId);
        User user = existingUserOptional.orElseThrow(() -> new UserException("User not found"));

        Optional<Post> optionalPost = postRepository.findById(postId);
        Post post = optionalPost.orElseThrow(() -> new PostException("Post not found"));

        comment.setUser(user);
        comment.setCreatedAt(LocalDateTime.now());

        comment.setPost(post);

        Comment createdComment = commentRepository.save(comment);
        post.getComments().add(createdComment);
        post.setNoOfComments(post.getNoOfComments() + 1);
        System.out.println("Incremented noOfComments to: " + post.getNoOfComments()); // Add logging

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

    @Transactional
    @DeleteMapping("/{commentId}")
    public ResponseEntity<MessageResponse> deleteComment(@PathVariable Long commentId) {
        try {
            Optional<Comment> optionalComment = commentRepository.findById(commentId);
            if (optionalComment.isEmpty()) {
                throw new ResourceNotFoundException("Comment", commentId);
            }
            Comment comment = optionalComment.get();
                        
            Post post = comment.getPost();
            if (post != null && post.getId().equals(comment.getPost().getId())) {
                post.getComments().remove(comment);
                post.setNoOfComments(post.getNoOfComments() - 1);
                postRepository.save(post); // This line saves the updated Post object with decreased noOfComments
            }
    
            commentRepository.deleteById(commentId);
            
            return ResponseEntity.ok(new MessageResponse("Comment deleted successfully"));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(new MessageResponse("Failed to delete comment with ID: " + commentId));
        }
    }
    

    @Transactional
    @PutMapping("/like-switcher/{commentId}")
    public ResponseEntity<EntityModel<Comment>> toggleLikeComment(HttpServletRequest request, @PathVariable Long commentId)
            throws UserException, PostException, CommentException {
    
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
                                               .orElseThrow(() -> new CommentException("Comment not found"));
    
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
    
            Comment likedComment = commentRepository.save(comment);
            return ResponseEntity.ok(commentModelAssembler.toModel(likedComment));
    
        } catch (UserException ex) {
            // Handle exceptions
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }catch (CommentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}    