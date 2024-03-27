package com.wellcare.wellcare.Controllers;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Repositories.UserRepository;
import com.wellcare.wellcare.Security.services.UserDetailsImpl;

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

    @GetMapping("/profile/{userId}")
    public ResponseEntity<?> getUserProfile(@PathVariable Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.getSavedPost().size();
            return ResponseEntity.ok().body(user);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }

    @PutMapping("/profile/{userId}")
    @Transactional
    public ResponseEntity<?> updateUserProfile(@PathVariable Long userId, @Valid @RequestBody User updatedUser) {
        // Get the authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Check if the authenticated user ID matches the requested user ID
        if (!userDetails.getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not authorized to update this profile");
        }

        Optional<User> existingUser = userRepository.findById(userId);
        if (existingUser.isPresent()) {
            User user = existingUser.get();

            String existingUsername = user.getUsername();
            String existingPassword = user.getPassword();

            // Update only the fields that are not null in the request body
            if (updatedUser.getName() != null) {
                user.setName(updatedUser.getName());
            }
            if (updatedUser.getEmail() != null) {
                user.setEmail(updatedUser.getEmail());
            }
            if (updatedUser.getMobile() != null) {
                user.setMobile(updatedUser.getMobile());
            }
            if (updatedUser.getBio() != null) {
                user.setBio(updatedUser.getBio());
            }
            if (updatedUser.getGender() != null) {
                user.setGender(updatedUser.getGender());
            }
            if (updatedUser.getImage() != null) {
                user.setImage(updatedUser.getImage());
            }

            // Set back the existing username and password
            user.setUsername(existingUsername);
            user.setPassword(existingPassword);

            userRepository.save(user);

            return ResponseEntity.ok().body("User profile updated successfully");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }

    @PutMapping("/profile/{userId}/password")
    @Transactional
    public ResponseEntity<?> updateUserPassword(@PathVariable Long userId,
            @RequestBody Map<String, String> passwordMap) {
        // Get the authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Extract the password from the map
        String newPassword = passwordMap.get("password");

        // Check if the authenticated user ID matches the requested user ID
        if (!userDetails.getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("You are not authorized to update this password");
        }

        // Check if the new password is empty or shorter than 8 characters
        if (newPassword == null || newPassword.isEmpty() || newPassword.length() < 8) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Password should have at least 8 characters");
        }

        Optional<User> existingUser = userRepository.findById(userId);
        if (existingUser.isPresent()) {
            User user = existingUser.get();

            // Hash the new password
            String hashedPassword = encoder.encode(newPassword);

            // Update the user's password
            user.setPassword(hashedPassword);
            userRepository.save(user);

            return ResponseEntity.ok().body("Password updated successfully");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }
}