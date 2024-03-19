package com.wellcare.wellcare.Controllers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.wellcare.wellcare.Models.Post;
import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Repositories.CommentRepository;
import com.wellcare.wellcare.Repositories.PostRepository;
import com.wellcare.wellcare.Repositories.UserRepository;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    @Autowired
    PostRepository postRepository;
    @Autowired
    CommentRepository commentRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PostModelAssembler postModelAssembler;

    // @PostMapping("/new-post")
    // public ResponseEntity<EntityModel<Post>> createPost(@RequestBody Post post,
    // Authentication authentication) throws UserException {
    // String userId = authentication.getDetails().
    // if (userId == null) {
    // throw new UserException("Missing user ID in JWT");
    // }
    // Long authorId = Long.parseLong(userId);
    // User user = optionalUser.get();
    // post.setAuthor(user);
    // post.setCreatedAt(LocalDateTime.now());
    // Post createdpost = postRepository.saveAndFlush(post);
    // EntityModel<Post> postModel = postModelAssembler.toModel(createdpost);
    // return new ResponseEntity<>(postModel, HttpStatus.CREATED);

    // }

    @PutMapping("/{postId}")
    public ResponseEntity<EntityModel<Post>> updatePost(@RequestBody Post updatedPost, @PathVariable Long postId) {
        Optional<Post> existingPostOptional = postRepository.findById(postId);
        if (existingPostOptional.isEmpty()) {
            throw new ResourceNotFoundException("Post", postId);
        }
        Post existingPost = existingPostOptional.get();
        existingPost.setContent(updatedPost.getContent());
        existingPost.setLocation(updatedPost.getLocation());
        existingPost.setAttachment(updatedPost.getAttachment());
        Post savedPost = postRepository.save(existingPost);
        EntityModel<Post> postModel = postModelAssembler.toModel(savedPost);
        return new ResponseEntity<>(postModel, HttpStatus.OK);
    }

    // to get all the posts of specific user
    @GetMapping("/{userId}")
    public ResponseEntity<List<EntityModel<Post>>> getPostsByAuthorId(@PathVariable Long userId) {
        List<Post> userposts = postRepository.findByUserId(userId);
        if (userposts.isEmpty()) {
            throw new ResourceNotFoundException("User", userId);
        }
        List<EntityModel<Post>> postModels = (List<EntityModel<Post>>) postModelAssembler.toCollectionModel(userposts);
        return new ResponseEntity<>(postModels, HttpStatus.OK);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<?> deletePost(@PathVariable Long postId) {

        Optional<Post> optionalPost = postRepository.findById(postId);
        if (optionalPost.isEmpty()) {
            throw new ResourceNotFoundException("Post", postId);
        }
        Post post = optionalPost.get();
        postRepository.delete(post);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/feed/{userId}")
    public ResponseEntity<CollectionModel<EntityModel<Post>>> getFilteredPosts(@PathVariable Long userId,
            @RequestParam(required = false) Boolean following) throws UserException {
        try {
            List<Long> userIds = getUserIds(userId, following);
            List<Post> posts = new ArrayList<>();

            for (Long id : userIds) {
                List<Post> userPosts = postRepository.findByUserId(id);
                posts.addAll(userPosts);
            }

            List<EntityModel<Post>> postModels = posts.stream()
                    .map(postModelAssembler::toModel)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(CollectionModel.of(
                    postModels,
                    linkTo(methodOn(PostController.class).getFilteredPosts(userId, following)).withSelfRel()));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @SuppressWarnings("null")
    private List<Long> getUserIds(Long userId, Boolean following) throws UserException {
        List<Long> userIds = new ArrayList<>();
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
            userIds.add(userId);
        }
        return userIds;
    }

    @SuppressWarnings("null")
    @PutMapping("/like-switcher/{postId}")
    public ResponseEntity<EntityModel<Post>> toggleLikePost(@PathVariable Long postId, @PathVariable Long userId)
            throws UserException, PostException {

        try {
            Optional<User> optionalUser = userRepository.findById(userId);
            User user = optionalUser.get();

            Optional<Post> optionalPost = postRepository.findById(postId);
            Post post = optionalPost.get();

            if (post.getLikes().contains(user))
                post.getLikes().remove(user);
            else
                post.getLikes().add(user);

            Post likedPost = postRepository.save(post);
            return ResponseEntity.ok(postModelAssembler.toModel(likedPost));

        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.notFound().build();

        }
    }

    @SuppressWarnings("null")
    @PutMapping("/save-switcher/{postId}")
    public ResponseEntity<EntityModel<Post>> toggleSavePost(@PathVariable Long postId, @PathVariable Long userId)
            throws UserException, PostException {
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
