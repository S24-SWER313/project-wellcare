package com.wellcare.wellcare.Controllers;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wellcare.wellcare.Exceptions.UserException;
import com.wellcare.wellcare.Models.Relationship;
import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Repositories.RelationshipRepository;
import com.wellcare.wellcare.Repositories.UserRepository;
import com.wellcare.wellcare.Security.jwt.AuthTokenFilter;
import com.wellcare.wellcare.Security.jwt.JwtUtils;
import com.wellcare.wellcare.payload.response.MessageResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/relationship")
public class RelationshipController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RelationshipRepository relationshipRepository;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    AuthTokenFilter authTokenFilter;

    //sends a friend request to another user
    @PutMapping("/adding/{friendUserId}")
    public ResponseEntity<MessageResponse> addFriend(HttpServletRequest request, @PathVariable Long friendUserId) {
        try {
            String jwtToken = authTokenFilter.parseJwt(request);
            System.out.println("Extracted JWT token: " + jwtToken);

            // Parse the JWT token to extract the userId
            Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);
            System.out.println("Extracted userId: " + userId);

            // Retrieve the user entity or throw exception if not found
            User loggedInUser = userRepository.findById(userId)
                    .orElseThrow(() -> new UserException("User not found"));

            if (friendUserId == null) {
                return ResponseEntity.badRequest().body(new MessageResponse("Friend user ID is required in the URL"));
            }

            User friendCandidateUser = userRepository.findById(friendUserId)
                    .orElseThrow(() -> new UserException("Friend user not found"));

            Relationship relationshipFromDb = relationshipRepository
                    .findRelationshipByUserOneIdAndUserTwoId(loggedInUser.getId(), friendUserId);

            Relationship newRelationship;
            if (relationshipFromDb == null) {
                newRelationship = new Relationship();
                newRelationship.setActionUser(loggedInUser);
                newRelationship.setUserOne(loggedInUser);
                newRelationship.setUserTwo(friendCandidateUser);
                newRelationship.setStatus(0);
                newRelationship.setTime(LocalDateTime.now());
            } else {
                newRelationship = relationshipFromDb;
                newRelationship.setActionUser(loggedInUser);
                newRelationship.setStatus(0);
                newRelationship.setTime(LocalDateTime.now());
            }

            Relationship savedRelationship = relationshipRepository.save(newRelationship);
            return ResponseEntity.ok(savedRelationship != null ? new MessageResponse("Friend request sent successfully") : new MessageResponse("Failed to send friend request"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("Error adding friend: " + e.getMessage()));
        }
    }


//remove friend from user's friend list
@DeleteMapping("/removing/{friendUserId}")
public ResponseEntity<MessageResponse> removeFriend(HttpServletRequest request, @PathVariable Long friendUserId) {
    try {
        String jwtToken = authTokenFilter.parseJwt(request);
        System.out.println("Extracted JWT token: " + jwtToken);

        // Parse the JWT token to extract the userId
        Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);
        System.out.println("Extracted userId: " + userId);

        // Retrieve the user entity or throw exception if not found
        User loggedInUser = userRepository.findById(userId)
                .orElseThrow(() -> new UserException("User not found"));

        if (friendUserId == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Friend user ID is required in the URL"));
        }

        boolean isFriendRemoved = changeStatusAndSave(loggedInUser.getId(), friendUserId, 1, 2);

        if (isFriendRemoved) {
            User friendUser = userRepository.findById(friendUserId)
                    .orElseThrow(() -> new UserException("Friend user not found"));

            loggedInUser.getFriends().remove(friendUser);
            friendUser.getFriends().remove(loggedInUser);

            loggedInUser.decrementFriendsNumber();
            friendUser.decrementFriendsNumber();

            userRepository.save(loggedInUser);
            userRepository.save(friendUser);
        }

        return ResponseEntity.ok(isFriendRemoved ? new MessageResponse("Friend removed successfully") : new MessageResponse("Failed to remove friend"));

    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("Error removing friend: " + e.getMessage()));
    }
}

  
    //accept a friend request from another user
    @PutMapping("/friend-accept/{friendUserId}")
    public ResponseEntity<MessageResponse> acceptFriend(HttpServletRequest request, @PathVariable Long friendUserId) {
        try {
            String jwtToken = authTokenFilter.parseJwt(request);
            System.out.println("Extracted JWT token: " + jwtToken);

            // Parse the JWT token to extract the userId
            Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);
            System.out.println("Extracted userId: " + userId);

            // Retrieve the user entity or throw exception if not found
            User loggedInUser = userRepository.findById(userId)
                    .orElseThrow(() -> new UserException("User not found"));

            if (friendUserId == null) {
                return ResponseEntity.badRequest().body(new MessageResponse("Friend user ID is required"));
            }

            boolean isFriendshipAccepted = changeStatusAndSave(loggedInUser.getId(), friendUserId, 0, 1);

            // If friendship is accepted, add each user to the other's friend list
            if (isFriendshipAccepted) {
                User friendUser = userRepository.findById(friendUserId)
                        .orElseThrow(() -> new UserException("Friend user not found"));

                loggedInUser.getFriends().add(friendUser);
                friendUser.getFriends().add(loggedInUser);

                loggedInUser.incrementFriendsNumber();
                friendUser.incrementFriendsNumber();

                userRepository.save(loggedInUser);
                userRepository.save(friendUser);
            }

            return ResponseEntity.ok(isFriendshipAccepted ? new MessageResponse("Friend request accepted successfully") : new MessageResponse("Failed to accept friend request"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("Error accepting friend: " + e.getMessage()));
        }
    }
    
   
    //cancel a sent friend request
    @PutMapping("/friend-cancel/{friendUserId}")
    public ResponseEntity<MessageResponse> cancelFriendshipRequest(HttpServletRequest request, @PathVariable Long friendUserId) {
        try {
            String jwtToken = authTokenFilter.parseJwt(request);
            System.out.println("Extracted JWT token: " + jwtToken);

            // Parse the JWT token to extract the userId
            Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);
            System.out.println("Extracted userId: " + userId);

            // Retrieve the user entity or throw exception if not found
            User loggedInUser = userRepository.findById(userId)
                    .orElseThrow(() -> new UserException("User not found"));

            if (friendUserId == null) {
                return ResponseEntity.badRequest().body(new MessageResponse("Friend user ID is required"));
            }

            return ResponseEntity.ok(changeStatusAndSave(loggedInUser.getId(), friendUserId, 0, 2) ? new MessageResponse("Friend request cancelled successfully") : new MessageResponse("Failed to cancel friend request"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("Error cancelling friend request: " + e.getMessage()));
        }
    }
    private boolean changeStatusAndSave(Long loggedInUserId, Long friendId, int fromStatus, int toStatus) throws Exception {
        Relationship relationship = relationshipRepository
                .findRelationshipByUserOneIdAndUserTwoId(loggedInUserId, friendId);

        if (relationship == null || relationship.getStatus() != fromStatus) {
            throw new Exception("Invalid relationship status");
        }

        relationship.setStatus(toStatus);
        relationship.setTime(LocalDateTime.now());

        Relationship savedRelationship = relationshipRepository.save(relationship);

        return savedRelationship != null;
    }
    
}
