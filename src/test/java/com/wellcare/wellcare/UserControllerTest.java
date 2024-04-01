package com.wellcare.wellcare;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wellcare.wellcare.Models.ERole;
import com.wellcare.wellcare.Models.Role;
import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Repositories.UserRepository;
import com.wellcare.wellcare.Security.jwt.JwtUtils;
import com.wellcare.wellcare.Security.services.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtUtils jwtUtils;

    @BeforeEach
    public void setUp() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testUser");
        user.setPassword("testPassword");
        user.setName("Test User");
        user.setEmail("test@example.com");

        Role role = new Role(ERole.PATIENT);
        user.setRole(role);

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName().toString()));

        UserDetails userDetails = new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                authorities);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtUtils.getUserNameFromJwtToken(anyString())).thenReturn(user.getUsername());
        when(jwtUtils.getUserIdFromJwtToken(anyString())).thenReturn(user.getId());

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @WithMockUser(username = "testUser", password = "testPassword")
    public void testGetUserProfile() throws Exception {
        mockMvc.perform(get("/api/users/profile/{userId}", 1L)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testUser"))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @WithMockUser(username = "testUser", password = "testPassword")
    public void testUpdateUserProfile() throws Exception {
        User updatedUser = new User();
        updatedUser.setName("Updated User");
        updatedUser.setEmail("updated@example.com");

        mockMvc.perform(put("/api/users/profile/{userId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User profile updated successfully"));

        verify(userRepository, times(1)).save(any(User.class));
    }
    
    @Test
    @WithMockUser(username = "testUser", password = "testPassword")
    public void testUpdateUserPassword() throws Exception {
        mockMvc.perform(put("/api/users/profile/{userId}/password", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\": \"newPassword123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password updated successfully"));

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
@WithMockUser(username = "testUser", password = "testPassword", authorities = {"DOCTOR"})
public void testUpdateDoctorProfile() throws Exception {
    Map<String, String> doctorData = new HashMap<>();
    doctorData.put("specialty", "Cardiologist");
    doctorData.put("degree", "MD");

    mockMvc.perform(put("/api/users/profile/{userId}/doctor", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(doctorData)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Doctor profile updated successfully"))
            .andExpect(result -> {
                System.out.println(result.getResponse().getContentAsString());
            });

    verify(userRepository, times(1)).save(any(User.class));
}

    


    @Test
    @WithMockUser(username = "testUser", password = "testPassword")
    public void testFollowUser() throws Exception {
        User friend = new User();
        friend.setId(2L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(friend));

        mockMvc.perform(put("/api/users/following/{userId}", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("You started following user with ID: 2"));

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @WithMockUser(username = "testUser", password = "testPassword")
    public void testUnfollowUser() throws Exception {
        User friend = new User();
        friend.setId(2L);

        User currentUser = new User();
    currentUser.setId(1L);
    currentUser.getFollowing().add(friend);

        when(userRepository.findById(2L)).thenReturn(Optional.of(friend));
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));

        mockMvc.perform(put("/api/users/unfollowing/{userId}", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("You have unfollowed user with ID: 2"));


        verify(userRepository, times(1)).save(any(User.class));
    }
}
