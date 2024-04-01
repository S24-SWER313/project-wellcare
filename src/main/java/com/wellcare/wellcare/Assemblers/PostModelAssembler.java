package com.wellcare.wellcare.Assemblers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;
import com.wellcare.wellcare.Controllers.PostController;
import com.wellcare.wellcare.Exceptions.PostException;
import com.wellcare.wellcare.Exceptions.UserException;
import com.wellcare.wellcare.Models.Post;

@Component
public class PostModelAssembler implements RepresentationModelAssembler<Post, EntityModel<Post>> {
    
    @Override
    public EntityModel<Post> toModel(Post post) {
        try {

            return EntityModel.of(
                    post,
                    linkTo(methodOn(PostController.class).getPostsByUserId(post.getUser().getId(), null)).withRel("allPosts"),
                    linkTo(methodOn(PostController.class).toggleLikePost(null, post.getId())).withRel("toggleLike"),
                    linkTo(methodOn(PostController.class).toggleSavePost(null, post.getId())).withRel("toggleSave"));

        } catch (PostException | UserException e) {
            e.printStackTrace();
            return EntityModel.of(post);
        }
    }

}