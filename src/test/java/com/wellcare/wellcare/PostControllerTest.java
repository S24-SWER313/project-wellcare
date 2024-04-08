package com.wellcare.wellcare;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wellcare.wellcare.Controllers.PostController;
import com.wellcare.wellcare.Exceptions.ResourceNotFoundException;
import com.wellcare.wellcare.Exceptions.UserException;
import com.wellcare.wellcare.Models.Post;
import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Repositories.PostRepository;
import com.wellcare.wellcare.Repositories.UserRepository;
import com.wellcare.wellcare.Security.jwt.AuthTokenFilter;
import com.wellcare.wellcare.Security.jwt.JwtUtils;
import com.wellcare.wellcare.Storage.StorageException;
import com.wellcare.wellcare.Storage.StorageService;
import static org.junit.jupiter.api.Assertions.assertThrows;


import jakarta.servlet.http.HttpServletRequest;

@SpringBootTest
@AutoConfigureMockMvc
public class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PostRepository postRepository;

    @Autowired
    private PostController postController;

    @MockBean
    private AuthTokenFilter authTokenFilter;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtUtils jwtUtils;

    
    @MockBean
    private StorageService storageService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        
    }

    // Common setup for parsing JWT token and retrieving user
    private void setupCommonMocking(HttpServletRequest request, String jwtToken, Long userId, User user) {
        when(authTokenFilter.parseJwt(request)).thenReturn(jwtToken);
        when(jwtUtils.getUserIdFromJwtToken(jwtToken)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.ofNullable(user));
    }

    @Test
    @WithMockUser(username = "testUser", roles = { "DOCTOR" })
    public void testCreatePost() throws Exception {
        // Mock data
        Post post = new Post();
        post.setContent("Test content");
        post.setCreatedAt(LocalDateTime.now());

        // Mocking behavior
        when(postRepository.save(any(Post.class))).thenReturn(post);

        // Perform the request
        mockMvc.perform(MockMvcRequestBuilders.post("/api/posts/new-post")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .param("content", "Test content")
                .param("file", "test.jpg"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testCreatePost_InvalidJwtToken() throws Exception {
        // Mock request and token
        HttpServletRequest request = mock(HttpServletRequest.class);
        String invalidJwtToken = "invalid_jwt_token";
        when(authTokenFilter.parseJwt(request)).thenReturn(invalidJwtToken);

        // Perform the request and expect UserException
        assertThrows(UserException.class, () -> {
            postController.createPost(request, new Post(), new MultipartFile[0], null);
        });
    }

    @Test
    public void testCreatePost_UserNotFound() throws Exception {
        // Mock request and token
        HttpServletRequest request = mock(HttpServletRequest.class);
        String validJwtToken = "valid_jwt_token";
        when(authTokenFilter.parseJwt(request)).thenReturn(validJwtToken);

        // Mocking behavior for token parsing and user retrieval
        Long userId = 1L;
        setupCommonMocking(request, validJwtToken, userId, null);

        // Perform the request and expect UserException
        assertThrows(UserException.class, () -> {
            postController.createPost(request, new Post(), new MultipartFile[0], null);
        });
    }

   
    @Test
    public void testUpdatePost_Success() throws Exception {
       // Mock data
       Long postId = 1L;
       Post existingPost = new Post();
       existingPost.setId(postId);
       existingPost.setContent("Existing content");
       existingPost.setLocation("Existing location");
    
       Post updatedPost = new Post();
       updatedPost.setContent("Updated content");
    
       // Mocking behavior
       when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
       when(postRepository.save(any(Post.class))).thenReturn(updatedPost);
    
       // Perform the request and verify the response
       mockMvc.perform(MockMvcRequestBuilders.put("/api/posts/{postId}", postId)
               .contentType(MediaType.APPLICATION_JSON)
               .content(asJsonString(updatedPost)))
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

   

    private String asJsonString(final Object obj) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}