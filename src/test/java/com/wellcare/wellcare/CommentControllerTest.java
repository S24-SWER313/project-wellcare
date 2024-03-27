package com.wellcare.wellcare;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
import com.wellcare.wellcare.Models.Comment;
import com.wellcare.wellcare.Models.Post;
import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Repositories.CommentRepository;
import com.wellcare.wellcare.Repositories.PostRepository;
import com.wellcare.wellcare.Security.jwt.AuthTokenFilter;
import com.wellcare.wellcare.Security.jwt.JwtUtils;

@SpringBootTest
@AutoConfigureMockMvc
public class CommentControllerTest {

    
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
    public void testCreateComment() throws Exception {
        Comment comment = new Comment();
        comment.setContent("Test content");
        comment.setCreatedAt(LocalDateTime.now());
    
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/comments/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(comment)))
        .andExpect(MockMvcResultMatchers.status().isOk());
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
public void testDeleteComment() throws Exception {
    Comment comment = new Comment();
    comment.setId(1L);
    comment.setContent("Test content");
    comment.setCreatedAt(LocalDateTime.now());

    when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
    doNothing().when(commentRepository).deleteById(1L);

    mockMvc.perform(MockMvcRequestBuilders.delete("/api/comments/1"))
            .andExpect(MockMvcResultMatchers.status().isOk());
}

@Test
@WithMockUser(username = "testUser", roles = { "DOCTOR" })
public void testToggleLikeComment() throws Exception {
    Comment comment = new Comment();
    comment.setId(1L);
    comment.setContent("Test content");
    comment.setCreatedAt(LocalDateTime.now());
    comment.setNoOfLikes(0);

    when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
    when(commentRepository.save(any(Comment.class))).thenReturn(comment);

    mockMvc.perform(MockMvcRequestBuilders.put("/api/comments/like-switcher/1"))
            .andExpect(MockMvcResultMatchers.status().isOk());
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
