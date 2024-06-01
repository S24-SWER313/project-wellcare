package com.wellcare.wellcare.Controllers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wellcare.wellcare.Exceptions.UserException;
import com.wellcare.wellcare.Models.ERole;
import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Repositories.UserRepository;
import com.wellcare.wellcare.Security.jwt.AuthTokenFilter;
import com.wellcare.wellcare.Security.jwt.JwtUtils;
import com.wellcare.wellcare.Storage.StorageService;
import com.wellcare.wellcare.payload.response.MessageResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    PasswordEncoder encoder;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StorageService storageService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private AuthTokenFilter authTokenFilter;

    @GetMapping("/profile/{username}")
    public ResponseEntity<?> getUserProfile(@PathVariable String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.getSavedPost().size();
            return ResponseEntity.ok().body(user);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }

    @GetMapping("/loggedin-profile")
    public ResponseEntity<?> getLoggedInUserProfile(HttpServletRequest request) {
        String jwtToken = authTokenFilter.parseJwt(request);
        System.out.println("Extracted JWT token: " + jwtToken);

        Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);
        System.out.println("Extracted userId: " + userId);

        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return ResponseEntity.ok().body(user);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }

    @PutMapping("/profile")
@Transactional
public ResponseEntity<?> updateUserProfile(HttpServletRequest request,
        @Valid @ModelAttribute User updatedUser,
        @RequestParam(value = "profile_picture", required = false) MultipartFile file,
        @RequestParam(value = "remove_profile_picture", required = false) Boolean removeProfilePicture) throws UserException {

    String jwtToken = authTokenFilter.parseJwt(request);
    System.out.println("Extracted JWT token: " + jwtToken);

    Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);
    System.out.println("Extracted userId: " + userId);

    Optional<User> existingUserOptional = userRepository.findById(userId);
    User user = existingUserOptional.orElseThrow(() -> new UserException("User not found"));

    System.out.println("User ID from database: " + user.getId());
    System.out.println("Updated User ID: " + updatedUser.getId());

    if (!user.getId().equals(userId)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("You are not authorized to update this profile"));
    }

    Optional<User> existingUser = userRepository.findById(userId);
    if (existingUser.isPresent()) {
        User userExisting = existingUser.get();

        String existingUsername = userExisting.getUsername();
        String existingPassword = userExisting.getPassword();

        // Update only the fields that are not null in the request body
        if (updatedUser.getName() != null) {
            userExisting.setName(updatedUser.getName());
        }
        if (updatedUser.getEmail() != null) {
            userExisting.setEmail(updatedUser.getEmail());
        }
        if (updatedUser.getMobile() != null) {
            userExisting.setMobile(updatedUser.getMobile());
        }
        if (updatedUser.getBio() != null) {
            userExisting.setBio(updatedUser.getBio());
        }
        if (updatedUser.getGender() != null) {
            userExisting.setGender(updatedUser.getGender());
        }

        if (removeProfilePicture != null && removeProfilePicture) {
            userExisting.setImage(null); // or set to a default image URL if you have one
        } else if (file != null && !file.isEmpty()) {
            System.out.println("Received file: " + file.getOriginalFilename());
            storageService.store(file);
            String imageUrl = "http://localhost:8080/files/" + file.getOriginalFilename();
            userExisting.setImage(imageUrl);
        } else if (updatedUser.getImage() != null) {
            userExisting.setImage(updatedUser.getImage());
        }

        // Set back the existing username and password
        userExisting.setUsername(existingUsername);
        userExisting.setPassword(existingPassword);

        User savedUser = userRepository.save(userExisting);

        return ResponseEntity.ok().body(new MessageResponse("User profile updated successfully", savedUser));
    } else {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("User not found"));
    }
}

    @PreAuthorize("hasAuthority('DOCTOR')")
    @PutMapping("/profile/doctor")
    @Transactional
    public ResponseEntity<?> updateDoctorProfile(HttpServletRequest request,
            @RequestParam("specialty") String specialty,
            @RequestParam("degree") String degree,
            @RequestParam(value = "attachment", required = false) MultipartFile file) throws UserException {

        try {
            String jwtToken = authTokenFilter.parseJwt(request);
            System.out.println("Extracted JWT token: " + jwtToken);

            // Parse the JWT token to extract the userId
            Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);
            System.out.println("Extracted userId: " + userId);

            // Use the extracted userId to get the User object
            Optional<User> existingUserOptional = userRepository.findById(userId);
            User userToUpdate = existingUserOptional.orElseThrow(() -> new UserException("User not found"));

            if (!userToUpdate.getRole().equals(ERole.DOCTOR)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Collections.singletonMap("message", "Access is denied"));
            }

            if (specialty != null) {
                userToUpdate.setSpecialty(specialty);
            }
            if (degree != null) {
                userToUpdate.setDegree(degree);
            }

            if (file != null && !file.isEmpty()) {
                System.out.println("Received file: " + file.getOriginalFilename());
                storageService.store(file);
                String imageUrl = "http://localhost:8080/files/" + file.getOriginalFilename();
                userToUpdate.setAttachment(imageUrl);
            } else if (userToUpdate.getAttachment() != null) {
                userToUpdate.setAttachment(userToUpdate.getAttachment());
            }

            User savedUser = userRepository.save(userToUpdate);

            return ResponseEntity.ok().body(new MessageResponse("Doctor profile updated successfully", savedUser));

        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message",
                            "Unauthorized: Full authentication is required to access this resource"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", "Access is denied"));
        }
    }

    @PutMapping("/profile/password")
    @Transactional
    public ResponseEntity<MessageResponse> updateUserPassword(HttpServletRequest request,
            @RequestBody Map<String, String> passwordMap) throws UserException {
        try {
            // Get the authenticated user
            String jwtToken = authTokenFilter.parseJwt(request);
            System.out.println("Extracted JWT token: " + jwtToken);

            // Parse the JWT token to extract the userId
            Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);
            System.out.println("Extracted userId: " + userId);

            // Use the extracted userId to get the User object
            Optional<User> existingUserOptional = userRepository.findById(userId);
            User user = existingUserOptional.orElseThrow(() -> new UserException("User not found"));

            // Extract the password from the map
            String newPassword = passwordMap.get("password");

            // Check if the authenticated user ID matches the requested user ID
            if (!user.getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new MessageResponse("You are not authorized to update this password"));
            }

            // Check if the new password is empty or shorter than 8 characters
            if (newPassword == null || newPassword.isEmpty() || newPassword.length() < 8) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new MessageResponse("Password should have at least 8 characters"));
            }

            if (encoder.matches(newPassword, user.getPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new MessageResponse("Password isn't updated"));
            }

            // Hash the new password
            String hashedPassword = encoder.encode(newPassword);

            // Update the user's password
            user.setPassword(hashedPassword);
            userRepository.save(user);

            return ResponseEntity.ok().body(new MessageResponse("Password updated successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Unauthorized: Full authentication is required to access this resource"));
        }

    }

    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUserHandler(@RequestParam("q") String query) throws UserException {
        List<User> users = userRepository.findByQuery(query);

        return new ResponseEntity<List<User>>(users, HttpStatus.OK);
    }

}