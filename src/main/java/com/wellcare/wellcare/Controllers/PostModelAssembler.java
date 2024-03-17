package com.wellcare.wellcare.Controllers;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.Collections;



import com.wellcare.wellcare.Controllers.PostController;
import com.wellcare.wellcare.Exceptions.PostException;
import com.wellcare.wellcare.Exceptions.UserException;
import com.wellcare.wellcare.Models.Post;

@Component
public class PostModelAssembler implements RepresentationModelAssembler<Post, EntityModel<Post>> {

    @SuppressWarnings("null")
    @Override
    public EntityModel<Post> toModel(Post post) {
        return EntityModel.of(post,
                linkTo(methodOn(PostController.class).getPostById(post.getId())).withSelfRel()
        );
    }

  
}