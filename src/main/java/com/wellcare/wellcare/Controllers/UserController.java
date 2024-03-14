package com.wellcare.wellcare.Controllers;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Repositories.UserRepository;

import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/profile/{userId}")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> getUserProfile(@PathVariable Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.getSavedPost().size(); // Initialize the collection
            return ResponseEntity.ok().body(user);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }

    @PutMapping("/profile/{userId}")
    @Transactional
    public ResponseEntity<?> updateUserProfile(@PathVariable Long userId, @RequestBody User updatedUser) {
        Optional<User> existingUser = userRepository.findById(userId);
        if (existingUser.isPresent()) {
            User user = existingUser.get();

            user.setFirstName(updatedUser.getFirstName());
            user.setUsername(updatedUser.getUsername());
            user.setLastName(updatedUser.getLastName());
            user.setEmail(updatedUser.getEmail());
            user.setMobile(updatedUser.getMobile());
            user.setBio(updatedUser.getBio());
            user.setGender(updatedUser.getGender());
            user.setImage(updatedUser.getImage());
            userRepository.save(user);

            return ResponseEntity.ok().body("User profile updated successfully");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }
}