package com.wellcare.wellcare.oAuth;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.wellcare.wellcare.Models.ERole;
import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Repositories.UserRepository;
import com.wellcare.wellcare.Security.jwt.JwtUtils;
import com.wellcare.wellcare.Security.services.UserDetailsImpl;
import com.wellcare.wellcare.Security.services.UserDetailsServiceImpl;
import com.wellcare.wellcare.payload.response.JwtResponse;

@Service
public class AuthService {

    @Autowired
    private GoogleTokenVerifier googleTokenVerifier;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    public JwtResponse authenticateGoogleUser(String googleToken) throws Exception {
        GoogleIdToken.Payload payload = googleTokenVerifier.verifyToken(googleToken);
    
        String email = payload.getEmail();
        Optional<User> userOptional = userRepository.findByEmail(email);
    
        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
        } else {
            user = new User();
            user.setEmail(email);
            user.setName(payload.get("name").toString());
            user.setUsername(payload.get("name").toString());
            user.setPassword(passwordEncoder.encode("defaultPassword"));
            user.setRole(ERole.PATIENT);
            user.setImage( "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460__340.png");
            userRepository.save(user);
        }
    
        UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(user.getUsername());
    
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    
        String token = jwtUtils.generateJwtToken(authentication);
    
        return new JwtResponse(token, userDetails.getId(), userDetails.getUsername(), userDetails.getName(), userDetails.getEmail());
    }}