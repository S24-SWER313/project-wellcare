package com.wellcare.wellcare;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wellcare.wellcare.Models.Post;
import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Repositories.CommentRepository;
import com.wellcare.wellcare.Repositories.PostRepository;
import com.wellcare.wellcare.Security.jwt.AuthTokenFilter;
import com.wellcare.wellcare.Security.jwt.JwtUtils;

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
    private JwtUtils jwtUtils;

    @MockBean
    private CommentRepository commentRepository;

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
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(post)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testGetPostsByUserId() throws Exception {
        User user = new User();
        user.setId(2L); // Assuming the user ID is 2

        Post post1 = new Post();
        post1.setId(1L); // Assuming the post ID is 1
        post1.setContent("Test content 1");
        post1.setUser(user);

        Post post2 = new Post();
        post2.setId(2L); // Assuming the post ID is 2
        post2.setContent("Test content 2");
        post2.setUser(user);

        List<Post> userPosts = Arrays.asList(post1, post2);

        when(postRepository.findByUserIdWithAttachment(anyLong())).thenReturn(userPosts);

        // Perform the request and print the JSON response for debugging
        String jsonResponse = mockMvc.perform(MockMvcRequestBuilders.get("/api/posts/{userId}", 2L)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        System.out.println("JSON Response: " + jsonResponse);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/posts/{userId}", 2L)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(userPosts.size()));
    }

    @Test
    public void testDeletePost() throws Exception {
        Long postId = 1L;

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/posts/{postId}", postId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testGetFilteredPosts() throws Exception {
        // Mock user role
        User user = new User();
        user.setId(1L);

        Post post1 = new Post();
        post1.setContent("Test content 1");
        post1.setUser(user);

        Post post2 = new Post();
        post2.setContent("Test content 2");
        post2.setUser(user);

        List<Post> filteredPosts = Arrays.asList(post1, post2);

        when(postRepository.findAll()).thenReturn(filteredPosts);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/posts/feed")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(filteredPosts.size()));
    }

    @Test
    public void testToggleLikePost() throws Exception {
        Long postId = 1L;

        mockMvc.perform(MockMvcRequestBuilders.put("/api/posts/like-switcher/{postId}", postId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testToggleSavePost() throws Exception {
        Long postId = 1L;

        mockMvc.perform(MockMvcRequestBuilders.put("/api/posts/save-switcher/{postId}", postId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testGetAllSavedPosts() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/posts/saved-posts")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    // Helper method to convert object to JSON string
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
