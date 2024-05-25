package com.wellcare.wellcare.Controllers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wellcare.wellcare.Models.Story;
import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Repositories.UserRepository;

// Import necessary dependenci

@RestController
@RequestMapping("/api/stories")
public class StoryController {

    @Autowired
    private UserRepository userRepository;

    // Endpoint to get stories for a user
    @GetMapping("/user/{username}")
    //
    //
    public ResponseEntity<?> getUserStories(@PathVariable String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            List<Story> stories = userRepository.findTop20ByUserOrderByCreatedAtDesc(user);
            return ResponseEntity.ok().body(stories);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }

    // Endpoint to delete expired stories
    @DeleteMapping("/expire")
    public ResponseEntity<?> deleteExpiredStories() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            userRepository.deleteByUserAndCreatedAtBefore(user, LocalDateTime.now().minusHours(24)); // Example: Expire
                                                                                                     // stories after 24
                                                                                                     // hours
        }
        return ResponseEntity.ok().body("Expired stories deleted successfully");
    }
}
