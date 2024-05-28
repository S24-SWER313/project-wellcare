package com.wellcare.wellcare;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wellcare.wellcare.Models.Relationship;
import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Repositories.RelationshipRepository;
import com.wellcare.wellcare.Repositories.UserRepository;
import com.wellcare.wellcare.Security.jwt.AuthTokenFilter;
import com.wellcare.wellcare.Security.jwt.JwtUtils;

@SpringBootTest
@AutoConfigureMockMvc
public class RelationshipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthTokenFilter authTokenFilter;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private RelationshipRepository relationshipRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testAddFriend() throws Exception {
        when(authTokenFilter.parseJwt(any())).thenReturn("mockedJwtToken");
        when(jwtUtils.getUserIdFromJwtToken("mockedJwtToken")).thenReturn(1L);

        User loggedInUser = new User();
        loggedInUser.setId(1L);
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(loggedInUser));

        User friendUser = new User();
        friendUser.setId(2L);
        when(userRepository.findById(2L)).thenReturn(java.util.Optional.of(friendUser));
        when(relationshipRepository.findRelationshipByUserOneUsernameAndUserTwoUsername("testuser1", "testuser2")).thenReturn(null);

        // Data to be sent in the request body
        Map<String, String> friendData = new HashMap<>();
        friendData.put("specialty", "Cardiologist");
        friendData.put("degree", "MD");

        // Performing the PUT request to add a friend
        mockMvc.perform(put("/api/relationship/new-friend/{friendUserId}", 2L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(friendData)))
                .andExpect(status().isOk());

    }
  
    @Test
    public void testAddFriendSelf() throws Exception {
        when(authTokenFilter.parseJwt(any())).thenReturn("mockedJwtToken");
        when(jwtUtils.getUserIdFromJwtToken("mockedJwtToken")).thenReturn(1L);

        mockMvc.perform(put("/api/relationship/new-friend/{friendUserId}", 1L)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }  

    @Test
    public void testAddFriendRelationshipExists() throws Exception {
        when(authTokenFilter.parseJwt(any())).thenReturn("mockedJwtToken");
        when(jwtUtils.getUserIdFromJwtToken("mockedJwtToken")).thenReturn(1L);

        User loggedInUser = new User();
        loggedInUser.setId(1L);
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(loggedInUser));

        User friendUser = new User();
        friendUser.setId(2L);
        when(userRepository.findById(2L)).thenReturn(java.util.Optional.of(friendUser));

        Relationship existingRelationship = new Relationship();
        existingRelationship.setStatus(1);
        when(relationshipRepository.findRelationshipByUserOneUsernameAndUserTwoUsername(loggedInUser.getUsername(), friendUser.getUsername())).thenReturn(existingRelationship);

        mockMvc.perform(put("/api/relationship/new-friend/{friendUserId}", 2L)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
  
    @Test
    public void testGetFriendRequests() throws Exception {
        String jwtToken = "mockedJwtToken";
        Long userId = 1L;
    
        // Mock JWT token and user ID
        when(authTokenFilter.parseJwt(any())).thenReturn(jwtToken);
        when(jwtUtils.getUserIdFromJwtToken(jwtToken)).thenReturn(userId);
    
        User user = new User();
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    
        Relationship relationship = new Relationship();
        relationship.setId(1L);
        relationship.setUserOne(user);
        relationship.setUserTwo(user);
        relationship.setStatus(0);
        List<Relationship> friendRequests = Collections.singletonList(relationship);
    
        when(relationshipRepository.findRelationshipsByUserTwoIdAndStatus(userId, 0)).thenReturn(friendRequests);
    
        mockMvc.perform(get("/api/relationship/friend-requests")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());
                
    }
    

   
    @Test
    public void testAddFriendAlreadySent() throws Exception {
        when(authTokenFilter.parseJwt(any())).thenReturn("mockedJwtToken");
        when(jwtUtils.getUserIdFromJwtToken("mockedJwtToken")).thenReturn(1L);

        User loggedInUser = new User();
        loggedInUser.setId(1L);
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(loggedInUser));

        User friendUser = new User();
        friendUser.setId(2L);
        when(userRepository.findById(2L)).thenReturn(java.util.Optional.of(friendUser));

        Relationship existingRelationship = new Relationship();
        existingRelationship.setStatus(0);
        when(relationshipRepository.findRelationshipByUserOneUsernameAndUserTwoUsername("testuser1", "testuser2")).thenReturn(existingRelationship);

        mockMvc.perform(put("/api/relationship/new-friend/{friendUserId}", 2L)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

 
 

    @Test
    public void testRemoveFriend() throws Exception {
        when(authTokenFilter.parseJwt(any())).thenReturn("mockedJwtToken");
        when(jwtUtils.getUserIdFromJwtToken("mockedJwtToken")).thenReturn(1L);

        User loggedInUser = new User();
        loggedInUser.setId(1L);
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(loggedInUser));

        User friendUser = new User();
        friendUser.setId(2L);
        when(userRepository.findById(2L)).thenReturn(java.util.Optional.of(friendUser));

        Relationship relationship = new Relationship();
        relationship.setId(1L);
        relationship.setUserOne(loggedInUser);
        relationship.setUserTwo(friendUser);
        relationship.setStatus(0);
        relationship.setTime(LocalDateTime.now());

        when(relationshipRepository.findRelationshipByUserOneUsernameAndUserTwoUsername("testuser1", "testuser2")).thenReturn(relationship);

        mockMvc.perform(delete("/api/relationship/friends/{friendUserId}", 2L)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testAcceptFriend() throws Exception {
        when(authTokenFilter.parseJwt(any())).thenReturn("mockedJwtToken");
        when(jwtUtils.getUserIdFromJwtToken("mockedJwtToken")).thenReturn(1L);

        User loggedInUser = new User();
        loggedInUser.setId(1L);
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(loggedInUser));

        User friendUser = new User();
        friendUser.setId(2L);
        when(userRepository.findById(2L)).thenReturn(java.util.Optional.of(friendUser));

        Relationship relationship = new Relationship();
        relationship.setId(1L);
        relationship.setUserOne(friendUser);
        relationship.setUserTwo(loggedInUser);
        relationship.setStatus(0);
        relationship.setTime(LocalDateTime.now());

        when(relationshipRepository.findRelationshipByUserOneUsernameAndUserTwoUsername("testuser1", "testuser2")).thenReturn(relationship);

        mockMvc.perform(put("/api/relationship/friend-accept/{friendUserId}", 2L)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testCancelFriendshipRequest() throws Exception {
        when(authTokenFilter.parseJwt(any())).thenReturn("mockedJwtToken");
        when(jwtUtils.getUserIdFromJwtToken("mockedJwtToken")).thenReturn(1L);

        User loggedInUser = new User();
        loggedInUser.setId(1L);
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(loggedInUser));

        User friendUser = new User();
        friendUser.setId(2L);
        when(userRepository.findById(2L)).thenReturn(java.util.Optional.of(friendUser));

        Relationship relationship = new Relationship();
        relationship.setId(1L);
        relationship.setUserOne(friendUser);
        relationship.setUserTwo(loggedInUser);
        relationship.setStatus(0);
        relationship.setTime(LocalDateTime.now());

        when(relationshipRepository.findRelationshipByUserOneUsernameAndUserTwoUsername("testuser1", "testuser2")).thenReturn(relationship);

        mockMvc.perform(put("/api/relationship/friend-request-cancel/{friendUserId}", 2L)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

 

}