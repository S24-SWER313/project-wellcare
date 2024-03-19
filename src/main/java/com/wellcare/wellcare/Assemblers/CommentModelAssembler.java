package com.wellcare.wellcare.Assemblers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import com.wellcare.wellcare.Controllers.CommentController;
import com.wellcare.wellcare.Models.Comment;

@Component
public class CommentModelAssembler implements RepresentationModelAssembler<Comment, EntityModel<Comment>> {

    @Override
    public EntityModel<Comment> toModel(Comment comment) {

        return EntityModel.of(comment,
                linkTo(methodOn(CommentController.class).toggleLikeComment(comment.getAuthor().getId(),
                        comment.getId())).withRel("toggleLike"));

    }

}