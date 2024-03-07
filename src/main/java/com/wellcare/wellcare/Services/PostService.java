package com.wellcare.wellcare.Services;

import java.util.List;

import com.wellcare.wellcare.Exceptions.PostException;
import com.wellcare.wellcare.Exceptions.UserException;
import com.wellcare.wellcare.Models.ERole;
import com.wellcare.wellcare.Models.Post;

public interface PostService {

    public Post createPost (Post post, Long userId) throws UserException;
    public String deletePost (Long postId, Long userId) throws UserException, PostException;

    public List<Post> findPostByUserId(Long userId) throws UserException;

    public Post findPostById(Long postId) throws PostException;

    public List<Post> findPostsByRole(List<Long> userIds, ERole role) throws UserException, PostException;

    public Post findPostByRole(Long userId, ERole role) throws UserException, PostException;

    public List<Post> findAllPostsByUserIds(List<Long> userIds) throws UserException, PostException;

    public String savePost(Long postId, Long userId) throws UserException, PostException;
    public String unsavePost(Long postId, Long userId) throws UserException, PostException;

    public Post likePost(Long postId, Long userId) throws UserException, PostException;
    public Post unlikePost(Long postId, Long userId) throws UserException, PostException;
    
}
