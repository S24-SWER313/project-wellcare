package com.wellcare.wellcare.Controllers;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wellcare.wellcare.Models.ERole;
import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Repositories.UserRepository;
import com.wellcare.wellcare.Security.jwt.JwtUtils;
import com.wellcare.wellcare.Security.services.UserDetailsImpl;
import com.wellcare.wellcare.Storage.StorageService;
import com.wellcare.wellcare.oAuth.AuthService;
import com.wellcare.wellcare.payload.request.LoginRequest;
import com.wellcare.wellcare.payload.request.SignupRequest;
import com.wellcare.wellcare.payload.request.TokenRequest;
import com.wellcare.wellcare.payload.response.JwtResponse;
import com.wellcare.wellcare.payload.response.MessageResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    StorageService storageService;

    @Autowired
    private AuthService authService;

    @PostMapping("/google")
    public ResponseEntity<?> authenticateGoogleUser(@RequestBody TokenRequest tokenRequest) {
        try {
            JwtResponse jwtResponse = authService.authenticateGoogleUser(tokenRequest.getToken());
            return ResponseEntity.ok(jwtResponse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @Transactional
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @ModelAttribute SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user's account
        User user = new User(signUpRequest.getUsername(), signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()));
        user.setName(signUpRequest.getName());

        if (signUpRequest.getRole() != null && signUpRequest.getRole().equals("DOCTOR")) {
            if (signUpRequest.getDegree() == null || signUpRequest.getSpecialty() == null) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Doctor specialty and degree are required!"));
            }
            user.setDegree(signUpRequest.getDegree());
            user.setSpecialty(signUpRequest.getSpecialty());
            user.setRole(ERole.DOCTOR);

            MultipartFile attachment = signUpRequest.getAttachment();
            if (attachment != null && !attachment.isEmpty()) {
                storageService.store(attachment);
                String filename = attachment.getOriginalFilename();
                String url = "http://localhost:8080/files/" + filename;// Assuming this returns the URL of the stored
                                                                       // attachment
                user.setAttachment(url); // Set the attachment URL only if an attachment is provided
            }
        } else {
            user.setRole(ERole.PATIENT);
        }

        user.setGender(signUpRequest.getGender());

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        if (loginRequest.getUsername() == null || loginRequest.getUsername().isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Validation Error"));
        }
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null,
                    userDetails.getAuthorities());

            String jwt = jwtUtils.generateJwtToken(auth);

            String roles = userDetails.getAuthorities().stream().map(item -> item.getAuthority())
                    .collect(Collectors.joining(","));

            return ResponseEntity.ok(new JwtResponse(jwt, userDetails.getId(),
                    userDetails.getUsername(), userDetails.getName(), userDetails.getEmail(), roles,
                    userDetails.getImage()));

        } catch (UsernameNotFoundException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Validation Error"));
        } catch (AuthenticationException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Invalid credentials"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Validation Error"));
        }
    }

    @GetMapping("/isAuth")
    public ResponseEntity<?> isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal() == "anonymousUser") {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Error: User is not authenticated"));
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("Error: User not found"));

        return ResponseEntity.ok(user);
    }

}
