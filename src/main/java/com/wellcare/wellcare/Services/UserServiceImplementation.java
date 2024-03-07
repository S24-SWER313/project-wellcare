package com.wellcare.wellcare.Services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service; 

import com.wellcare.wellcare.Security.jwt.JwtUtils;
import com.wellcare.wellcare.Exceptions.UserException;
import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Repositories.UserRepository;

@Service
public class UserServiceImplementation implements UserService {

    @Autowired
    private UserRepository userRepository;

    
    @Autowired
    private JwtUtils jwtUtils; 

    @SuppressWarnings("null")
    @Override
    public User findUserById(Long userId) throws UserException {
        Optional<User> opt = userRepository.findById(userId);
        if(opt.isPresent()) return opt.get();

        throw new UserException("User with "+ userId +" is not found.");
    }

    @Override
    public User findUserProfile(String token) throws UserException {
        String username = jwtUtils.getUserNameFromJwtToken(token);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserException("User with username: " + username + " is not found."));

        return user;
    }

  
    @Override
    public User findUserByUsername(String username) throws UserException {

        Optional<User> user = userRepository.findByUsername(username);
        if(user.isPresent()) return user.get();

        throw new UserException("Username is not found.");
    }

    @Override
    public String followUser(Long requestId, Long followUserId) throws UserException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'followUser'");
    }

    @Override
    public String unfollowUser(Long requestId, Long followUserId) throws UserException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'unfollowUser'");
    }

    @Override
    public List<User> findUserByIds(List<Long> userIds) throws UserException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findUserByIds'");
    }

    @Override
    public List<User> searchUser(String query) throws UserException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'searchUser'");
    }

    @Override
    public User updateUserProfile(User updatedUser, User user) throws UserException {
        if(updatedUser.getEmail() != null) user.setEmail(updatedUser.getEmail());
        if(updatedUser.getBio() != null) user.setBio(updatedUser.getBio());
        if(updatedUser.getFirstName() != null) user.setFirstName(updatedUser.getFirstName());
        if(updatedUser.getLastName() != null) user.setLastName(updatedUser.getLastName());
        if(updatedUser.getUsername() != null) user.setUsername(updatedUser.getUsername());
        if(updatedUser.getMobile() != null) user.setMobile(updatedUser.getMobile());
        if(updatedUser.getGender() != null) user.setGender(updatedUser.getGender());
        if(updatedUser.getImage() != null) user.setImage(updatedUser.getImage());
        if(updatedUser.getId().equals(user.getId())) return userRepository.save(user);

        throw new UserException("You cannot update this user!");
    }
    
}
