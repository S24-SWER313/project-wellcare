package com.wellcare.wellcare.Assemblers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;
import com.wellcare.wellcare.Controllers.CommentController;
import com.wellcare.wellcare.Exceptions.CommentException;
import com.wellcare.wellcare.Exceptions.PostException;
import com.wellcare.wellcare.Exceptions.UserException;
import com.wellcare.wellcare.Models.Comment;

@Component
public class CommentModelAssembler implements RepresentationModelAssembler<Comment, EntityModel<Comment>> {

    @Override
    public EntityModel<Comment> toModel(Comment comment) {

        try {
            return EntityModel.of(comment,
                                       linkTo(methodOn(CommentController.class).toggleLikeComment(null, comment.getId())).withRel("toggleLike"));
                                    } catch (PostException | CommentException | UserException e) {
                                        e.printStackTrace();
                                        return EntityModel.of(comment);
                                    }
                                }

                                
}