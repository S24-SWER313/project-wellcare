package com.wellcare.wellcare.Controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import com.wellcare.wellcare.Assemblers.UserModelAssembler;
import com.wellcare.wellcare.Exceptions.UserException;
import com.wellcare.wellcare.Models.ERole;
import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Repositories.UserRepository;
import com.wellcare.wellcare.Security.Services.UserDetailsServiceImpl;
import com.wellcare.wellcare.Security.jwt.JwtUtils;
import com.wellcare.wellcare.Services.UserService;


@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

     @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    private UserModelAssembler userModelAssembler;

    @SuppressWarnings("null")
    @GetMapping("/profile/{userId}")
    public EntityModel<User> getUserProfile(@PathVariable Long userId) throws UserException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException("User with id: " + userId + " is not found."));
        return userModelAssembler.toModel(user);
    }

    @GetMapping("/{id}")
    public EntityModel<User> getUserById(@PathVariable Long id) throws UserException{
        
        User user = userService.findUserById(id);
        return userModelAssembler.toModel(user);
       
    }

    @GetMapping("/role/{roleName}")
    public EntityModel<User> getUserByRole(@PathVariable ERole roleName) throws UserException {
        Optional<User> userOptional = userRepository.findUserByRoleName(roleName);
        User user = userOptional.orElseThrow(() -> new UserException("User not found for role: " + roleName));
        return userModelAssembler.toModel(user);
    }

    @SuppressWarnings("null")
    @GetMapping("/role/all")
    public CollectionModel<EntityModel<User>> getAllUsersByRole(@RequestParam("roleName") ERole roleName){
        List<User> users = userRepository.findAllUsersByRoleName(roleName);
        return userModelAssembler.toCollectionModel(users);
    }

    @PutMapping("/profile/{userId}")
    public EntityModel<User> updateUserProfile(@RequestHeader("Authorization") String token, @RequestBody User user) throws UserException {
       
        User currentUser = userService.findUserProfile(token);
        User updatedUser = userService.updateUserProfile(user, currentUser);
        
        return userModelAssembler.toModel(updatedUser);
       
    }
    

}
