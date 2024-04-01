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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wellcare.wellcare.Exceptions.UserException;
import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Repositories.UserRepository;
import com.wellcare.wellcare.Security.services.UserDetailsImpl;
import com.wellcare.wellcare.Storage.StorageService;
import com.wellcare.wellcare.payload.response.MessageResponse;

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
    public ResponseEntity<MessageResponse> updateUserProfile(@PathVariable Long userId, @Valid @ModelAttribute User updatedUser, @RequestParam(value = "file", required = false) MultipartFile file) {
        // Get the authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Check if the authenticated user ID matches the requested user ID
        if (!userDetails.getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("You are not authorized to update this profile"));
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
          
                if (file != null && !file.isEmpty()) {
                    System.out.println("Received file: " + file.getOriginalFilename());
                     storageService.store(file); 
                     String imageUrl = "http://localhost:8080/files/" + file.getOriginalFilename();  
                    user.setImage(imageUrl);
                } else if (updatedUser.getImage() != null) {
                    user.setImage(updatedUser.getImage());
                }
            

            // Set back the existing username and password
            user.setUsername(existingUsername);
            user.setPassword(existingPassword);

            userRepository.save(user);

            return ResponseEntity.ok().body(new MessageResponse("User profile updated successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("User not found"));
        }
    }

    // @PreAuthorize("hasRole('DOCTOR')")
    @PutMapping("/profile/{userId}/doctor")
    @Transactional
    public ResponseEntity<MessageResponse> updateDoctorProfile(@PathVariable Long userId,
            @RequestBody Map<String, String> doctorData) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        logger.info("Authorities: {}", userDetails.getAuthorities());

        if (!userDetails.getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("You are not authorized to update this profile"));
        }

        if (!userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("DOCTOR"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("You are not authorized to update doctor-specific data"));
        }

        Optional<User> existingUser = userRepository.findById(userId);
        if (existingUser.isPresent()) {
            User user = existingUser.get();

            String specialty = doctorData.get("specialty");
            String degree = doctorData.get("degree");

            if (specialty != null) {
                user.setSpecialty(specialty);
            }
            if (degree != null) {
                user.setDegree(degree);
            }
           
            userRepository.save(user);

            return ResponseEntity.ok().body(new MessageResponse("Doctor profile updated successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("User not found"));
        }
    }
     

     
    @PutMapping("/profile/{userId}/password")
    @Transactional
    public ResponseEntity<MessageResponse> updateUserPassword(@PathVariable Long userId,
            @RequestBody Map<String, String> passwordMap) {
        // Get the authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Extract the password from the map
        String newPassword = passwordMap.get("password");

        // Check if the authenticated user ID matches the requested user ID
        if (!userDetails.getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("You are not authorized to update this password"));
        }

        // Check if the new password is empty or shorter than 8 characters
        if (newPassword == null || newPassword.isEmpty() || newPassword.length() < 8) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Password should have at least 8 characters"));
        }

        Optional<User> existingUser = userRepository.findById(userId);
        if (existingUser.isPresent()) {
            User user = existingUser.get();

            // Hash the new password
            String hashedPassword = encoder.encode(newPassword);

            // Update the user's password
            user.setPassword(hashedPassword);
            userRepository.save(user);

            return ResponseEntity.ok().body(new MessageResponse("Password updated successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("User not found"));
        }
    }
    
    @PutMapping("/following/{userId}")
    @Transactional
    public ResponseEntity<MessageResponse> followUser(@PathVariable Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        Optional<User> friendOptional = userRepository.findById(userId);
        Optional<User> currentUserOptional = userRepository.findById(userDetails.getId());

        if (friendOptional.isEmpty() || currentUserOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("User to befriend not found"));
        }

        User friend = friendOptional.get();
        User currentUser = currentUserOptional.get();

        if (currentUser.getFollowing().contains(friend)) {
            return ResponseEntity.badRequest().body(new MessageResponse("You are already following the user with ID: " + userId));
        }

        currentUser.getFollowing().add(friend);
        friend.getFollowers().add(currentUser);

        userRepository.save(currentUser);

        return ResponseEntity.ok().body(new MessageResponse("You started following user with ID: " + userId));
    }

    @PutMapping("/unfollowing/{userId}")
    @Transactional
    public ResponseEntity<MessageResponse> unfriendUser(@PathVariable Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        Optional<User> friendOptional = userRepository.findById(userId);
        Optional<User> currentUserOptional = userRepository.findById(userDetails.getId());

        if (friendOptional.isEmpty() || currentUserOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("User to unfriend not found"));
        }

        User friend = friendOptional.get();
        User currentUser = currentUserOptional.get();

        if (!currentUser.getFollowing().contains(friend)) {
            return ResponseEntity.badRequest().body(new MessageResponse("You are not following the user with ID: " + userId));
        }

        currentUser.getFollowing().remove(friend);
        friend.getFollowers().remove(currentUser);

        userRepository.save(currentUser);

        return ResponseEntity.ok().body(new MessageResponse("You have unfollowed user with ID: " + userId));
    }


}