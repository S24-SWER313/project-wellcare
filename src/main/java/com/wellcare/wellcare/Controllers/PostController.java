package com.wellcare.wellcare.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wellcare.wellcare.Assemblers.PostModelAssembler;
import com.wellcare.wellcare.Exceptions.PostException;
import com.wellcare.wellcare.Exceptions.ResourceNotFoundException;
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


@RestController
@RequestMapping("/api/posts")
public class PostController {
    
    PostRepository postRepository;

    UserRepository userRepository;


    @Autowired
    PostModelAssembler postModelAssembler;

@SuppressWarnings("null")
@GetMapping("/feed/{userId}")
public ResponseEntity<CollectionModel<EntityModel<Post>>> getFilteredPosts(@PathVariable Long userId, @RequestParam(required = false) Set<ERole> roles, @RequestParam(required = false) Boolean following) throws UserException {
    try {
        List<Long> userIds = getUserIds(userId, roles, following);
        List<Post> posts = new ArrayList<>();
        
        if (roles != null && !roles.isEmpty()) {
            for (Long id : userIds) {
                for (ERole role : roles) {
                    Optional<Post> post = postRepository.findAllPostsByRole(id, role);
                    post.ifPresent(posts::add);
                }
            }
        } else {
            posts = postRepository.findAllPostByUserIds(userIds);
        }
        
        List<EntityModel<Post>> postModels = posts.stream()
                .map(postModelAssembler::toModel)
                .collect(Collectors.toList());
        return ResponseEntity.ok(CollectionModel.of(
            postModels,
            linkTo(methodOn(PostController.class).getFilteredPosts(userId, roles, following)).withSelfRel()
        ));
    } catch (ResourceNotFoundException ex) {
        return ResponseEntity.notFound().build();
    }
}

    
    @SuppressWarnings("null")
    private List<Long> getUserIds(Long userId, Set<ERole> roles, Boolean following) throws UserException {
        List<Long> userIds = new ArrayList<>();
        if (roles != null && !roles.isEmpty()) {
            for (ERole role : roles) {
                List<User> users = userRepository.findAllUsersByRole(role);
                for (User user : users) {
                    userIds.add(user.getId());
                }
            }
        } else {
            if (following != null && following) {
                Optional<User> optionalUser = userRepository.findById(userId);
                if (optionalUser.isPresent()) {
                    User user = optionalUser.get();
                    Set<User> followedUsers = user.getFollowing();
                    for (User followedUser : followedUsers) {
                        userIds.add(followedUser.getId());
                    }
                }
            } else {
                List<User> allUsers = userRepository.findAll();
                for (User user : allUsers) {
                    userIds.add(user.getId());
                }
            }
        }
        return userIds;
    }
    

    @GetMapping("/posts/{postId}")
    public ResponseEntity<EntityModel<Post>> getPostById(@PathVariable Long postId) {
        try {
            Post post = postRepository.findByIdWithLikesAndComments(postId)
                    .orElseThrow(() -> new ResourceNotFoundException("Post", postId));
            
            return ResponseEntity.ok(postModelAssembler.toModel(post));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }



    @SuppressWarnings("null")
    @PutMapping("/posts/{postId}")
    public ResponseEntity<EntityModel<Post>> updatePost(@RequestBody Post updatedPost, @PathVariable Long postId) {
        try {
            return postRepository.findById(postId)
                .map(existingPost -> {
                    existingPost.setContent(updatedPost.getContent());
                    existingPost.setLocation(updatedPost.getLocation());
                    existingPost.setAttachment(updatedPost.getAttachment());

                    postRepository.save(existingPost);
                    return ResponseEntity.ok(postModelAssembler.toModel(existingPost)); 
                })
                .orElseGet(() -> {
                    updatedPost.setId(postId);
                    Post savedPost = postRepository.save(updatedPost); 
                    return ResponseEntity.ok(postModelAssembler.toModel(savedPost)); 
                });
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    



    @SuppressWarnings("null")                                          //@RequestHeader("Authorization") String token
    @PutMapping("/toggle-like/{postId}")
    public ResponseEntity<EntityModel<Post>> toggleLikePost(@PathVariable Long postId, @PathVariable Long userId) throws UserException, PostException{
    
    try{
        Optional<User> optionalUser = userRepository.findById(userId);
        User user = optionalUser.get();

        Optional<Post> optionalPost = postRepository.findById(postId);
        Post post = optionalPost.get();

        if(post.getLikes().contains(user)) post.getLikes().remove(user);
        else post.getLikes().add(user);

        Post likedPost = postRepository.save(post);
       return ResponseEntity.ok(postModelAssembler.toModel(likedPost));

    }catch(ResourceNotFoundException ex){
        return ResponseEntity.notFound().build();

    }
        }

       @SuppressWarnings("null")
@PutMapping("/save/{postId}")                                      // @RequestHeader("Authorization") String token
public ResponseEntity<EntityModel<Post>> toggleSavePost(@PathVariable Long postId, @PathVariable Long userId) throws UserException, PostException {
    try {
        Optional<User> optionalUser = userRepository.findById(userId);
        Optional<Post> optionalPost = postRepository.findById(postId);
        
        if (optionalUser.isPresent() && optionalPost.isPresent()) {
            User user = optionalUser.get();
            Post post = optionalPost.get();
    
            if (user.getSavedPost().contains(post)) {
                user.getSavedPost().remove(post);
            } else {
                user.getSavedPost().add(post);
            }
    
            userRepository.save(user);
    
            return ResponseEntity.ok(postModelAssembler.toModel(post));
        } else {
            return ResponseEntity.notFound().build(); 
        }
    } catch (ResourceNotFoundException ex) {
       
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}

        

  
    }

