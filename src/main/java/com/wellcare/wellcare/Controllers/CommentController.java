package com.wellcare.wellcare.Controllers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wellcare.wellcare.Assemblers.CommentModelAssembler;
import com.wellcare.wellcare.Exceptions.CommentException;
import com.wellcare.wellcare.Exceptions.PostException;
import com.wellcare.wellcare.Exceptions.UserException;
import com.wellcare.wellcare.Models.Comment;
import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Services.CommentService;
import com.wellcare.wellcare.Services.UserService;

@RestController
@RequestMapping("/api/comments")
public class CommentController {
    
    @Autowired
    private CommentService commentService;

    @Autowired
    private UserService userService;

    @Autowired
    private CommentModelAssembler commentModelAssembler;


    @PostMapping("/create-comment/{postId}")
    public EntityModel<Comment> createComment(@RequestBody Comment comment, @PathVariable Long postId, @RequestHeader("Authorization") String token) throws UserException, PostException {

        User user = userService.findUserProfile(token);
        
        Comment   createdComment = commentService.createComment(comment, postId, user.getId());
        return commentModelAssembler.toModel(createdComment);
    } 

    @GetMapping("/{commentId}")
    public EntityModel<Comment> getCommentById(@PathVariable Long commentId) throws CommentException {
        
        Comment comment = commentService.findCommentById(commentId);
        return commentModelAssembler.toModel(comment);

    }

    @SuppressWarnings("null")
    @GetMapping("/{postId}")
    public CollectionModel<EntityModel<Comment>> getAllCommentsByPostId(@PathVariable Long postId) throws PostException {
        
        List<Comment> comments = commentService.findAllCommentsByPost(postId);

        List<EntityModel<Comment>> commentModels = comments.stream()
            .map(commentModelAssembler::toModel).collect(Collectors.toList());

        return CollectionModel.of(commentModels, linkTo(methodOn(CommentController.class).getAllCommentsByPostId(postId)).withSelfRel());

    }

    @PutMapping("/like/{commentId}")
    public EntityModel<Comment> likeComment(@RequestHeader("Authorization") String token, @PathVariable Long commentId) throws UserException, CommentException{
        
        User user = userService.findUserProfile(token);
        Comment comment = commentService.likComment(commentId, user.getId());
        return commentModelAssembler.toModel(comment);
    }


    @PutMapping("/unlike/{commentId}")
    public EntityModel<Comment> unlikeComment(@RequestHeader("Authorization") String token, @PathVariable Long commentId) throws UserException, CommentException{

        User user = userService.findUserProfile(token);
        Comment comment = commentService.unlikComment(commentId, user.getId());
        return commentModelAssembler.toModel(comment);
    
    }
}
