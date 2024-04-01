package com.wellcare.wellcare;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wellcare.wellcare.Models.ERole;
import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Repositories.RoleRepository;
import com.wellcare.wellcare.Repositories.UserRepository;
import com.wellcare.wellcare.Security.jwt.JwtUtils;
import com.wellcare.wellcare.Security.services.UserDetailsImpl;
import com.wellcare.wellcare.payload.request.LoginRequest;
import com.wellcare.wellcare.payload.request.SignupRequest;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private UserRepository userRepository;

        @MockBean
        private RoleRepository roleRepository;

        @MockBean
        private JwtUtils jwtUtils;

        @MockBean
        private AuthenticationManager authenticationManager;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @BeforeEach
        public void setUp() {
                User user = new User();
                user.setId(1L);
                user.setUsername("testUser");
                user.setPassword(passwordEncoder.encode("testPassword"));
                user.setName("Test User");
                user.setEmail("test@example.com");

                user.setRole(ERole.PATIENT);

                List<GrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + ERole.PATIENT.name().toString()));

                UserDetailsImpl userDetails = new UserDetailsImpl(
                                user.getId(),
                                user.getUsername(),
                                user.getEmail(),
                                user.getPassword(),
                                authorities);

                when(userRepository.existsByUsername(anyString())).thenReturn(false);
                when(userRepository.existsByEmail(anyString())).thenReturn(false);
                when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
                when(userRepository.save(any(User.class))).thenReturn(user);

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        @Test
        public void testRegisterUserDuplicateUsername() throws Exception {
                SignupRequest signupRequest = new SignupRequest();
                signupRequest.setUsername("testUser");
                signupRequest.setPassword("newPassword123");
                signupRequest.setEmail("new@example.com");
                signupRequest.setName("New User");
                signupRequest.setRole("PATIENT");

                when(userRepository.existsByUsername(anyString())).thenReturn(true);

                mockMvc.perform(post("/api/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(signupRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Error: Username is already taken!"));

                verify(userRepository, times(0)).save(any(User.class));
        }

        @Test
        public void testRegisterUserDuplicateEmail() throws Exception {
                SignupRequest signupRequest = new SignupRequest();
                signupRequest.setUsername("newUser");
                signupRequest.setPassword("newPassword123");
                signupRequest.setEmail("test@example.com");
                signupRequest.setName("New User");
                signupRequest.setRole("PATIENT");

                when(userRepository.existsByEmail(anyString())).thenReturn(true);

                mockMvc.perform(post("/api/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(signupRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Error: Email is already in use!"));

                verify(userRepository, times(0)).save(any(User.class));
        }

        @Test
        public void testAuthenticateUserWrongPassword() throws Exception {
                LoginRequest loginRequest = new LoginRequest();
                loginRequest.setUsername("testUser");
                loginRequest.setPassword("wrongPassword");

                when(authenticationManager.authenticate(any())).thenThrow(new RuntimeException("Invalid credentials"));

                mockMvc.perform(post("/api/auth/signin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isBadRequest()); // Expecting 400 Bad Request due to
                                                                     // GlobalExceptionHandler

                verify(authenticationManager, times(1)).authenticate(any());
        }

        @Test
        public void testAuthenticateUserNonExistingUsername() throws Exception {
                LoginRequest loginRequest = new LoginRequest();

                loginRequest.setPassword("password123");

                when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
                when(authenticationManager.authenticate(any()))
                                .thenThrow(new UsernameNotFoundException("User not found"));

                mockMvc.perform(post("/api/auth/signin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Validation Error"));

                verify(authenticationManager, times(0)).authenticate(any());

        }

        @Test
        public void testRegisterUserSuccess() throws Exception {
                SignupRequest signupRequest = new SignupRequest();
                signupRequest.setUsername("newUser");
                signupRequest.setPassword("newPassword123");
                signupRequest.setEmail("new@example.com");
                signupRequest.setName("New User");
                signupRequest.setRole("PATIENT");

                mockMvc.perform(post("/api/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(signupRequest)))
                                .andExpect(status().isOk())
                                .andExpect(content().string("{\"message\":\"User registered successfully!\"}"));

                verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        public void testAuthenticateUserSuccess() throws Exception {
                LoginRequest loginRequest = new LoginRequest();
                loginRequest.setUsername("testUser");
                loginRequest.setPassword("testPassword");

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                                new UserDetailsImpl(
                                                1L,
                                                "testUser",
                                                "test@example.com",
                                                passwordEncoder.encode("testPassword"),
                                                new ArrayList<>()),
                                null,
                                new ArrayList<>());
                when(authenticationManager.authenticate(any())).thenReturn(authentication);

                mockMvc.perform(post("/api/auth/signin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("testUser"))
                                .andExpect(jsonPath("$.email").value("test@example.com"));

                verify(authenticationManager, times(1)).authenticate(any());
        }

        @Test
        public void testAuthenticateUserInvalidCredentials() throws Exception {
                LoginRequest loginRequest = new LoginRequest();
                loginRequest.setUsername("testUser");
                loginRequest.setPassword("wrongPassword");

                when(authenticationManager.authenticate(any())).thenThrow(new RuntimeException("Invalid credentials"));

                mockMvc.perform(post("/api/auth/signin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isBadRequest());

                verify(authenticationManager, times(1)).authenticate(any());
        }

}
