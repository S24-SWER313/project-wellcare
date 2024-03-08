package com.wellcare.wellcare.Controllers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.Optional;

import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.wellcare.wellcare.Exceptions.ResourceNotFoundException;
import com.wellcare.wellcare.Models.Comment;
import com.wellcare.wellcare.Models.Post;
import com.wellcare.wellcare.Repositories.CommentRepository;
import com.wellcare.wellcare.Repositories.PostRepository;

@RestController
public class PostController {

    private  PostRepository postRepo;
    private  CommentRepository comRepo;

    public PostController(PostRepository postRepo, CommentRepository comRepo) {
        this.postRepo = postRepo;
        this.comRepo = comRepo;
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<EntityModel<Post>> getPostById(@PathVariable Long postId) {
        Optional<Post> postOptional = postRepo.findByIdWithLikesAndComments(postId);
    
        if (!postOptional.isPresent()) {
        throw new ResourceNotFoundException("Post", postId); 
        }
    
        Post post = postOptional.get();
        EntityModel<Post> postModel = EntityModel.of(post);
            postModel.add(
            linkTo(methodOn(PostController.class).getPostById(postId)).withSelfRel(),
            linkTo(methodOn(PostController.class).allPosts()).slash("posts").withRel("posts"),
          //  /posts/{postId}/comments
            linkTo(methodOn(CommentController.class).getAllCommentsForPost(postId)).withRel("comments")
        );
        return ResponseEntity.ok(postModel);
    }
    @PutMapping("/posts/{postId}")
    public ResponseEntity<?> updatePost(@RequestBody Post updatedPost, @PathVariable Long postId) {
        return postRepo.findById(postId)
            .map(existingPost -> {
                existingPost.setContent(updatedPost.getContent());
                existingPost.setLocation(updatedPost.getLocation());
                existingPost.setAttachment(updatedPost.getAttachment());

                Post savedPost = postRepo.save(existingPost);
                EntityModel<Post> postModel = EntityModel.of(savedPost);
    
                postModel.add(
                    linkTo(methodOn(PostController.class).getPostById(postId)).withSelfRel(),
                    linkTo(methodOn(PostController.class).allPosts()).slash("posts").withRel("posts"),
                    //  /posts/{postId}/comments
                    linkTo(methodOn(CommentController.class).getAllCommentsForPost(postId)).withRel("comments")

                );
    
                return ResponseEntity.ok(postModel);
            })
            .orElseGet(() -> {
                updatedPost.setId(postId);
                Post createdPost = postRepo.save(updatedPost);
                EntityModel<Post> postModel = EntityModel.of(createdPost);
                postModel.add(
                    linkTo(methodOn(PostController.class).getPostById(postId)).withSelfRel(),
                    linkTo(methodOn(PostController.class).allPosts()).slash("posts").withRel("posts")
                );
                return ResponseEntity.ok(postModel);
            });
    }
    
}

