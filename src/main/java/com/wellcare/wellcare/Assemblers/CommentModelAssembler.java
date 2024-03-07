package com.wellcare.wellcare.Assemblers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Controller;

import com.wellcare.wellcare.Controllers.CommentController;
import com.wellcare.wellcare.Exceptions.CommentException;
import com.wellcare.wellcare.Exceptions.PostException;
import com.wellcare.wellcare.Exceptions.UserException;
import com.wellcare.wellcare.Models.Comment;

@Controller
public class CommentModelAssembler implements RepresentationModelAssembler<Comment, EntityModel<Comment>> {

    @SuppressWarnings("null")
    @Override
    public EntityModel<Comment> toModel(Comment comment) {
        try{
        return EntityModel.of(comment, 
        linkTo(methodOn(CommentController.class).getCommentById(comment.getId())).withSelfRel(),
        linkTo(methodOn(CommentController.class).getAllCommentsByPostId(comment.getPost().getId())).withRel("commentsForPost"),
        linkTo(methodOn(CommentController.class).likeComment(null, comment.getId())).withRel("like"),
        linkTo(methodOn(CommentController.class).unlikeComment(null ,comment.getId())).withRel("unlike")
        );    
    }catch(CommentException | UserException | PostException e){
        e.printStackTrace();
        return EntityModel.of(comment);
        }
    }
    
}
