package com.wellcare.wellcare.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wellcare.wellcare.Exceptions.PostException;
import com.wellcare.wellcare.Exceptions.UserException;
import com.wellcare.wellcare.Models.ERole;
import com.wellcare.wellcare.Models.Post;
import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Repositories.PostRepository;

@Service
public class PostServiceImplementation implements PostService {

      @Autowired
    private PostRepository postRepository;


    @Autowired
    private UserService userService;

    @Override
    public Post createPost(Post post, Long userId) throws UserException {
        User user = userService.findUserById(userId);
        post.setAuthor(user);
        return postRepository.save(post);
    }

    @Override
    public String deletePost(Long postId, Long userId) throws UserException, PostException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deletePost'");
    }

    @Override
    public List<Post> findPostByUserId(Long userId) throws UserException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findPostByUserId'");
    }

    @Override
    public Post findPostById(Long postId) throws PostException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findPostById'");
    }

    @Override
    public List<Post> findAllPostsByUserIds(List<Long> userIds) throws UserException, PostException {
        
            List<Post> posts = postRepository.findAllPostByUserIds(userIds);
            if (posts.isEmpty()) throw new PostException("No posts available."); 
            return posts;
    }

    @Override
    public String savePost(Long postId, Long userId) throws UserException, PostException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'savePost'");
    }

    @Override
    public String unsavePost(Long postId, Long userId) throws UserException, PostException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'unsavePost'");
    }

    @Override
    public Post likePost(Long postId, Long userId) throws UserException, PostException {
        Post post = findPostById(postId);
        User user = userService.findUserById(userId);
        post.getLikes().add(user);
        return postRepository.save(post);

    }

    @Override
    public Post unlikePost(Long postId, Long userId) throws UserException, PostException {
        Post post = findPostById(postId);
        User user = userService.findUserById(userId);
        post.getLikes().remove(user);
        return postRepository.save(post);
    }

    @Override
    public List<Post> findPostsByRole(List<Long> userIds, ERole role) throws UserException, PostException {

        List<Post> posts = new ArrayList<>();
        switch (role) {
            case PATIENT:
                posts.addAll(postRepository.findAllPostsForPatients(userIds));
                break;
            case DOCTOR:
                posts.addAll(postRepository.findAllPostsForDoctors(userIds));
                break;
            default:
                break;
        }
            return posts;
    }

    @Override
    public Post findPostByRole(Long postId, ERole role) throws UserException, PostException {
        Optional<Post> optionalPost = postRepository.findPostByRole(postId, role);
        return optionalPost.orElseThrow(() -> new PostException("Post not found for role: " + role));
    }
    

}
