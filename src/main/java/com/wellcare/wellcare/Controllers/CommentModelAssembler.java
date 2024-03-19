package com.wellcare.wellcare.Controllers;


import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import com.wellcare.wellcare.Controllers.CommentController;
import com.wellcare.wellcare.Controllers.PostController;
import com.wellcare.wellcare.Models.Comment;

@Component
public class CommentModelAssembler implements RepresentationModelAssembler<Comment, EntityModel<Comment>> {



    @SuppressWarnings("null")
    @Override
    public EntityModel<Comment> toModel(Comment comment) {

        return EntityModel.of(comment
       
        );    

    }

}