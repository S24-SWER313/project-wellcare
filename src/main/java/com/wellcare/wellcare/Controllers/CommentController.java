package com.wellcare.wellcare.Controllers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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


    @SuppressWarnings("null")                                                                         //@RequestHeader("Authorization") string token
    @PostMapping("/{postId}")
    public ResponseEntity<EntityModel<Comment>> createComment(@RequestBody Comment comment, @PathVariable Long postId, @PathVariable Long userId) throws UserException, PostException {

    try{
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
    }catch(ResourceNotFoundException ex){
        return ResponseEntity.notFound().build();

    }
    } 

    @SuppressWarnings("null")
    @PutMapping("/{commentId}")
    public ResponseEntity<EntityModel<Comment>> updateComment(@PathVariable Long postId, @PathVariable Long commentId, @RequestBody Comment updatedComment) {
        try {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new ResourceNotFoundException("Post", postId));
    
            Comment comment = post.getComments().stream()
                    .filter(c -> c.getId().equals(commentId))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));
    
            if (updatedComment.getContent() == null && updatedComment.getAttachment() == null) {
                throw new IllegalArgumentException("Comment must have either content or attachment");
            }
    
            comment.setContent(updatedComment.getContent());
            comment.setAttachment(updatedComment.getAttachment());
    
            postRepository.save(post);
    
            return ResponseEntity.ok(commentModelAssembler.toModel(comment));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }
    
    
    @SuppressWarnings("null")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<EntityModel<Comment>> deleteComment(@PathVariable Long postId, @PathVariable Long commentId) {
        try {
            Optional<Post> optionalPost = postRepository.findById(postId);
            if (optionalPost.isEmpty()) {
                throw new ResourceNotFoundException("Post", postId);
            }
            Post post = optionalPost.get();
            Optional<Comment> optionalComment = post.getComments().stream()
                    .filter(comment -> comment.getId().equals(commentId))
                    .findFirst();
    
            if (optionalComment.isEmpty()) {
                throw new ResourceNotFoundException("Comment", commentId);
            }
            Comment comment = optionalComment.get();
            post.getComments().remove(comment);
            postRepository.save(post); 
            commentRepository.delete(comment);
    
            return ResponseEntity.ok(commentModelAssembler.toModel(comment));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @SuppressWarnings("null")
    @GetMapping("/{commentId}")
    public ResponseEntity<EntityModel<Comment>> getCommentById(@PathVariable Long commentId) {
        try {
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));
            
            return ResponseEntity.ok(commentModelAssembler.toModel(comment));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @SuppressWarnings("null")
    @GetMapping("/{postId}")
    public ResponseEntity<CollectionModel<EntityModel<Comment>>> getAllCommentsByPostId(@PathVariable Long postId) {
        try {
            Optional<Post> optionalPost = postRepository.findById(postId);
            if (optionalPost.isEmpty()) {
                return ResponseEntity.notFound().build(); 
            }
            Post post = optionalPost.get();
            List<Comment> comments = commentRepository.findAllByPost(post);
    
            List<EntityModel<Comment>> commentModels = comments.stream()
                .map(commentModelAssembler::toModel)
                .collect(Collectors.toList());
    
            return ResponseEntity.ok(CollectionModel.of(
                commentModels,
                linkTo(methodOn(CommentController.class).getAllCommentsByPostId(postId)).withSelfRel()));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @SuppressWarnings("null")
    @PutMapping("/like/{commentId}")
    public ResponseEntity<EntityModel<Comment>> toggleLikeComment(@PathVariable Long userId, @PathVariable Long commentId) {
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