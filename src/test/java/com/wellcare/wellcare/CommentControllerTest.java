package com.wellcare.wellcare;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.mock.web.MockMultipartFile;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wellcare.wellcare.Controllers.CommentController;
import com.wellcare.wellcare.Exceptions.CommentException;
import com.wellcare.wellcare.Exceptions.ResourceNotFoundException;
import com.wellcare.wellcare.Exceptions.UserException;
import com.wellcare.wellcare.Models.Comment;
import com.wellcare.wellcare.Models.Post;
import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Repositories.CommentRepository;
import com.wellcare.wellcare.Repositories.PostRepository;
import com.wellcare.wellcare.Repositories.UserRepository;
import com.wellcare.wellcare.Security.jwt.AuthTokenFilter;
import com.wellcare.wellcare.Security.jwt.JwtUtils;

import jakarta.servlet.http.HttpServletRequest;

@SpringBootTest
@AutoConfigureMockMvc
public class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CommentController commentController;

    @MockBean
    private PostRepository postRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private AuthTokenFilter authTokenFilter;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private CommentRepository commentRepository;

   
        @BeforeEach
        public void setUp() {
            MockitoAnnotations.openMocks(this);
            this.mockMvc = MockMvcBuilders.standaloneSetup(commentController).build();
        }

        @Test
        @WithMockUser(username = "testUser", roles = { "DOCTOR" })
        public void testCreateCommentWithFile() throws Exception {
            // Mock data
            Long postId = 1L;
            Comment comment = new Comment();
            comment.setContent("Test content");
        
            // Mock repository behavior
            when(authTokenFilter.parseJwt(any(HttpServletRequest.class))).thenReturn("testToken");
            when(jwtUtils.getUserIdFromJwtToken("testToken")).thenReturn(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
            when(postRepository.findById(postId)).thenReturn(Optional.of(new Post()));
            when(commentRepository.save(any(Comment.class))).thenReturn(comment);
        
            // Create a mock file
            MockMultipartFile file = new MockMultipartFile("file", "testFile.txt", "text/plain", "Test file content".getBytes());
        
            // Perform the request with file attachment
            mockMvc.perform((MockHttpServletRequestBuilder) multipart("/api/comments/" + postId)
                    .file(file)
                    .param("content", comment.getContent()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").value("Test content"));
        }
    
        @Test
@WithMockUser(username = "testUser", roles = { "DOCTOR" })
public void testCreateCommentWithoutFile() throws Exception {
    // Mock data
    Long postId = 1L;
    Comment comment = new Comment();
    comment.setContent("Test content");

    // Mock repository behavior
    when(authTokenFilter.parseJwt(any(HttpServletRequest.class))).thenReturn("testToken");
    when(jwtUtils.getUserIdFromJwtToken("testToken")).thenReturn(1L);
    when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
    when(postRepository.findById(postId)).thenReturn(Optional.of(new Post()));
    when(commentRepository.save(any(Comment.class))).thenReturn(comment);

    // Perform the request without file attachment
    mockMvc.perform(post("/api/comments/{postId}", postId)
            .param("content", comment.getContent()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value("Test content"));
}

    @Test
    public void testCreateComment_InvalidCommentData() throws Exception {
        Long userId = 1L;
        Long postId = 1L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(postRepository.findById(postId)).thenReturn(Optional.of(new Post()));

        MockHttpServletRequestBuilder request = post("/api/comments/{postId}", postId)
                .param("content", "") // Invalid empty content
                .contentType(MediaType.MULTIPART_FORM_DATA);

        mockMvc.perform(request)
                .andExpect(status().isBadRequest());
    }



    @Test
    @WithMockUser(username = "testUser", roles = { "DOCTOR" })
    public void testUpdateComment() throws Exception {
        Comment existingComment = new Comment();
        existingComment.setId(1L);
        existingComment.setContent("Existing content");
        existingComment.setCreatedAt(LocalDateTime.now());

        Comment updatedComment = new Comment();
        updatedComment.setId(1L);
        updatedComment.setContent("Updated content");

        when(commentRepository.findById(1L)).thenReturn(Optional.of(existingComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(updatedComment);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/comments/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(updatedComment)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
@WithMockUser(username = "testUser", roles = { "DOCTOR" })
public void testUpdateCommentWithAttachment() throws Exception {
    Comment existingComment = new Comment();
    existingComment.setId(1L);
    existingComment.setContent("Existing content");
    existingComment.setCreatedAt(LocalDateTime.now());

    Comment updatedComment = new Comment();
    updatedComment.setId(1L);
    updatedComment.setContent("Updated content");
    updatedComment.setAttachment("http://example.com/image.jpg"); // Set attachment

    when(commentRepository.findById(1L)).thenReturn(Optional.of(existingComment));
    when(commentRepository.save(any(Comment.class))).thenReturn(updatedComment);

    mockMvc.perform(MockMvcRequestBuilders.put("/api/comments/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(updatedComment)))
            .andExpect(MockMvcResultMatchers.status().isOk());
}

@Test
@WithMockUser(username = "testUser", roles = { "DOCTOR" })
public void testUpdateNonExistentComment() throws Exception {
    when(commentRepository.findById(1L)).thenReturn(Optional.empty());

    Comment updatedComment = new Comment();
    updatedComment.setId(1L);
    updatedComment.setContent("Updated content");

    mockMvc.perform(MockMvcRequestBuilders.put("/api/comments/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(updatedComment)))
            .andExpect(MockMvcResultMatchers.status().isNotFound()); // Expect 404 Not Found
}

    @Test
    void updateComment_WithNonExistingComment_ShouldThrowResourceNotFoundException() {
        Long commentId = 1L;
        Comment updatedComment = new Comment();
        updatedComment.setContent("Updated content");
        MultipartFile file = null;
    
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());
    
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> commentController.updateComment(commentId, updatedComment, file));
    
        assertEquals("Resource of type 'Comment' with ID '1' not found", exception.getMessage());
    }

    
    @Test
@WithMockUser(username = "testUser", roles = { "DOCTOR" })
public void testDeleteComment_Success() throws Exception {
    Long commentId = 1L;
    Comment comment = new Comment();
    comment.setId(commentId);
    comment.setContent("Test content");
    User user = new User();
    user.setId(1L);
    comment.setUser(user);

    when(authTokenFilter.parseJwt(any(HttpServletRequest.class))).thenReturn("testToken");
    when(jwtUtils.getUserIdFromJwtToken("testToken")).thenReturn(1L); // Assuming the user ID extracted from JWT is 1L
    when(userRepository.findById(1L)).thenReturn(Optional.of(user)); // Mocking the user repository to return the user
    when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
    when(postRepository.findPostByCommentId(commentId)).thenReturn(Optional.of(new Post()));

    mockMvc.perform(delete("/api/comments/{commentId}", commentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Comment deleted successfully!"));

    verify(commentRepository, times(1)).deleteById(commentId);
}

@Test
@WithMockUser(username = "testUser", roles = { "DOCTOR" })
public void testDeleteNonexistentComment() throws Exception {
    Long commentId = 1L;
    when(authTokenFilter.parseJwt(any(HttpServletRequest.class))).thenReturn("testToken");
    when(jwtUtils.getUserIdFromJwtToken("testToken")).thenReturn(1L); // Assuming the user ID extracted from JWT is 1L
    when(userRepository.findById(1L)).thenReturn(Optional.of(new User())); // Assuming user ID 1L exists
    when(commentRepository.findById(commentId)).thenReturn(Optional.empty()); // Mocking the comment repository to return empty optional

    mockMvc.perform(delete("/api/comments/{commentId}", commentId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Comment not found"));
}

@Test
@WithMockUser(username = "testUser", roles = { "DOCTOR" })
public void testDeleteComment_Unauthorized() throws Exception {
    // Mock data
    Long commentId = 1L;
    Comment comment = new Comment();
    comment.setId(commentId);
    comment.setContent("Test content");
    User user = new User();
    user.setId(1L); 
    comment.setUser(user);

    // Mock repository behavior
    when(authTokenFilter.parseJwt(any(HttpServletRequest.class))).thenReturn("testToken");
    when(jwtUtils.getUserIdFromJwtToken("testToken")).thenReturn(2L); // Assuming the user ID extracted from JWT is 2L
    when(userRepository.findById(2L)).thenReturn(Optional.of(user)); // Mocking the user repository to return the user
    when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

    // Perform the request
    mockMvc.perform(delete("/api/comments/{commentId}", commentId))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Unauthorized!"));
}


@Test
@Order(1)
@WithMockUser(username = "testUser", roles = { "DOCTOR" })
public void testToggleLikeComment_LikeAdded() throws Exception {
    Long commentId = 1L;
    Comment comment = new Comment();
    comment.setId(commentId);
    User user = new User();
    user.setId(1L);
    comment.setUser(user);
    Set<User> likes = new HashSet<>();
    comment.setCommentLikes(likes);

    when(authTokenFilter.parseJwt(any(HttpServletRequest.class))).thenReturn("testToken");
    when(jwtUtils.getUserIdFromJwtToken("testToken")).thenReturn(1L);
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
    when(commentRepository.save(any(Comment.class))).thenReturn(comment);

    mockMvc.perform(put("/api/comments/like-switcher/{commentId}", commentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.noOfLikes").value(1)); // Assuming like count is properly updated
}

@Test
@Order(2)
@WithMockUser(username = "testUser", roles = { "DOCTOR" })
public void testToggleLikeComment_LikeRemoved() throws Exception {
    // Mock data
    Long commentId = 1L;
    Comment comment = new Comment();
    comment.setId(commentId);
    User user = new User();
    user.setId(1L);
    comment.setUser(user);
    Set<User> likes = new HashSet<>();
    likes.add(user); // User has already liked the comment
    comment.setCommentLikes(likes);

    // Mock repository behavior
    when(authTokenFilter.parseJwt(any(HttpServletRequest.class))).thenReturn("testToken");
    when(jwtUtils.getUserIdFromJwtToken("testToken")).thenReturn(1L);
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
    when(commentRepository.save(any(Comment.class))).thenReturn(comment);

    // Perform the request
    mockMvc.perform(put("/api/comments/like-switcher/{commentId}", commentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.noOfLikes").value(0)); // Assuming like count is properly updated
}

@Test
@WithMockUser(username = "testUser", roles = { "DOCTOR" })
public void testToggleLikeComment_UserNotFound() throws Exception {
    // Mock data
    Long commentId = 1L;
    Comment comment = new Comment();
    comment.setId(commentId);

    // Mock repository behavior
    when(authTokenFilter.parseJwt(any(HttpServletRequest.class))).thenReturn("testToken");
    when(jwtUtils.getUserIdFromJwtToken("testToken")).thenReturn(1L);
    when(userRepository.findById(1L)).thenReturn(Optional.empty()); // Simulating user not found
    when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

    // Perform the request
    mockMvc.perform(put("/api/comments/like-switcher/{commentId}", commentId))
            .andExpect(status().isUnauthorized());
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
