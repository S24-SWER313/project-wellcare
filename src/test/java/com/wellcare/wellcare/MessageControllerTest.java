package com.wellcare.wellcare;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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

        MockMultipartFile file = new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE,
                "Test file content".getBytes());

        // Mocking behavior
        mockCommonBehaviors();

        // Perform the request
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/messages/new-message/2")
                .file(file)
                .param("content", "Test message content")
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    private void mockCommonBehaviors() {
        when(authTokenFilter.parseJwt(any(HttpServletRequest.class))).thenReturn("mockedJwtToken");
        when(jwtUtils.getUserIdFromJwtToken(anyString())).thenReturn(1L);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(new User()));
    }

    @Test
    public void testUpdateMessage() throws Exception {
        // Mocking behavior
        mockCommonBehaviors();
        when(messageRepository.findById(anyLong())).thenReturn(Optional.of(new Message()));

        // Perform the request
        mockMvc.perform(MockMvcRequestBuilders.put("/api/messages/1")
                .param("content", "Updated test message content")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testGetRecentConversations() throws Exception {
        // Mocking behavior
        mockCommonBehaviors();
        when(messageRepository.findRecentConversations(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(new ArrayList<>()));

        // Perform the request
        mockMvc.perform(MockMvcRequestBuilders.get("/api/messages/recent"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testGetMessagesWithUser() throws Exception {
        // Mocking behavior
        mockCommonBehaviors();
        when(messageRepository.findAllMessagesBetweenTwoUsers(anyLong(), anyLong()))
                .thenReturn(new ArrayList<Message>());

        // Perform the request
        mockMvc.perform(MockMvcRequestBuilders.get("/api/messages/chat/2"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testGetUnreadMessages() throws Exception {
        // Mocking behavior
        mockCommonBehaviors();
        when(messageRepository.getAllUnreadMessages(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(new ArrayList<>()));

        // Perform the request
        mockMvc.perform(MockMvcRequestBuilders.get("/api/messages/unread"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testSendMessageInvalidUser() throws Exception {
        // Mocking behavior
        mockCommonBehaviors();
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Perform the request
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/messages/new-message/2")
                .file(new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE,
                        "Test file content".getBytes()))
                .param("content", "Test message content")
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testUpdateMessageInvalidMessage() throws Exception {
        // Mocking behavior
        mockCommonBehaviors();
        when(messageRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Perform the request
        mockMvc.perform(MockMvcRequestBuilders.put("/api/messages/2")
                .param("content", "Updated test message content")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testGetRecentConversationsNoData() throws Exception {
        // Mocking behavior
        mockCommonBehaviors();
        when(messageRepository.findRecentConversations(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        // Perform the request
        mockMvc.perform(MockMvcRequestBuilders.get("/api/messages/recent"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testGetUnreadMessagesNoData() throws Exception {
        // Mocking behavior
        mockCommonBehaviors();
        when(messageRepository.getAllUnreadMessages(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        // Perform the request
        mockMvc.perform(MockMvcRequestBuilders.get("/api/messages/unread"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}