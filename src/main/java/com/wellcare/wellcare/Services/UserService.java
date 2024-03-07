package com.wellcare.wellcare.Services;

import com.wellcare.wellcare.Exceptions.UserException;
import com.wellcare.wellcare.Models.User;
import java.util.*;
public interface UserService {

    public User findUserById (Long userId) throws UserException;
    public User findUserProfile (String username) throws UserException;
    public User findUserByUsername (String token) throws UserException;
    public String followUser (Long requestId, Long followUserId) throws UserException;
    public String unfollowUser (Long requestId, Long followUserId) throws UserException;
    public List<User> findUserByIds(List<Long> userIds) throws UserException;
    public List<User> searchUser(String query) throws UserException;
    public User updateUserProfile(User updatedUser, User user) throws UserException;
    

}
