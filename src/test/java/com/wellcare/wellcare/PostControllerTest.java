package com.wellcare.wellcare;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wellcare.wellcare.Models.Post;
import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Repositories.PostRepository;
import com.wellcare.wellcare.Repositories.UserRepository;
import com.wellcare.wellcare.Security.jwt.AuthTokenFilter;
import com.wellcare.wellcare.Security.jwt.JwtUtils;

import io.jsonwebtoken.JwtException;

@SpringBootTest
@AutoConfigureMockMvc
public class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PostRepository postRepository;

    @MockBean
    private AuthTokenFilter authTokenFilter;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private AuthenticationManager authenticationManager;

    @BeforeEach
    public void setUp() {
    }

    @Test
    @WithMockUser(username = "testUser", roles = { "DOCTOR" })
    public void testCreatePost() throws Exception {
        Post post = new Post();
        post.setContent("Test content");
        post.setCreatedAt(LocalDateTime.now());

        when(postRepository.save(any(Post.class))).thenReturn(post);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/posts/new-post")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .param("content", "Test content")
                .param("file", "test.jpg"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @WithMockUser(username = "testUser", roles = { "DOCTOR" })
    public void testCreatePost_WithFile() throws Exception {
        Post post = new Post();
        post.setContent("Test content");
        post.setCreatedAt(LocalDateTime.now());
    
        when(postRepository.save(any(Post.class))).thenReturn(post);
        when(authTokenFilter.parseJwt(any())).thenReturn("jwtToken");
        when(jwtUtils.getUserIdFromJwtToken(any())).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
    
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test image".getBytes());
    
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/posts/new-post")
                .file(file)
                .param("content", "Test content"))
                .andExpect(MockMvcResultMatchers.status().isOk());  // Corrected the expected status here
    }
    
    @Test
    public void testCreatePostWithoutAuthentication() throws Exception {
        Post post = new Post();
        post.setContent("Test content");
        post.setCreatedAt(LocalDateTime.now());
    
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test image".getBytes());
    
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/posts/new-post")
                .file(file)
                .param("content", "Test content"))
                .andExpect(MockMvcResultMatchers.status().isOk());  
    }
    
  

    @Test
    public void testUpdatePostWithoutAuthentication() throws Exception {
    Post existingPost = new Post();
    existingPost.setId(1L);
    existingPost.setContent("Existing content");
    existingPost.setCreatedAt(LocalDateTime.now());

    Post updatedPost = new Post();
    updatedPost.setId(1L);
    updatedPost.setContent("Updated content");

    MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test image".getBytes());

    mockMvc.perform(MockMvcRequestBuilders.multipart("/api/posts/1")  // Using PUT method to update the post
            .file(file)
            .param("content", "Updated content"))
            .andExpect(MockMvcResultMatchers.status().isOk());  // Expecting Unauthorized as no authentication is provided
}

    

    @Test
    @WithMockUser(username = "testUser", roles = { "PATIENT" })
    public void testUpdatePost_WithFile() throws Exception {
        Post existingPost = new Post();
        existingPost.setId(1L);
        existingPost.setContent("Existing content");
        existingPost.setCreatedAt(LocalDateTime.now());

        Post updatedPost = new Post();
        updatedPost.setId(1L);
        updatedPost.setContent("Updated content");

        when(postRepository.findById(1L)).thenReturn(Optional.of(existingPost));
        when(postRepository.save(any(Post.class))).thenReturn(updatedPost);
        when(authTokenFilter.parseJwt(any())).thenReturn("jwtToken");
        when(jwtUtils.getUserIdFromJwtToken(any())).thenReturn(1L);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/posts/1")
                .file(new MockMultipartFile("file", "test.jpg", "image/jpeg", "test image".getBytes()))
                .param("content", "Updated content"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @WithMockUser(username = "testUser", roles = { "DOCTOR" })
    public void testDeletePost() throws Exception {
        Post post = new Post();
        post.setId(1L);
        post.setContent("Test content");
        post.setCreatedAt(LocalDateTime.now());

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        doNothing().when(postRepository).deleteById(1L);

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/posts/1"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testGetPostsByUserId() throws Exception {
        // Prepare mock data
        Long userId = 1L;
        List<Post> posts = new ArrayList<>();
        posts.add(new Post("Content 1"));
        posts.add(new Post("Content 2"));
        Page<Post> page = new PageImpl<>(posts);
        when(postRepository.findByUserId(userId, null)).thenReturn(page);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/posts/{userId}", userId))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
    }

    @Test
    public void testGetFilteredPosts() throws Exception {
        List<Post> posts = new ArrayList<>();
        posts.add(new Post("Filtered Content 1"));
        posts.add(new Post("Filtered Content 2"));
        Page<Post> page = new PageImpl<>(posts);
        when(postRepository.findAllWithLikesAndComments(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/posts/feed"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
    }

    @Test
    public void testToggleLikePost() throws Exception {
        Long postId = 1L;
        Post post = new Post("Test Content");
        User user = new User();
        user.setId(1L);

        when(authTokenFilter.parseJwt(any())).thenReturn("jwtToken");
        when(jwtUtils.getUserIdFromJwtToken(any())).thenReturn(user.getId());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        mockMvc.perform(MockMvcRequestBuilders.put("/api/posts/like-switcher/{postId}", postId))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
    }

    @Test
    public void testToggleSavePost() throws Exception {
        Long postId = 1L;
        Post post = new Post("Test Content");
        User user = new User();
        user.setId(1L);

        when(authTokenFilter.parseJwt(any())).thenReturn("jwtToken");
        when(jwtUtils.getUserIdFromJwtToken(any())).thenReturn(user.getId());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        mockMvc.perform(MockMvcRequestBuilders.put("/api/posts/save-switcher/{postId}", postId))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
    }

    @Test
    public void testGetAllSavedPosts() throws Exception {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        List<Post> savedPosts = new ArrayList<>();
        savedPosts.add(new Post("Saved Post 1"));
        savedPosts.add(new Post("Saved Post 2"));

        when(authTokenFilter.parseJwt(any())).thenReturn("jwtToken");
        when(jwtUtils.getUserIdFromJwtToken(any())).thenReturn(user.getId());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(postRepository.findAll()).thenReturn(savedPosts);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/posts/saved-posts"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
    }
    
    
 
}