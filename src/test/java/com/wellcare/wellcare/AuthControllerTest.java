package com.wellcare.wellcare;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wellcare.wellcare.Controllers.AuthController;
import com.wellcare.wellcare.Models.ERole;
import com.wellcare.wellcare.Repositories.UserRepository;
import com.wellcare.wellcare.Security.jwt.JwtUtils;
import com.wellcare.wellcare.Security.services.UserDetailsImpl;
import com.wellcare.wellcare.Storage.StorageService;
import com.wellcare.wellcare.payload.request.LoginRequest;
import com.wellcare.wellcare.payload.request.SignupRequest;

@WebMvcTest(AuthController.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PasswordEncoder encoder;

    @MockBean
    private JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StorageService storageService;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void testRegisterUserSuccess() throws Exception {
        SignupRequest request = new SignupRequest();
        request.setUsername("testUser");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setName("Test User");
        request.setRole(ERole.PATIENT.toString());

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(encoder.encode(any(CharSequence.class))).thenReturn("encodedPassword");

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .param("username", request.getUsername())
                .param("email", request.getEmail())
                .param("password", request.getPassword())
                .param("name", request.getName())
                .param("role", request.getRole()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully!"));
    }

    @Test
    public void testRegisterUsernameTaken() throws Exception {
        SignupRequest request = new SignupRequest();
        request.setUsername("existingUser");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setName("Test User");
        request.setRole("PATIENT");

        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .param("username", request.getUsername())
                .param("email", request.getEmail())
                .param("password", request.getPassword())
                .param("name", request.getName())
                .param("role", request.getRole()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: Username is already taken!"));
    }

    @Test
    public void testRegisterEmailTaken() throws Exception {
        SignupRequest request = new SignupRequest();
        request.setUsername("testUser");
        request.setEmail("existing@example.com");
        request.setPassword("password123");
        request.setName("Test User");
        request.setRole("PATIENT");

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .param("username", request.getUsername())
                .param("email", request.getEmail())
                .param("password", request.getPassword())
                .param("name", request.getName())
                .param("role", request.getRole()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: Email is already in use!"));
    }

    @Test
    public void testRegisterDoctorMissingDetails() throws Exception {
        SignupRequest request = new SignupRequest();
        request.setUsername("doctorUser");
        request.setEmail("doctor@example.com");
        request.setPassword("password123");
        request.setName("Doctor User");
        request.setRole("DOCTOR");

        MockMultipartFile file = new MockMultipartFile("attachment", "test.txt", "text/plain", "test data".getBytes());

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/auth/signup")
        .file(file)
        .param("username", request.getUsername())
        .param("email", request.getEmail())
        .param("password", request.getPassword())
        .param("name", request.getName())
        .param("role", request.getRole()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Error: Doctor specialty and degree are required!"));
    }

    
    @Test
    public void testRegisterMissingUsername() throws Exception {
        SignupRequest request = new SignupRequest();
        request.setUsername(null);
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setName("Test User");
        request.setRole(ERole.PATIENT.toString());

        mockMvc.perform(post("/api/auth/signup")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .param("username", request.getUsername())
        .param("email", request.getEmail())
        .param("password", request.getPassword())
        .param("name", request.getName())
        .param("role", request.getRole()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Validation Error"));
    }

    @Test
public void testRegisterInvalidEmail() throws Exception {
    SignupRequest request = new SignupRequest();
    request.setUsername("test_user1");
    request.setEmail("test@");
    request.setPassword("password123");
    request.setName("Test User");
    request.setRole(ERole.DOCTOR.toString());

    mockMvc.perform(post("/api/auth/signup")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .param("username", request.getUsername())
            .param("email", request.getEmail())
            .param("password", request.getPassword())
            .param("name", request.getName())
            .param("role", request.getRole()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation Error"));
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
                        encoder.encode("testPassword"),
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

    @Test
    public void testAuthenticateUserValidationFailure() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername(null);
        request.setPassword(null);

        mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation Error"));
    }

    public static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}