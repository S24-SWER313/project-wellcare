package com.wellcare.wellcare.Controllers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import java.util.Optional;

import org.apache.coyote.BadRequestException;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wellcare.wellcare.Exceptions.CommentException;
import com.wellcare.wellcare.Exceptions.PostException;
import com.wellcare.wellcare.Exceptions.ResourceNotFoundException;
import com.wellcare.wellcare.Models.Comment;
import com.wellcare.wellcare.Models.Post;
import com.wellcare.wellcare.Repositories.CommentRepository;
import com.wellcare.wellcare.Repositories.PostRepository;


@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private  CommentRepository comRepo;
    private  PostRepository  postRepository;
    private  CommentModelAssembler commentModelAssembler;


    public CommentController(CommentRepository comRepo , CommentModelAssembler commentModelAssembler ,  PostRepository  postRepository) {
        this.comRepo = comRepo;
        this.commentModelAssembler = commentModelAssembler;
        this.postRepository = postRepository;
    }
    //to update a comment 
   @PutMapping("/{commentId}")
    public ResponseEntity<EntityModel<Comment>> updateComment(@PathVariable Long commentId, @RequestBody Comment updatedComment) throws BadRequestException {
            Optional<Comment> optionalComment = comRepo.findById(commentId);
        if (optionalComment.isEmpty()) {
            throw new ResourceNotFoundException("Comment", commentId);
}

        Comment existingComment = optionalComment.get();
        
    
        existingComment.setContent(updatedComment.getContent());
        existingComment.setAttachment(updatedComment.getAttachment());
            comRepo.save(existingComment);
            return ResponseEntity.ok(commentModelAssembler.toModel(existingComment));
        } 


    // to delete a comment
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long postId, @PathVariable Long commentId) {  

        Optional<Comment> optionalComment = comRepo.findById(commentId);
            if (optionalComment.isEmpty()) {
                throw new ResourceNotFoundException("Comment", commentId);
            }
            Comment comment = optionalComment.get();
            comRepo.delete(comment);
            return ResponseEntity.noContent().build(); 
    }
    
    
}

