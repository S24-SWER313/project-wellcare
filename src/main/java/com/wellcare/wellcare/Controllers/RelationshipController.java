package com.wellcare.wellcare.Controllers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wellcare.wellcare.Assemblers.RelationshipModelAssembler;
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

        @Autowired
        RelationshipModelAssembler relationshipModelAssembler;

        /*
         * relationship status = 0 => friend request is pending
         * relationship status = 1 => friend request is accepted, user1 and user2 are
         * friend
         * relationship status = 2 => friend request is canceled, only if status = 0
         * 1. addFriend: user1 sends friend request to user2, so relationship status = 0
         * (pending request)
         * 2. cancelFriendshipRequest: user1 or user2 can cancel the friend request, so
         * the status will equal 2, row won't be deleted from DB => only used if status
         * = 0
         * 3. getFriendRequests: user1 or user2 can see their friend requests, where
         * relationship status = 0
         * 4. acceptFriend: if the status = 0, user1 or user2 can accept the friend
         * request came to them, so the status will become 1 (request accepted)
         * 5. removeFriend: if status = 1, user1 or user2 can unfriend the users from
         * their friends list, so the unfriended user will be removed from the DB =>
         * only used if status = 1
         */

        // sends a friend request to another user
        @PutMapping("/new-friend/{friendUsername}")
        public ResponseEntity<EntityModel<MessageResponse>> addFriend(HttpServletRequest request,
                        @PathVariable String friendUsername) {
                try {
                        String jwtToken = authTokenFilter.parseJwt(request);
                        Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);
                        String username = jwtUtils.getUserNameFromJwtToken(jwtToken);

                        if (username.equals(friendUsername)) {
                                return ResponseEntity.badRequest()
                                                .body(EntityModel.of(new MessageResponse(
                                                                "You cannot add yourself as a friend")));
                        }

                        User loggedInUser = userRepository.findById(userId)
                                        .orElseThrow(() -> new UserException("User not found"));

                        Optional<User> friendCandidateUserOptional = userRepository.findByUsername(friendUsername);

                        if (friendCandidateUserOptional.isEmpty()) {
                                return ResponseEntity.badRequest()
                                                .body(EntityModel.of(new MessageResponse("Friend user not found")));
                        }

                        User friendCandidateUser = friendCandidateUserOptional.get();

                        // Check if a relationship already exists
                        Relationship relationshipFromDb = relationshipRepository
                                        .findRelationshipByUserOneUsernameAndUserTwoUsername(loggedInUser.getUsername(),
                                                        friendCandidateUser.getUsername());

                        if (relationshipFromDb != null) {
                                if (relationshipFromDb.getStatus() == 1) {
                                        return ResponseEntity.badRequest()
                                                        .body(EntityModel.of(new MessageResponse(
                                                                        "You're already friends with this user")));
                                } else if (relationshipFromDb.getStatus() == 0) {
                                        relationshipFromDb.setStatus(2); // Cancel the friend request
                                        relationshipRepository.save(relationshipFromDb);
                                        return ResponseEntity.ok(EntityModel
                                                        .of(new MessageResponse("Friend request cancelled")));
                                } else if (relationshipFromDb.getStatus() == 2) {
                                        // Resetting the relationship status to 0 and updating actionUser
                                        relationshipFromDb.setStatus(0);
                                        relationshipFromDb.setActionUser(loggedInUser);
                                        relationshipFromDb.setUserOne(loggedInUser);
                                        relationshipFromDb.setUserTwo(friendCandidateUser);
                                        relationshipRepository.save(relationshipFromDb);
                                        return ResponseEntity.ok(EntityModel
                                                        .of(new MessageResponse("Friend request sent successfully")));

                                }
                        } else {
                                // Create a new relationship entry
                                Relationship newRelationship = new Relationship();
                                newRelationship.setActionUser(loggedInUser);
                                newRelationship.setUserOne(loggedInUser); // User1 is the one sending the request
                                newRelationship.setUserTwo(friendCandidateUser); // User2 is the one receiving the
                                                                                 // request
                                newRelationship.setStatus(0);
                                newRelationship.setTime(LocalDateTime.now());
                                relationshipRepository.save(newRelationship);
                                return ResponseEntity.ok(EntityModel
                                                .of(new MessageResponse("Friend request sent successfully")));

                        }

                        MessageResponse messageResponse = new MessageResponse("Friend request sent successfully");
                        EntityModel<MessageResponse> entityModel = EntityModel.of(messageResponse);
                        entityModel.add(linkTo(methodOn(RelationshipController.class).addFriend(request,
                                        friendUsername)).withSelfRel());
                        entityModel.add(linkTo(methodOn(RelationshipController.class)
                                        .cancelFriendshipRequest(request, friendUsername))
                                        .withRel("cancelFriendshipRequest"));

                        return ResponseEntity.ok(entityModel);

                } catch (UserException e) {
                        return ResponseEntity.badRequest().body(EntityModel.of(new MessageResponse(e.getMessage())));
                } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(EntityModel.of(
                                                        new MessageResponse("Error adding friend: " + e.getMessage())));
                }
        }

        // cancel a sent friend request
        @PutMapping("/friend-request-cancel/{friendUsername}")
        public ResponseEntity<EntityModel<MessageResponse>> cancelFriendshipRequest(HttpServletRequest request,
                        @PathVariable String friendUsername) {
                try {
                        String jwtToken = authTokenFilter.parseJwt(request);
                        Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);

                        User loggedInUser = userRepository.findById(userId)
                                        .orElseThrow(() -> new UserException("User not found"));

                        if (friendUsername == null) {
                                return ResponseEntity.badRequest()
                                                .body(EntityModel.of(
                                                                new MessageResponse("Friend user name is required")));
                        }

                        // Check if a friend request exists between the two users
                        Relationship relationship = relationshipRepository
                                        .findRelationshipByUserOneUsernameAndUserTwoUsername(loggedInUser.getUsername(),
                                                        friendUsername);

                        // If no relationship is found, check the other way around
                        if (relationship == null) {
                                relationship = relationshipRepository
                                                .findRelationshipByUserOneUsernameAndUserTwoUsername(friendUsername,
                                                                loggedInUser.getUsername());
                        }

                        if (relationship == null || relationship.getStatus() != 0) {
                                return ResponseEntity.badRequest()
                                                .body(EntityModel.of(
                                                                new MessageResponse("No friend request to cancel")));
                        }

                        // Set status to 2 to indicate canceled
                        relationship.setStatus(2);
                        relationshipRepository.save(relationship);

                        MessageResponse messageResponse = new MessageResponse("Friend request cancelled successfully");
                        EntityModel<MessageResponse> entityModel = EntityModel.of(messageResponse);
                        entityModel.add(linkTo(methodOn(RelationshipController.class)
                                        .cancelFriendshipRequest(request, friendUsername)).withSelfRel());
                        entityModel.add(linkTo(
                                        methodOn(RelationshipController.class).addFriend(request, friendUsername))
                                        .withRel("addFriend"));

                        return ResponseEntity.ok(entityModel);

                } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(EntityModel.of(new MessageResponse(
                                                        "Error cancelling friend request: " + e.getMessage())));
                }
        }

        @GetMapping("/friend-requests")
        public ResponseEntity<?> getFriendRequests(HttpServletRequest request) {
                try {
                        String jwtToken = authTokenFilter.parseJwt(request);
                        Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);

                        User loggedInUser = userRepository.findById(userId)
                                        .orElseThrow(() -> new UserException("User not found"));

                        // Get only friend requests sent to the user
                        List<Relationship> receivedFriendRequests = relationshipRepository
                                        .findRelationshipsByUserTwoIdAndStatus(userId, 0);

                        List<EntityModel<Relationship>> relationshipEntityModels = receivedFriendRequests.stream()
                                        .map(relationshipModelAssembler::toModel)
                                        .collect(Collectors.toList());

                        CollectionModel<EntityModel<Relationship>> collectionModel = CollectionModel
                                        .of(relationshipEntityModels);
                        collectionModel.add(linkTo(methodOn(RelationshipController.class).getFriendRequests(request))
                                        .withSelfRel());
                        return ResponseEntity.ok(collectionModel);

                } catch (Exception e) {
                        EntityModel<MessageResponse> errorModel = EntityModel.of(new MessageResponse(
                                        "Error retrieving friend requests: " + e.getMessage()));
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CollectionModel.of(errorModel));
                }
        }

        // accept a friend request from another user
        @PutMapping("/friend-accept/{friendUsername}")
        public ResponseEntity<EntityModel<MessageResponse>> acceptFriend(HttpServletRequest request,
                        @PathVariable String friendUsername) {
                try {
                        String jwtToken = authTokenFilter.parseJwt(request);
                        Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);

                        User loggedInUser = userRepository.findById(userId)
                                        .orElseThrow(() -> new UserException("User not found"));

                        if (friendUsername == null) {
                                return ResponseEntity.badRequest()
                                                .body(EntityModel
                                                                .of(new MessageResponse("Friend user ID is required")));
                        }

                        if (userId.equals(friendUsername)) {
                                return ResponseEntity.badRequest()
                                                .body(EntityModel.of(new MessageResponse(
                                                                "You cannot accet yourself as a friend")));
                        }

                        Optional<User> friendUserOptional = userRepository.findByUsername(friendUsername);

                        if (friendUserOptional.isEmpty()) {
                                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                                .body(EntityModel.of(new MessageResponse("Friend user not found")));
                        }

                        boolean isFriendshipAccepted = changeStatusAndSave(loggedInUser.getUsername(), friendUsername,
                                        0, 1);

                        if (isFriendshipAccepted) {
                                User friendUser = friendUserOptional.get();

                                loggedInUser.getFriends().add(friendUser);
                                friendUser.getFriends().add(loggedInUser);

                                loggedInUser.incrementFriendsNumber();
                                friendUser.incrementFriendsNumber();

                                userRepository.save(loggedInUser);
                                userRepository.save(friendUser);
                        }

                        MessageResponse messageResponse = isFriendshipAccepted
                                        ? new MessageResponse("Friend request accepted successfully")
                                        : new MessageResponse("Failed to accept friend request");
                        EntityModel<MessageResponse> entityModel = EntityModel.of(messageResponse);
                        entityModel.add(
                                        linkTo(methodOn(RelationshipController.class).acceptFriend(request,
                                                        friendUsername)).withSelfRel());
                        entityModel.add(linkTo(
                                        methodOn(RelationshipController.class).removeFriend(request, friendUsername))
                                        .withRel("removeFriend"));

                        return ResponseEntity.ok(entityModel);
                } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(EntityModel.of(new MessageResponse(
                                                        "Error accepting friend: " + e.getMessage())));
                }
        }

        // remove friend from user's friend list
        @DeleteMapping("/friends/{friendUsername}")
        public ResponseEntity<EntityModel<MessageResponse>> removeFriend(HttpServletRequest request,
                        @PathVariable String friendUsername) {
                try {
                        String jwtToken = authTokenFilter.parseJwt(request);
                        Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);

                        User loggedInUser = userRepository.findById(userId)
                                        .orElseThrow(() -> new UserException("User not found"));

                        if (friendUsername == null) {
                                return ResponseEntity.badRequest()
                                                .body(EntityModel.of(new MessageResponse(
                                                                "Friend user ID is required in the URL")));
                        }

                        Relationship relationship = relationshipRepository
                                        .findRelationshipByUserOneUsernameAndUserTwoUsername(loggedInUser.getUsername(),
                                                        friendUsername);

                        if (relationship != null) {
                                relationshipRepository.delete(relationship);

                                User friendUser = userRepository.findByUsername(friendUsername)
                                                .orElseThrow(() -> new UserException("Friend user not found"));

                                loggedInUser.removeFriend(friendUser);
                                friendUser.removeFriend(loggedInUser);

                                loggedInUser.decrementFriendsNumber();
                                friendUser.decrementFriendsNumber();

                                userRepository.save(loggedInUser);
                                userRepository.save(friendUser);

                                MessageResponse messageResponse = new MessageResponse("Friend removed successfully");
                                EntityModel<MessageResponse> entityModel = EntityModel.of(messageResponse);
                                entityModel.add(
                                                linkTo(methodOn(RelationshipController.class).removeFriend(request,
                                                                friendUsername)).withSelfRel());
                                entityModel.add(linkTo(methodOn(RelationshipController.class).addFriend(request,
                                                friendUsername))
                                                .withRel("addFriend"));

                                return ResponseEntity.ok(entityModel);
                        } else {
                                return ResponseEntity.badRequest()
                                                .body(EntityModel.of(new MessageResponse("Failed to remove friend")));
                        }

                } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(EntityModel.of(new MessageResponse(
                                                        "Error removing friend: " + e.getMessage())));
                }
        }

        @GetMapping("/friend-request-status/{friendUsername}")
        public ResponseEntity<MessageResponse> getFriendRequestStatus(HttpServletRequest request,
                        @PathVariable String friendUsername) {
                try {
                        String jwtToken = authTokenFilter.parseJwt(request);
                        Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);

                        User loggedInUser = userRepository.findById(userId)
                                        .orElseThrow(() -> new UserException("User not found"));

                        if (friendUsername == null) {
                                return ResponseEntity.badRequest()
                                                .body(new MessageResponse("Friend username is required"));
                        }

                        Relationship relationship = relationshipRepository
                                        .findRelationshipByUserOneUsernameAndUserTwoUsername(loggedInUser.getUsername(),
                                                        friendUsername);

                        if (relationship != null) {
                                if (relationship.getStatus() == 0) {
                                        if (relationship.getActionUser().getUsername().equals(friendUsername)) {
                                                return ResponseEntity.ok()
                                                                .body(new MessageResponse("Friend request received"));
                                        } else {
                                                return ResponseEntity.ok().body(
                                                                new MessageResponse("Friend request already sent"));
                                        }
                                } else if (relationship.getStatus() == 1) {
                                        return ResponseEntity.ok().body(new MessageResponse("Already friends"));
                                }
                        }
                        return ResponseEntity.ok().body(new MessageResponse("Friend request not sent"));
                } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new MessageResponse(
                                                        "Error checking friend request status: " + e.getMessage()));
                }
        }

        private boolean changeStatusAndSave(String loggedInUsername, String friendusername, int fromStatus,
                        int toStatus)
                        throws Exception {
                Relationship relationship = relationshipRepository
                                .findRelationshipByUserOneUsernameAndUserTwoUsername(loggedInUsername, friendusername);

                if (relationship == null || relationship.getStatus() != fromStatus) {
                        throw new Exception("Invalid relationship status");
                }

                relationship.setStatus(toStatus);
                relationship.setTime(LocalDateTime.now());

                Relationship savedRelationship = relationshipRepository.save(relationship);

                return savedRelationship != null;
        }

        @GetMapping("/{userId}/friends")
        public ResponseEntity<?> getUserFriends(@PathVariable Long userId) {
                try {
                        Optional<User> userOptional = userRepository.findById(userId);

                        if (userOptional.isEmpty()) {
                                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                                .body(new MessageResponse("User not found"));
                        }

                        User user = userOptional.get();
                        List<User> friends = user.getFriends();

                        return ResponseEntity.ok(friends);
                } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new MessageResponse("Error fetching user friends: " + e.getMessage()));
                }
        }

        @GetMapping("/count/{username}")
        public ResponseEntity<?> getUserFriendsCount(@PathVariable String username) {
                try {
                        Optional<User> userOptional = userRepository.findByUsername(username);

                        if (userOptional.isEmpty()) {
                                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                                .body(new MessageResponse("User not found"));
                        }

                        User user = userOptional.get();
                        long friendsCount = relationshipRepository.countFriends(user);

                        return ResponseEntity.ok(Map.of("friendsCount", friendsCount));
                } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new MessageResponse(
                                                        "Error fetching user friends count: " + e.getMessage()));
                }
        }
}