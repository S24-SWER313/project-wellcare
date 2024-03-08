package com.wellcare.wellcare.Controllers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import java.util.Optional;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.wellcare.wellcare.Exceptions.CommentException;
import com.wellcare.wellcare.Exceptions.PostException;
import com.wellcare.wellcare.Exceptions.ResourceNotFoundException;
import com.wellcare.wellcare.Models.Comment;
import com.wellcare.wellcare.Models.Post;
import com.wellcare.wellcare.Repositories.CommentRepository;
import com.wellcare.wellcare.Repositories.PostRepository;

import io.micrometer.common.util.StringUtils;

@RestController
public class CommentController {

    private final CommentRepository comRepo;
    private final PostRepository postRepo;

    public CommentController(PostRepository postRepo, CommentRepository comRepo) {
        this.postRepo = postRepo;
        this.comRepo = comRepo;
    }
    //to update a comment 
    @PutMapping("/posts/{postId}/comments/{commentId}")
    public ResponseEntity<?> updateComment(@PathVariable Long postId, @PathVariable Long commentId, @RequestBody Comment updatedComment)throws CommentException , PostException {
        Optional<Post> optionalPost = postRepo.findById(postId);
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
        // to ensure that the content is not empty 
        if (StringUtils.isBlank(updatedComment.getContent()) && updatedComment.getAttachment() == null) {
            throw new IllegalArgumentException("Comment must have either content or attachment");
        }
        comment.setContent(updatedComment.getContent());
        comment.setAttachment(updatedComment.getAttachment());
        postRepo.save(post);
        EntityModel<Comment> commentModel = EntityModel.of(comment);
        commentModel.add(
            linkTo(methodOn(CommentController.class).updateComment(postId, commentId, updatedComment)).withSelfRel(),
            //   /posts
            linkTo(methodOn(PostController.class).all()).slash("posts").withRel("posts"),
            //   /posts/{postId}
            linkTo(methodOn(PostController.class).getPostById(postId)).withRel("post"),
            //   /posts/{postId}/comments
            linkTo(methodOn(CommentController.class).getAllCommentsForPost(postId)).withRel("comments")  );
    
        return ResponseEntity.ok(commentModel);
    }
    
   
    // to delete a comment
    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long postId, @PathVariable Long commentId) throws CommentException , PostException {
    Optional<Post> optionalPost = postRepo.findById(postId);
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
    comRepo.delete(comment);// delete the comment from the DB
    EntityModel<Comment> commentModel = EntityModel.of(comment);
    commentModel.add(
        linkTo(methodOn(CommentController.class).deleteComment(postId, commentId)).withSelfRel(),
        //   /posts
        linkTo(methodOn(CommentController.class).getAllPosts()).slash("posts").withRel("posts"),
        //   /posts/{postId}
        linkTo(methodOn(CommentController.class).getPostById(postId)).withRel("post"),
        //   /posts/{postId}/comments
        linkTo(methodOn(CommentController.class).getAllCommentsForPost(postId)).withRel("comments")  );
    return ResponseEntity.noContent().build();
}

}
