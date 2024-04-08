package com.wellcare.wellcare;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wellcare.wellcare.Controllers.UserController;
import com.wellcare.wellcare.Models.ERole;
import com.wellcare.wellcare.Models.Gender;
import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Repositories.UserRepository;
import com.wellcare.wellcare.Security.jwt.AuthTokenFilter;
import com.wellcare.wellcare.Security.jwt.JwtUtils;
import com.wellcare.wellcare.Storage.StorageService;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private AuthTokenFilter authTokenFilter;

    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
    }

    @Test
    public void testGetUserProfileSuccess() throws Exception {
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testUser");

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        mockMvc.perform(get("/api/users/profile/1"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.username").value("testUser"));
    }

    @Test
    public void testGetUserProfileNotFound() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/profile/1"))
               .andExpect(status().isNotFound())
               .andExpect(content().string("User not found"));
    }

    @Test
    public void testUpdateUserProfileSuccess() throws Exception {
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testUser");
        mockUser.setPassword("oldPassword");

        User updatedUser = new User();
        updatedUser.setName("updatedName");
        updatedUser.setEmail("updated@test.com");
        updatedUser.setMobile("1234567890");
        updatedUser.setBio("updatedBio");
        updatedUser.setGender(Gender.MALE);
        updatedUser.setImage("http://localhost:8080/files/image.jpg");

        when(authTokenFilter.parseJwt(any())).thenReturn("jwtToken");
        when(jwtUtils.getUserIdFromJwtToken(any())).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        doNothing().when(storageService).store(any()); 
        when(userRepository.save(any())).thenReturn(updatedUser);


        mockMvc.perform(
                put("/api/users/profile")
                        .content(new ObjectMapper().writeValueAsString(updatedUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer jwtToken"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.message").value("User profile updated successfully"));
    }

    @Test
public void testUpdateUserProfileUnauthorized() throws Exception {
    User mockUser = new User();
    mockUser.setId(1L);
    mockUser.setUsername("testUser");

    User updatedUser = new User();
    updatedUser.setId(1L); 
    updatedUser.setBio("Welcome to my profile!");

    when(authTokenFilter.parseJwt(any())).thenReturn("invalidJwtToken"); // Providing an incorrect JWT token
    when(jwtUtils.getUserIdFromJwtToken(any())).thenReturn(2L); // Any ID different from the mockUser's ID
    when(userRepository.findById(2L)).thenReturn(Optional.of(mockUser));

    mockMvc.perform(
            put("/api/users/profile")
                    .content(new ObjectMapper().writeValueAsString(updatedUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer invalidJwtToken"))
           .andExpect(status().isUnauthorized())
           .andExpect(jsonPath("$.message").value("You are not authorized to update this profile"));
}

    

    @Test
    public void testUpdateDoctorProfileSuccess() throws Exception {
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("doctorUser");
        mockUser.setRole(ERole.DOCTOR);

        User updatedUser = new User();
        updatedUser.setSpecialty("updated");
        updatedUser.setDegree("updated");
        updatedUser.setAttachment("http://localhost:8080/files/images.png");

        MockMultipartFile file = new MockMultipartFile("file", "images.png", "image/png", "test data".getBytes());

        when(authTokenFilter.parseJwt(any())).thenReturn("jwtToken");
        when(jwtUtils.getUserIdFromJwtToken(any())).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        doNothing().when(storageService).store(any());
        when(userRepository.save(any())).thenReturn(updatedUser);

        mockMvc.perform(
                put("/api/users/profile/doctor")
                        .param("specialty", "updated")
                        .param("degree", "updated")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .header("Authorization", "Bearer jwtToken")
                        .content(file.getBytes()))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.message").value("Doctor profile updated successfully"));
    }

    @Test
    public void testUpdateDoctorProfileUnauthorized() throws Exception {
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("doctorUser");
    
        when(authTokenFilter.parseJwt(any())).thenReturn("jwtToken");
        when(jwtUtils.getUserIdFromJwtToken(any())).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        doNothing().when(storageService).store(any());
    
        MockMultipartFile file = new MockMultipartFile("file", "images.png", "image/png", "test data".getBytes());
    
        mockMvc.perform(
                MockMvcRequestBuilders.put("/api/users/profile/doctor")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .content(file.getBytes())
                        .param("specialty", "updated")
                        .param("degree", "updated")
                        .header("Authorization", "Bearer jwtToken"))
               .andExpect(status().isBadRequest())  // Expecting a 400 Bad Request status
               .andExpect(jsonPath("$.message").value("Access is denied"));
    }
    
    
    @Test
    public void testUpdateDoctorProfile_LoggedInAsPatient() throws Exception {
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("patientUser");
        mockUser.setRole(ERole.PATIENT);
    
        when(authTokenFilter.parseJwt(any())).thenReturn("jwtToken");
        when(jwtUtils.getUserIdFromJwtToken(any())).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        doNothing().when(storageService).store(any());
    
        MockMultipartFile file = new MockMultipartFile("file", "images.png", "image/png", "test data".getBytes());
    
        mockMvc.perform(
                MockMvcRequestBuilders.put("/api/users/profile/doctor")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .content(file.getBytes())
                        .param("specialty", "updated")
                        .param("degree", "updated")
                        .header("Authorization", "Bearer jwtToken"))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.message").value("Access is denied"));
    }
    
    

    @Test
    public void testUpdateDoctorProfileWithoutFile() throws Exception {
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("doctorUser");
        mockUser.setRole(ERole.DOCTOR);

        User updatedUser = new User();
        updatedUser.setSpecialty("updated");
        updatedUser.setDegree("updated");

        when(authTokenFilter.parseJwt(any())).thenReturn("jwtToken");
        when(jwtUtils.getUserIdFromJwtToken(any())).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any())).thenReturn(updatedUser);

        mockMvc.perform(
                put("/api/users/profile/doctor")
                        .param("specialty", "updated")
                        .param("degree", "updated")
                        .header("Authorization", "Bearer jwtToken"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.message").value("Doctor profile updated successfully"));
    }

    @Test
    public void testUpdateUserPasswordSuccess() throws Exception {
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testUser");
        mockUser.setPassword("$2a$10$12345678901234567890"); 

        when(authTokenFilter.parseJwt(any())).thenReturn("jwtToken");
        when(jwtUtils.getUserIdFromJwtToken(any())).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(encoder.encode(anyString())).thenReturn("$2a$10$09876543210987654321"); // hashed "newPassword"
        when(userRepository.save(any())).thenReturn(mockUser);

        mockMvc.perform(
                put("/api/users/profile/password")
                        .content("{\"password\": \"newPassword\"}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer jwtToken"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.message").value("Password updated successfully"));
    }

    @Test
    public void testUpdateUserPasswordBadRequest() throws Exception {
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testUser");
        mockUser.setPassword("$2a$10$12345678901234567890"); 

        when(authTokenFilter.parseJwt(any())).thenReturn("jwtToken");
        when(jwtUtils.getUserIdFromJwtToken(any())).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        mockMvc.perform(
                put("/api/users/profile/password")
                        .content("{\"password\": \"123\"}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer jwtToken"))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.message").value("Password should have at least 8 characters"));
    }

    @Test
public void testUpdateUserPasswordUnauthorized() throws Exception {
    User mockUser = new User();
    mockUser.setId(1L);
    mockUser.setUsername("testUser");
    mockUser.setPassword("$2a$10$oldPasswordHashed"); // hashed "oldPassword"

    when(authTokenFilter.parseJwt(any())).thenReturn("jwtToken");
    when(jwtUtils.getUserIdFromJwtToken(any())).thenReturn(2L);  // Different user ID to make it unauthorized
    when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

    mockMvc.perform(
            put("/api/users/profile/password")
                    .content("{\"password\": \"newPassword\"}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer jwtToken"))
           .andExpect(status().isUnauthorized())
           .andExpect(jsonPath("$.message").value("Unauthorized: Full authentication is required to access this resource"));
}

}


