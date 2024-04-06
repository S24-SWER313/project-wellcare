package com.wellcare.wellcare;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wellcare.wellcare.Models.ERole;
import com.wellcare.wellcare.Models.Gender;
import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Repositories.UserRepository;
import com.wellcare.wellcare.Security.jwt.JwtUtils;
import com.wellcare.wellcare.Security.services.UserDetailsImpl;

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

                ERole role = ERole.PATIENT;

                List<GrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toString()));

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
                updatedUser.setMobile("1234567890");
                updatedUser.setBio("Updated bio");
                updatedUser.setGender(Gender.MALE);

                when(userRepository.findById(1L)).thenReturn(Optional.of(updatedUser));
                when(userRepository.save(any(User.class))).thenReturn(updatedUser);

                mockMvc.perform(put("/api/users/profile/{userId}", 1L)
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .param("name", "Updated User")
                                .param("email", "updated@example.com")
                                .param("mobile", "1234567890")
                                .param("bio", "Updated bio")
                                .param("gender", "MALE"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("User profile updated successfully"));

                verify(userRepository, times(1)).findById(anyLong());
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
        @WithMockUser(username = "testUser", password = "testPassword", authorities = { "DOCTOR" })
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
        @WithMockUser(username = "testUser", password = "testPassword", authorities = { "DOCTOR" })
        public void testUpdateDoctorProfile_Unauthorized() throws Exception {
                Map<String, String> doctorData = new HashMap<>();
                doctorData.put("specialty", "Cardiologist");
                doctorData.put("degree", "MD");

                mockMvc.perform(put("/api/users/profile/{userId}/doctor", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(doctorData)))
                                .andExpect(status().isForbidden());

                verify(userRepository, times(0)).save(any(User.class));
        }

        @Test
        @WithMockUser(username = "testUser", password = "testPassword")
        public void testUnfollowUser_UserNotFollowing() throws Exception {
                User friend = new User();
                friend.setId(2L);

                when(userRepository.findById(2L)).thenReturn(Optional.of(friend));
                when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));

                mockMvc.perform(put("/api/users/unfollowing/{userId}", 2L))
                                .andExpect(status().isBadRequest());

                verify(userRepository, times(0)).save(any(User.class));
        }

        @Test
        public void testUpdateUserProfile_InvalidEmail() throws Exception {
                User updatedUser = new User();
                updatedUser.setName("Updated User");
                updatedUser.setEmail("invalidemail");

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                                new UserDetailsImpl(1L, "testUser", "test@example.com", "testPassword",
                                                List.of(new SimpleGrantedAuthority("PATIENT"))),
                                null,
                                List.of(new SimpleGrantedAuthority("PATIENT")));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                mockMvc.perform(put("/api/users/profile/{userId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updatedUser)))
                                .andExpect(status().isBadRequest());

                verify(userRepository, times(0)).save(any(User.class));
        }

        @Test
        @WithMockUser(username = "testUser", password = "testPassword")
        public void testUpdateUserPassword_InvalidPassword() throws Exception {
                mockMvc.perform(put("/api/users/profile/{userId}/password", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"password\": \"short\"}"))
                                .andExpect(status().isBadRequest());

                verify(userRepository, times(0)).save(any(User.class));
        }

}