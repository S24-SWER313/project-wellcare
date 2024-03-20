package com.wellcare.wellcare.Controllers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.Authentication;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
import com.wellcare.wellcare.Repositories.CommentRepository;
import com.wellcare.wellcare.Repositories.PostRepository;
import com.wellcare.wellcare.Repositories.UserRepository;
import com.wellcare.wellcare.Security.jwt.AuthTokenFilter;
import com.wellcare.wellcare.Security.jwt.JwtUtils;

import io.jsonwebtoken.JwtException;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;

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
    @Autowired
    JwtUtils jwtUtils;
    @Autowired
    AuthTokenFilter authTokenFilter;
    private static final Logger logger = LoggerFactory.getLogger(PostController.class);

    @Autowired
    private EntityManager entityManager;

    @PostMapping("/new-post")
    public ResponseEntity<EntityModel<Post>> createPost(HttpServletRequest request, @RequestBody Post post,
            Authentication authentication) throws UserException {
        try {
            // Extract the JWT token from the request
            String jwtToken = authTokenFilter.parseJwt(request);
            System.out.println("Extracted JWT token: " + jwtToken);

            // Parse the JWT token to extract the userId
            Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);
            System.out.println("Extracted userId: " + userId);

            // Use the extracted userId to get the User object
            Optional<User> existingUserOptional = userRepository.findById(userId);
            User user = existingUserOptional.orElseThrow(() -> new UserException("User not found"));

            post.setUser(user); // Set the User for the Post
            post.setCreatedAt(LocalDateTime.now());
            Post createdPost = postRepository.save(post);
            EntityModel<Post> postModel = postModelAssembler.toModel(createdPost);

            // Pass the userId to the linkTo method
            return new ResponseEntity<>(postModel, HttpStatus.CREATED);
        } catch (Exception ex) {
            // Log the complete exception details for better analysis
            logger.error("Error processing JWT token", ex);
            if (ex instanceof JwtException) {
                throw new UserException("Invalid JWT token: " + ex.getMessage());
            } else {
                throw new UserException("Error processing JWT token: " + ex.getMessage());
            }
        }
    }

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
    public ResponseEntity<List<EntityModel<Post>>> getPostsByUserId(@PathVariable Long userId) {
        List<Post> userposts = postRepository.findByUserId(userId);

        List<EntityModel<Post>> postModels = userposts.stream()
        .map(postModelAssembler::toModel)
        .collect(Collectors.toList());
       
        return ResponseEntity.ok(postModels);
    }

    @Transactional
    @DeleteMapping("/{postId}")
    public ResponseEntity<?> deletePost(@PathVariable Long postId) {
        Optional<Post> optionalPost = postRepository.findById(postId);
        if (optionalPost.isEmpty()) {
            throw new ResourceNotFoundException("Post", postId);
        }
        Post post = optionalPost.get();
        postRepository.deleteById(post.getId());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/feed")
    public ResponseEntity<CollectionModel<EntityModel<Post>>> getFilteredPosts(
            @RequestParam(required = false) ERole role) {

        List<Post> posts;
        if (role != null) {
            // Fetch users by role
            List<User> usersByRole = userRepository.findAllUsersByRole(role);
            if (usersByRole.isEmpty()) {
                throw new ResourceNotFoundException("Users", null, new Throwable("No users found for the given role"));
            }

            // Extract user IDs from usersByRole list
            List<Long> userIds = usersByRole.stream().map(User::getId).collect(Collectors.toList());
            System.out.println("userIds userIds userIds " + userIds);
            // Fetch posts by user IDs
            posts = postRepository.findAllPostsByUserIds(userIds)
                    .orElseThrow(() -> new ResourceNotFoundException("Posts", null,
                            new Throwable("No posts found for the given role")));
        } else {
            posts = postRepository.findAll();
        }

        List<EntityModel<Post>> postModels = posts.stream()
                .map(postModelAssembler::toModel)
                .collect(Collectors.toList());

        return ResponseEntity.ok(CollectionModel.of(
                postModels,
                linkTo(methodOn(PostController.class).getFilteredPosts(role)).withSelfRel()));
    }
    
    @Transactional
    @PutMapping("/like-switcher/{postId}")
    public ResponseEntity<EntityModel<Post>> toggleLikePost(HttpServletRequest request, @PathVariable Long postId)
            throws UserException, PostException {

        try {
            // Extract the JWT token from the request
            String jwtToken = authTokenFilter.parseJwt(request);
            System.out.println("Extracted JWT token: " + jwtToken);

            // Parse the JWT token to extract the userId
            Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);
            System.out.println("Extracted userId: " + userId);
            Optional<User> optionalUser = userRepository.findById(userId);
            User user = optionalUser.orElseThrow(() -> new UserException("User not found"));

            Optional<Post> optionalPost = postRepository.findById(postId);
            Post post = optionalPost.orElseThrow(() -> new PostException("Post not found"));

           
            user = entityManager.merge(user);

            // Initialize the likes collection
            post.getLikes().size(); // Force initialization
            post.getComments().size();
            if (post.getLikes().contains(user)){
                post.getLikes().remove(user);
                post.setNoOfLikes(post.getNoOfLikes() - 1);}
            else{
                post.getLikes().add(user);
                post.setNoOfLikes(post.getNoOfLikes() + 1);}

            Post likedPost = postRepository.save(post);
            return ResponseEntity.ok(postModelAssembler.toModel(likedPost));

        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/")
    public ResponseEntity<List<EntityModel<Post>>> getAllPosts() {
        List<Post> posts = postRepository.findAll();
        List<EntityModel<Post>> postModels = posts.stream()
                .map(postModelAssembler::toModel)
                .collect(Collectors.toList());

        return ResponseEntity.ok(postModels);
    }

    @PutMapping("/save-switcher/{postId}")
    public ResponseEntity<EntityModel<Post>> toggleSavePost(HttpServletRequest request, @PathVariable Long postId)
            throws UserException, PostException {
        try {
            // Extract the JWT token from the request
            String jwtToken = authTokenFilter.parseJwt(request);
            System.out.println("Extracted JWT token: " + jwtToken);

            // Parse the JWT token to extract the userId
            Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);
            System.out.println("Extracted userId: " + userId);
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
