package com.wellcare.wellcare;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.wellcare.wellcare.Controllers.MessageController;
import com.wellcare.wellcare.Models.Message;
import com.wellcare.wellcare.Models.Relationship;
import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Repositories.MessageRepository;
import com.wellcare.wellcare.Repositories.RelationshipRepository;
import com.wellcare.wellcare.Repositories.UserRepository;
import com.wellcare.wellcare.Security.jwt.AuthTokenFilter;
import com.wellcare.wellcare.Security.jwt.JwtUtils;
import com.wellcare.wellcare.Storage.StorageService;

import jakarta.servlet.http.HttpServletRequest;

@SpringBootTest
@AutoConfigureMockMvc
public class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MessageRepository messageRepository;

    @MockBean
    private RelationshipRepository relationshipRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private AuthTokenFilter authTokenFilter;

    @MockBean
    private StorageService storageService;

    @InjectMocks
    private MessageController messageController;

    @Test
    public void testSendMessage() throws Exception {
        // Mock data
        Message message = new Message();
        message.setContent("Test message content");

        MockMultipartFile file = new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "Test file content".getBytes());

        // Mocking behavior
        when(authTokenFilter.parseJwt(any(HttpServletRequest.class))).thenReturn("mockedJwtToken");
        when(jwtUtils.getUserIdFromJwtToken(anyString())).thenReturn(1L);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(new User())); // Mock loggedInUser
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(new User())); // Mock fromUser
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(new User())); // Mock toUser
        when(relationshipRepository.findRelationshipByUserOneIdAndUserTwoId(anyLong(), anyLong())).thenReturn(new Relationship());

        // Perform the request
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/message/sending/2")
            .file(file)
            .param("content", "Test message content")
            .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testGetAllMessagesWithUser() throws Exception {
        // Mocking behavior
        when(authTokenFilter.parseJwt(any(HttpServletRequest.class))).thenReturn("mockedJwtToken");
        when(jwtUtils.getUserIdFromJwtToken(anyString())).thenReturn(1L);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(new User())); // Mock loggedInUser
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(new User())); // Mock chatUser
        when(messageRepository.findAllMessagesBetweenTwoUsers(anyLong(), anyLong())).thenReturn(new ArrayList<Message>());

        // Perform the request
        mockMvc.perform(MockMvcRequestBuilders.get("/api/message/2"))
            .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testGetAllFriendMessages() throws Exception {
        // Mocking behavior
        when(authTokenFilter.parseJwt(any(HttpServletRequest.class))).thenReturn("mockedJwtToken");
        when(jwtUtils.getUserIdFromJwtToken(anyString())).thenReturn(1L);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(new User())); // Mock loggedInUser
        when(messageRepository.getAllUnreadMessages(anyLong())).thenReturn(new ArrayList<Message>());

        // Perform the request
        mockMvc.perform(MockMvcRequestBuilders.get("/api/message/unread"))
            .andExpect(MockMvcResultMatchers.status().isOk());
    }
}
