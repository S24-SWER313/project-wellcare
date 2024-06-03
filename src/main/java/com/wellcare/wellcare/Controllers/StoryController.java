package com.wellcare.wellcare.Controllers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wellcare.wellcare.Models.Story;
import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Repositories.StoryRepository;
import com.wellcare.wellcare.Repositories.UserRepository;
import com.wellcare.wellcare.Security.jwt.AuthTokenFilter;
import com.wellcare.wellcare.Security.jwt.JwtUtils;
import com.wellcare.wellcare.Storage.StorageService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/stories")
public class StoryController {

    @Autowired
    JwtUtils jwtUtils;
    @Autowired
    AuthTokenFilter authTokenFilter;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    StorageService storageService;
    private static final Logger logger = LoggerFactory.getLogger(StoryController.class);

    // Endpoint to get stories for a user
    @GetMapping("/user/{username}")
    public ResponseEntity<?> getUserStories(@PathVariable String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            List<Story> stories = storyRepository.findTop20ByUserOrderByCreatedAtDesc(user);
            return ResponseEntity.ok().body(stories);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }

    // Endpoint to get non-expired stories for a user
    // Endpoint to get all stories (excluding expired ones)
    @GetMapping("/active")
    public ResponseEntity<?> getAllActiveStories() {
        // Get the current date and time
        LocalDateTime currentTime = LocalDateTime.now();

        // Find all stories that are not expired
        List<Story> stories = storyRepository.findAllByExpiresAtAfter(currentTime);

        if (!stories.isEmpty()) {
            return ResponseEntity.ok().body(stories);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No active stories found");
        }
    }

    @GetMapping("/archived")
    public ResponseEntity<?> getAllExpiredStories() {
        // Get the current date and time
        LocalDateTime currentTime = LocalDateTime.now();

        // Find all stories that are expired
        List<Story> expiredStories = storyRepository.findAllByExpiresAtBefore(currentTime);

        if (!expiredStories.isEmpty()) {
            return ResponseEntity.ok().body(expiredStories);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No expired stories found");
        }
    }

    // Endpoint to get all stories
    @GetMapping("/")
    public ResponseEntity<?> getAllStories() {
        List<Story> stories = storyRepository.findAll();
        if (!stories.isEmpty()) {
            return ResponseEntity.ok().body(stories);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No stories found");
        }
    }

    @Transactional
    @DeleteMapping("/expire")
    public ResponseEntity<?> deleteExpiredStories() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            // Example: Expire stories after 24 hours
            storyRepository.deleteByUserAndCreatedAtBefore(user, LocalDateTime.now().minusHours(24));
        }
        return ResponseEntity.ok().body("Expired stories deleted successfully");
    }

    @PostMapping("/user/{id}")
    public ResponseEntity<?> createStory(HttpServletRequest request, @PathVariable long id,
            @RequestPart("caption") String caption, @RequestPart("image") MultipartFile imageFile) {
        try {
            Optional<User> userOptional = userRepository.findById(id);
            if (userOptional.isPresent()) {
                User user = userOptional.get();

                // Save the image to a location and get the URL/path
                System.out.println("Received file: " + imageFile.getOriginalFilename());
                storageService.store(imageFile);
                String filename = imageFile.getOriginalFilename();
                String url = "http://localhost:8080/files/" + filename;

                Story story = new Story();
                story.setUser(user);
                story.setCaption(caption);
                story.setImage(url);
                story.setCreatedAt(LocalDateTime.now());
                story.setExpiresAt(LocalDateTime.now().plusHours(24)); // Set expiry to 24 hours from creation

                storyRepository.save(story);

                // Return the created story details
                return ResponseEntity.status(HttpStatus.CREATED).body(story);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to save image");
        }
    }

    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStory(@PathVariable Long id) {
        try {
            logger.info("Request received to delete story with ID: " + id);
            Optional<Story> storyOptional = storyRepository.findById(id);
            if (storyOptional.isPresent()) {
                Story story = storyOptional.get();
                logger.info("Found story in the database: " + story);

                // Delete the story
                storyRepository.delete(story);

                return ResponseEntity.ok().body("Story deleted successfully");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Story not found");
            }
        } catch (Exception e) {
            logger.error("Error deleting story with ID: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete story: " + e.getMessage());
        }
    }

}
