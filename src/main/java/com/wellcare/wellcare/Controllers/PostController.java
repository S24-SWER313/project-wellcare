package com.wellcare.wellcare.Controllers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.wellcare.wellcare.Exceptions.PostException;
import com.wellcare.wellcare.Exceptions.ResourceNotFoundException;
import com.wellcare.wellcare.Exceptions.UserException;
import com.wellcare.wellcare.Models.Comment;
import com.wellcare.wellcare.Models.Post;
import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Repositories.CommentRepository;
import com.wellcare.wellcare.Repositories.PostRepository;
import com.wellcare.wellcare.Repositories.UserRepository;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private  PostRepository postRepo;
    private  CommentRepository comRepo;
    private PostModelAssembler postModelAssembler;
    private UserRepository userRepo;

    public PostController(PostRepository postRepo, CommentRepository comRepo , PostModelAssembler postModelAssembler) {
        this.postRepo = postRepo;
        this.comRepo = comRepo;
        this.postModelAssembler = postModelAssembler;
    }

     @PostMapping("/{UserId}")
    public ResponseEntity<EntityModel<Post>> createPost(@RequestBody Post post, @PathVariable Long userId) throws UserException {
        Optional<User> optionalUser = userRepo.findById(userId);
        if (optionalUser.isEmpty()) {
            throw new ResourceNotFoundException("User", userId);
        }
        User user = optionalUser.get();
        post.setAuthor(user);
        post.setCreatedAt(LocalDateTime.now());
        Post createdpost = postRepo.saveAndFlush(post);        
        EntityModel<Post> postModel = postModelAssembler.toModel(createdpost);
        return new ResponseEntity<>(postModel, HttpStatus.CREATED);
        
    } 

    @GetMapping("/{postId}")
    public ResponseEntity<EntityModel<Post>> getPostById(@PathVariable Long postId) {
            Post post = postRepo.findByIdWithLikesAndComments(postId)
                    .orElseThrow(() -> new ResourceNotFoundException("Post", postId));
                    EntityModel<Post> postModel = postModelAssembler.toModel(post);
                    return new ResponseEntity<>(postModel, HttpStatus.OK);
             
        }
    

    @PutMapping("/{postId}")
    public ResponseEntity<EntityModel<Post>> updatePost(@RequestBody Post updatedPost, @PathVariable Long postId) {
        Optional<Post> existingPostOptional = postRepo.findById(postId);
        if (existingPostOptional.isEmpty()) {
         throw new ResourceNotFoundException("Post", postId); 
        }
        Post existingPost = existingPostOptional.get();
                    existingPost.setContent(updatedPost.getContent());
                    existingPost.setLocation(updatedPost.getLocation());
                    existingPost.setAttachment(updatedPost.getAttachment());
                    Post savedPost = postRepo.save(existingPost);
                    EntityModel<Post> postModel = postModelAssembler.toModel(savedPost);
                    return new ResponseEntity<>(postModel, HttpStatus.OK);
    }

// to get all the posts of specific user
    @GetMapping("/{userId}")
    public ResponseEntity<List<EntityModel<Post>>> getPostsByAuthorId(@PathVariable Long userId) {
    List<Post> userposts = postRepo.findByAuthorId(userId);
     if (userposts.isEmpty()) {
        throw new ResourceNotFoundException("User", userId); 
    }
    List<EntityModel<Post>> postModels = (List<EntityModel<Post>>) postModelAssembler.toCollectionModel(userposts);
    return new ResponseEntity<>(postModels, HttpStatus.OK);
}
    
@DeleteMapping("/{postId}")
public ResponseEntity<?> deletePost(@PathVariable Long postId) {

        Optional<Post> optionalPost = postRepo.findById(postId);
        if (optionalPost.isEmpty()) {
            throw new ResourceNotFoundException("Post", postId);
        }
        Post post = optionalPost.get();
        postRepo.delete(post);
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    
}
}
