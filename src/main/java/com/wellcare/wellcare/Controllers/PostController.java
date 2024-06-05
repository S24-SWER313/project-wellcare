package com.wellcare.wellcare.Controllers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.Authentication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wellcare.wellcare.Assemblers.PostModelAssembler;
import com.wellcare.wellcare.Exceptions.PostException;
import com.wellcare.wellcare.Exceptions.ResourceNotFoundException;
import com.wellcare.wellcare.Exceptions.UserException;
import com.wellcare.wellcare.Models.Comment;
import com.wellcare.wellcare.Models.ERole;
import com.wellcare.wellcare.Models.Post;
import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Repositories.CommentRepository;
import com.wellcare.wellcare.Repositories.PostRepository;
import com.wellcare.wellcare.Repositories.UserRepository;
import com.wellcare.wellcare.Security.jwt.AuthTokenFilter;
import com.wellcare.wellcare.Security.jwt.JwtUtils;
import com.wellcare.wellcare.Security.services.UserDetailsImpl;
import com.wellcare.wellcare.Storage.StorageService;
import com.wellcare.wellcare.payload.response.MessageResponse;

import io.jsonwebtoken.JwtException;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:3000")
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

    @Autowired
    StorageService storageService;

    private static final Logger logger = LoggerFactory.getLogger(PostController.class);

    @Autowired
    private PagedResourcesAssembler<Post> pagedResourcesAssembler;

    @Autowired
    private EntityManager entityManager;

    @Transactional
    @PostMapping("/new-post")
    public ResponseEntity<EntityModel<Post>> createPost(HttpServletRequest request,
            @Valid @ModelAttribute Post post,
            @RequestParam(value = "file", required = false) MultipartFile[] files,
            Authentication authentication) throws UserException {
        try {

            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Extract the JWT token from the request
            String jwtToken = authTokenFilter.parseJwt(request);
            System.out.println("Extracted JWT token: " + jwtToken);

            // Parse the JWT token to extract the userId
            Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);
            System.out.println("Extracted userId: " + userId);

            // Use the extracted userId to get the User object
            Optional<User> existingUserOptional = userRepository.findById(userId);
            User user = existingUserOptional.orElseThrow(() -> new UserException("User not found"));
            post.setId(null);
            post.setUser(user); // Set the User for the Post
            post.setCreatedAt(LocalDateTime.now());

            List<String> attachmentUrls = new ArrayList<>();

            if (files != null) {
                for (MultipartFile file : files) {
                    System.out.println("Received file: " + file.getOriginalFilename());
                    String fileName = storageService.store(file);
                    String url = "http://localhost:8080/files/" + fileName;
                    attachmentUrls.add(url);
                }
            }

            post.setAttachment(attachmentUrls); // Set attachments to the post

            Post createdPost = postRepository.save(post);

            user.setPostsCount(user.getPostsCount() + 1);
            userRepository.save(user);

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

    @Transactional
    @PutMapping("/{postId}")
    public ResponseEntity<EntityModel<Post>> updatePost(HttpServletRequest request,
            @ModelAttribute Post updatedPost,
            @PathVariable Long postId,
            @RequestParam(value = "file", required = false) MultipartFile[] files,
            Authentication authentication) throws PostException, UserException {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Extract the JWT token from the request
            String jwtToken = authTokenFilter.parseJwt(request);
            System.out.println("Extracted JWT token: " + jwtToken);

            // Parse the JWT token to extract the userId
            Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);
            System.out.println("Extracted userId: " + userId);

            // Use the extracted userId to get the User object
            Optional<User> existingUserOptional = userRepository.findById(userId);
            User user = existingUserOptional.orElseThrow(() -> new UserException("User not found"));

            Optional<Post> existingPostOptional = postRepository.findById(postId);
            if (existingPostOptional.isEmpty()) {
                throw new ResourceNotFoundException("Post", postId);
            }
            Post existingPost = existingPostOptional.get();

            if (!existingPost.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            entityManager.detach(existingPost);

            // Update only the fields that are not null in the updatedPost
            if (updatedPost.getContent() != null) {
                existingPost.setContent(updatedPost.getContent());
            }
            if (updatedPost.getLocation() != null) {
                existingPost.setLocation(updatedPost.getLocation());
            }

            // Keep existing attachments if files are not updated
            List<String> existingAttachments = existingPost.getAttachment() != null ? existingPost.getAttachment()
                    : new ArrayList<>();

            List<String> attachmentUrls = new ArrayList<>(existingAttachments);

            if (files != null && files.length > 0) {
                for (MultipartFile file : files) {
                    storageService.store(file);
                    String filename = file.getOriginalFilename();
                    String url = "http://localhost:8080/files/" + filename;
                    attachmentUrls.add(url);
                }
            }

            existingPost.setAttachment(attachmentUrls); // Set attachments to the existing post

            Post savedPost = postRepository.save(existingPost);

            EntityModel<Post> postModel = postModelAssembler.toModel(savedPost);
            return new ResponseEntity<>(postModel, HttpStatus.OK);
        } catch (Exception ex) {
            logger.error("Error updating post", ex);
            throw new PostException("Error updating post: " + ex.getMessage());
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<PagedModel<EntityModel<Post>>> getPostsByUserId(@PathVariable Long userId,
            Pageable pageable) {
        Page<Post> userPosts = postRepository.findByUserId(userId, pageable);

        PagedModel<EntityModel<Post>> pagedModel = pagedResourcesAssembler.toModel(userPosts, postModelAssembler);

        return new ResponseEntity<>(pagedModel, HttpStatus.OK);

    }

    @GetMapping("/{userId}/count")
    public ResponseEntity<Long> getPostCountByUserId(@PathVariable Long userId) {
        Long postCount = postRepository.countPostsByUserId(userId);
        return new ResponseEntity<>(postCount, HttpStatus.OK);
    }

    @Transactional
    @DeleteMapping("/{postId}")
    public ResponseEntity<MessageResponse> deletePost(@PathVariable Long postId) {
        try {
            Optional<Post> optionalPost = postRepository.findById(postId);
            if (optionalPost.isEmpty()) {
                throw new ResourceNotFoundException("Post", postId);
            }
            Post post = optionalPost.get();

            List<User> users = userRepository.findBySavedPost(post);
            for (User user : users) {
                user.getSavedPost().remove(post);
            }

            List<Comment> postComments = post.getComments();
            commentRepository.deleteAll(postComments);

            postRepository.delete(post);
            return ResponseEntity.ok(new MessageResponse("Post deleted successfully"));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("Failed to delete post: Post with ID " + postId + " not found"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Failed to delete post with ID: " + postId));
        }
    }

    @GetMapping("/feed")
    public ResponseEntity<PagedModel<EntityModel<Post>>> getFilteredPosts(
            @RequestParam(required = false) ERole role,
            @RequestParam(required = false) Boolean friendsOnly,
            Pageable pageable,
            @AuthenticationPrincipal UserDetailsImpl userDetails) throws UserException {

        Page<Post> postsPage;

        if (role != null && Boolean.TRUE.equals(friendsOnly)) {
            // Filter posts based on role and following
            if (userDetails != null && userDetails.getId() != null) {
                List<User> friendsUsers = userRepository.findById(userDetails.getId())
                        .orElseThrow(() -> new UserException("User not found")).getFriends();

                List<User> usersByRole = userRepository.findAllUsersByRole(role);
                if (usersByRole.isEmpty()) {
                    // Return empty response if no users found for the given role
                    return ResponseEntity.ok(PagedModel.empty());
                }

                List<Long> userIds = usersByRole.stream().map(User::getId).collect(Collectors.toList());
                postsPage = postRepository.findAllPostsByUserIds(userIds, pageable);

                if (postsPage.isEmpty()) {
                    // Return empty response if no posts found for the given role
                    return ResponseEntity.ok(PagedModel.empty());
                }

                List<Post> roleBasedPosts = postsPage.getContent().stream()
                        .filter(post -> friendsUsers.contains(post.getUser()))
                        .collect(Collectors.toList());

                postsPage = new PageImpl<>(roleBasedPosts, pageable, roleBasedPosts.size());

            } else {
                throw new UserException("User details not found");
            }
        } else if (role != null) {
            // Filter posts based on role
            List<User> usersByRole = userRepository.findAllUsersByRole(role);
            if (usersByRole.isEmpty()) {
                // Return empty response if no users found for the given role
                return ResponseEntity.ok(PagedModel.empty());
            }

            List<Long> userIds = usersByRole.stream().map(User::getId).collect(Collectors.toList());
            postsPage = postRepository.findAllPostsByUserIds(userIds, pageable);

            if (postsPage.isEmpty()) {
                // Return empty response if no posts found for the given role
                return ResponseEntity.ok(PagedModel.empty());
            }
        } else if (Boolean.TRUE.equals(friendsOnly)) {
            // Filter posts to show only those from people the user follows
            if (userDetails != null && userDetails.getId() != null) {
                List<User> followingUsers = userRepository.findById(userDetails.getId())
                        .orElseThrow(() -> new UserException("User not found")).getFriends();
                List<Post> followingUsersPosts = postRepository.findAllByUserInOrderByCreatedAtDesc(followingUsers);
                postsPage = new PageImpl<>(followingUsersPosts, pageable, followingUsersPosts.size());
            } else {
                throw new UserException("User details not found");
            }
        } else {
            // Fetch all posts
            postsPage = postRepository.findAllWithLikesAndComments(pageable);
        }

        PagedModel<EntityModel<Post>> pagedModel = pagedResourcesAssembler.toModel(postsPage, postModelAssembler);

        return ResponseEntity.ok(pagedModel);
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
            if (optionalPost.isEmpty()) {
                throw new PostException("Post not found");
            }

            Post post = optionalPost.get();

            boolean hasLiked = post.getLikes().contains(user);

            if (hasLiked) {
                post.getLikes().remove(user);
                post.setNoOfLikes(post.getNoOfLikes() - 1);
            } else {
                post.getLikes().add(user);
                post.setNoOfLikes(post.getNoOfLikes() + 1);
            }

            Post likedPost = postRepository.save(post);
            EntityModel<Post> postModel = postModelAssembler.toModel(likedPost);
            return new ResponseEntity<>(postModel, HttpStatus.OK);
        } catch (Exception ex) {
            logger.error("Error toggling like on post", ex);
            throw new PostException("Error toggling like on post: " + ex.getMessage());
        }
    }

    @Transactional
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
            User user = optionalUser.orElseThrow(() -> new UserException("User not found"));

            Optional<Post> optionalPost = postRepository.findById(postId);
            if (optionalPost.isEmpty()) {
                throw new PostException("Post not found");
            }

            Post post = optionalPost.get();

            boolean hasSaved = post.getSavedBy().contains(user);

            if (hasSaved) {
                post.getSavedBy().remove(user);
            } else {
                post.getSavedBy().add(user);
            }

            Post savedBy = postRepository.save(post);
            EntityModel<Post> postModel = postModelAssembler.toModel(savedBy);
            return new ResponseEntity<>(postModel, HttpStatus.OK);
        } catch (Exception ex) {
            logger.error("Error toggling save on post", ex);
            throw new PostException("Error toggling save on post: " + ex.getMessage());
        }
    }

    @Transactional
    @GetMapping("/saved-posts")
    public ResponseEntity<PagedModel<EntityModel<Post>>> getAllSavedPosts(HttpServletRequest request, Pageable pageable)
            throws UserException {
        try {
            // Extract the JWT token from the request
            String jwtToken = authTokenFilter.parseJwt(request);
            System.out.println("Extracted JWT token: " + jwtToken);

            // Parse the JWT token to extract the userId
            Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);
            System.out.println("Extracted userId: " + userId);

            // Retrieve the user entity or throw exception if not found
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserException("User not found"));

            // Retrieve saved posts for the user with pagination
            Page<Post> savedPostsPage = postRepository.findBySavedBy(user, pageable);

            // Convert the page of saved posts to a paged model
            PagedModel<EntityModel<Post>> pagedModel = pagedResourcesAssembler.toModel(savedPostsPage,
                    postModelAssembler);

            return ResponseEntity.ok(pagedModel);
        } catch (UserException ex) {
            ex.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<Comment>> getCommentsForPost(@PathVariable Long postId) {
        Optional<Post> optionalPost = postRepository.findById(postId);
        if (optionalPost.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Post post = optionalPost.get();
        List<Comment> comments = post.getComments();

        return ResponseEntity.ok(comments);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Post>> searchPostHandler(@RequestParam("q") String query) throws PostException {
        List<Post> posts = postRepository.findByContent(query);

        return new ResponseEntity<List<Post>>(posts, HttpStatus.OK);
    }

}