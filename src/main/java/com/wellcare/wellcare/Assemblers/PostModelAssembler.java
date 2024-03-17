package com.wellcare.wellcare.Assemblers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import java.util.Set;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import com.wellcare.wellcare.Controllers.CommentController;
import com.wellcare.wellcare.Controllers.PostController;
import com.wellcare.wellcare.Exceptions.PostException;
import com.wellcare.wellcare.Exceptions.UserException;
import com.wellcare.wellcare.Models.ERole;
import com.wellcare.wellcare.Models.Post;

@Component
public class PostModelAssembler implements RepresentationModelAssembler<Post, EntityModel<Post>> {

    @SuppressWarnings("null")
    @Override
    public EntityModel<Post> toModel(Post post) {
        try {
            Set<ERole> roles = Set.of(post.getAuthor().getRole().getName()); // Get the role directly

            return EntityModel.of(
                    post,
                    linkTo(methodOn(PostController.class).getPostById(post.getId())).withSelfRel(),
                    linkTo(methodOn(PostController.class).getFilteredPosts(post.getAuthor().getId(), roles, null))
                            .withRel("allPosts"),
                    linkTo(methodOn(PostController.class).toggleLikePost(post.getId(), post.getAuthor().getId()))
                            .withRel("toggleLike"),
                    linkTo(methodOn(PostController.class).toggleSavePost(post.getId(), post.getAuthor().getId()))
                            .withRel("toggleSave"),
                    linkTo(methodOn(CommentController.class).getAllCommentsByPostId(post.getId()))
                            .withRel("commentsForPost")

            );
        } catch (PostException | UserException e) {
            e.printStackTrace();
            return EntityModel.of(post);
        }
    }

}
