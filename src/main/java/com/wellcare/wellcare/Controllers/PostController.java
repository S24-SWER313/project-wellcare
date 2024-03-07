package com.wellcare.wellcare.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wellcare.wellcare.Assemblers.PostModelAssembler;
import com.wellcare.wellcare.Exceptions.PostException;
import com.wellcare.wellcare.Exceptions.UserException;
import com.wellcare.wellcare.Models.ERole;
import com.wellcare.wellcare.Models.Post;
import com.wellcare.wellcare.Models.User;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.*;
import java.util.stream.Collectors;

import com.wellcare.wellcare.Repositories.PostRepository;
import com.wellcare.wellcare.Repositories.UserRepository;
import com.wellcare.wellcare.Services.PostService;
import com.wellcare.wellcare.Services.UserService;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    
    PostRepository postRepository;

    UserRepository userRepository;

    UserService userService;

    PostService postService;

    @Autowired
    PostModelAssembler postModelAssembler;

    //feed
    @SuppressWarnings("null")
    @GetMapping("/following/{ids}")
    public CollectionModel<EntityModel<Post>> getAllPostByUserIds(@PathVariable("ids") List<Long> userIds) throws UserException, PostException {
        List<Post> posts = postService.findAllPostsByUserIds(userIds);
        List<EntityModel<Post>> postModels = posts.stream()
            .map(postModelAssembler::toModel).collect(Collectors.toList());
        
        return CollectionModel.of(postModels, linkTo(methodOn(PostController.class).getAllPostByUserIds(userIds)).withSelfRel());
  
    }

    //feed to see all the posts by a doctor or by a patient
    @SuppressWarnings("null")
    @GetMapping("/role/{role}")
    public CollectionModel<EntityModel<Post>> getPostsByRole(@RequestParam("userIds") List<Long> userIds, @PathVariable ERole role) throws UserException, PostException {
        List<Post> posts = postService.findPostsByRole(userIds, role);
        List<EntityModel<Post>> postModels = posts.stream()
                .map(postModelAssembler::toModel)
                .collect(Collectors.toList());
        return CollectionModel.of(postModels, linkTo(methodOn(PostController.class).getPostsByRole(userIds, role)).withSelfRel());
    }

    //see one post by a doctor or by a patient
    @GetMapping("/{postId}/{role}")
    public EntityModel<Post> getPostByRole(@PathVariable Long postId, @PathVariable ERole role) throws UserException, PostException{

        Post post = postService.findPostByRole(postId, role);
        return postModelAssembler.toModel(post);
    }


    //see one post by a post id - regardless whether he's a doctor or a patient
    @GetMapping("/{postId}")
    public EntityModel<Post> getPostById(@PathVariable Long postId) throws PostException{

    Post post = postService.findPostById(postId);        
    
    return postModelAssembler.toModel(post);
}




 @PutMapping("/like/{postId}")
public EntityModel<Post> likePost(@PathVariable Long postId, @RequestHeader("Authorization") String token) throws UserException, PostException{

    User user = userService.findUserProfile(token);
    Post post = postService.likePost(postId, user.getId());

   return postModelAssembler.toModel(post);
    }

    @PutMapping("/unlike/{postId}")
    public EntityModel<Post> unlikePost(@PathVariable Long postId, @RequestHeader("Authorization") String token) throws UserException, PostException{
    
        User user = userService.findUserProfile(token);
        Post post = postService.unlikePost(postId, user.getId());
    
       return postModelAssembler.toModel(post);
        }

    @PutMapping("/save/{postId}")
    public EntityModel<Post> savePost(@PathVariable Long postId,@RequestHeader("Authorization") String token) throws UserException, PostException{
    
        return null;
    }

    @PutMapping("/unsave/{postId}")
    public EntityModel<Post> unsavePost(@PathVariable Long postId,@RequestHeader("Authorization") String token) throws UserException, PostException{
    
        return null;
    }
}
