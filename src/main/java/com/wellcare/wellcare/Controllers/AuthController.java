package com.wellcare.wellcare.Controllers;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wellcare.wellcare.Models.ERole;
import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Repositories.RoleRepository;
import com.wellcare.wellcare.Repositories.UserRepository;
import com.wellcare.wellcare.Security.jwt.JwtUtils;
import com.wellcare.wellcare.Security.services.UserDetailsImpl;
import com.wellcare.wellcare.Storage.StorageService;
import com.wellcare.wellcare.payload.request.LoginRequest;
import com.wellcare.wellcare.payload.request.SignupRequest;
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
    RoleRepository roleRepository;

    @Autowired
    StorageService storageService;

    @PostMapping("/signup")
    @Transactional
    public ResponseEntity<?> registerUser(@Valid @ModelAttribute SignupRequest signUpRequest,
            @RequestParam(value = "file", required = false) MultipartFile attachment) {
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
            System.out.println("testtttttttttttttttttttttttttttttttttttttt");
            System.out.println("attachment" + attachment);
            if (attachment != null) {
                System.out.println("Received file: " + attachment.getOriginalFilename());
                storageService.store(attachment);
                String filename = attachment.getOriginalFilename();
                String url = "http://localhost:8080/files/" + filename;
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
                    userDetails.getUsername(), userDetails.getEmail(), roles));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Validation Error"));
        } catch (AuthenticationException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Invalid credentials"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Validation Error"));
        }
    }

}