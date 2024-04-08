package com.wellcare.wellcare.Controllers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

        // sends a friend request to another user
        @PutMapping("/adding/{friendUserId}")
        public ResponseEntity<EntityModel<MessageResponse>> addFriend(HttpServletRequest request,
                        @PathVariable Long friendUserId) {
            try {
                String jwtToken = authTokenFilter.parseJwt(request);
                Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);
        
                if (userId.equals(friendUserId)) {
                    return ResponseEntity.badRequest()
                                    .body(EntityModel.of(new MessageResponse("You cannot add yourself as a friend")));
                }
        
                User loggedInUser = userRepository.findById(userId)
                                .orElseThrow(() -> new UserException("User not found"));
        
                Optional<User> friendCandidateUserOptional = userRepository.findById(friendUserId);
        
                if (friendCandidateUserOptional.isEmpty()) {
                    return ResponseEntity.badRequest()
                                    .body(EntityModel.of(new MessageResponse("Friend user not found")));
                }
        
                User friendCandidateUser = friendCandidateUserOptional.get();
        
                Relationship relationshipFromDb = relationshipRepository
                                .findRelationshipByUserOneIdAndUserTwoId(loggedInUser.getId(), friendUserId);
        
                if (relationshipFromDb != null) {
                    if (relationshipFromDb.getStatus() == 1) {
                        return ResponseEntity.badRequest()
                                        .body(EntityModel.of(new MessageResponse("You're already friends with this user")));
                    } else if (relationshipFromDb.getStatus() == 0) {
                        return ResponseEntity.badRequest()
                                        .body(EntityModel.of(new MessageResponse("Friend request already sent")));
                    } else if (relationshipFromDb.getStatus() == 2) {
                        // Resetting the relationship status to 0
                        relationshipFromDb.setStatus(0);
                        relationshipRepository.save(relationshipFromDb);
                    }
                } else {
                    Relationship newRelationship = new Relationship();
                    newRelationship.setActionUser(loggedInUser);
                    newRelationship.setUserOne(loggedInUser);
                    newRelationship.setUserTwo(friendCandidateUser);
                    newRelationship.setStatus(0);
                    newRelationship.setTime(LocalDateTime.now());
        
                    relationshipRepository.save(newRelationship);
                }
        
                MessageResponse messageResponse = new MessageResponse("Friend request sent successfully");
                EntityModel<MessageResponse> entityModel = EntityModel.of(messageResponse);
                entityModel.add(linkTo(methodOn(RelationshipController.class).addFriend(request,
                                friendUserId)).withSelfRel());
                entityModel.add(linkTo(methodOn(RelationshipController.class)
                                .cancelFriendshipRequest(request, friendUserId))
                                .withRel("cancelFriendshipRequest"));
        
                return ResponseEntity.ok(entityModel);
        
            } catch (UserException e) {
                return ResponseEntity.badRequest().body(EntityModel.of(new MessageResponse(e.getMessage())));
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(EntityModel.of(new MessageResponse("Error adding friend: " + e.getMessage())));
            }
        }
        
        @GetMapping("/friend-requests")
        public ResponseEntity<?> getFriendRequests(HttpServletRequest request) {
            try {
                String jwtToken = authTokenFilter.parseJwt(request);
                Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);
        
                User loggedInUser = userRepository.findById(userId)
                                .orElseThrow(() -> new UserException("User not found"));
        
                List<Relationship> friendRequests = relationshipRepository
                                .findRelationshipsByUserTwoIdAndStatus(userId, 0);
        
                CollectionModel<EntityModel<Relationship>> collectionModel = 
                                relationshipModelAssembler.toCollectionModel(friendRequests);
                
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
        

        // remove friend from user's friend list
        @DeleteMapping("/removing/{friendUserId}")
public ResponseEntity<EntityModel<MessageResponse>> removeFriend(HttpServletRequest request,
                @PathVariable Long friendUserId) {
    try {
        String jwtToken = authTokenFilter.parseJwt(request);
        Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);

        User loggedInUser = userRepository.findById(userId)
                        .orElseThrow(() -> new UserException("User not found"));

        if (friendUserId == null) {
            return ResponseEntity.badRequest()
                            .body(EntityModel.of(new MessageResponse(
                                            "Friend user ID is required in the URL")));
        }

        // Find and delete the relationship from the database
        Relationship relationship = relationshipRepository
                        .findRelationshipByUserOneIdAndUserTwoId(loggedInUser.getId(), friendUserId);

        if (relationship != null) {
            relationshipRepository.delete(relationship);
            
            User friendUser = userRepository.findById(friendUserId)
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
                                            friendUserId)).withSelfRel());
            entityModel.add(linkTo(methodOn(RelationshipController.class).addFriend(request, friendUserId))
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


        // accept a friend request from another user
        @PutMapping("/friend-accept/{friendUserId}")
        public ResponseEntity<EntityModel<MessageResponse>> acceptFriend(HttpServletRequest request,
                        @PathVariable Long friendUserId) {
            try {
                String jwtToken = authTokenFilter.parseJwt(request);
                Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);
        
                User loggedInUser = userRepository.findById(userId)
                                .orElseThrow(() -> new UserException("User not found"));
        
                if (friendUserId == null) {
                    return ResponseEntity.badRequest()
                                    .body(EntityModel.of(new MessageResponse("Friend user ID is required")));
                }
        
                if (userId.equals(friendUserId)) {
                    return ResponseEntity.badRequest()
                                    .body(EntityModel.of(new MessageResponse("You cannot accet yourself as a friend")));
                }
        
                Optional<User> friendUserOptional = userRepository.findById(friendUserId);
        
                if (friendUserOptional.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body(EntityModel.of(new MessageResponse("Friend user not found")));
                }
        
                boolean isFriendshipAccepted = changeStatusAndSave(loggedInUser.getId(), friendUserId, 0, 1);
        
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
                                                friendUserId)).withSelfRel());
                entityModel.add(linkTo(
                                methodOn(RelationshipController.class).removeFriend(request, friendUserId))
                                .withRel("removeFriend"));
        
                return ResponseEntity.ok(entityModel);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(EntityModel.of(new MessageResponse(
                                                "Error accepting friend: " + e.getMessage())));
            }
        }
        
        // cancel a sent friend request
        @PutMapping("/friend-cancel/{friendUserId}")
        public ResponseEntity<EntityModel<MessageResponse>> cancelFriendshipRequest(HttpServletRequest request,
                        @PathVariable Long friendUserId) {
            try {
                String jwtToken = authTokenFilter.parseJwt(request);
                Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);
        
                User loggedInUser = userRepository.findById(userId)
                                .orElseThrow(() -> new UserException("User not found"));
        
                if (friendUserId == null) {
                    return ResponseEntity.badRequest()
                                    .body(EntityModel.of(new MessageResponse("Friend user ID is required")));
                }
        
                // Check if a friend request exists between the two users
                Relationship relationship = relationshipRepository
                                .findRelationshipByUserOneIdAndUserTwoId(loggedInUser.getId(), friendUserId);
        
                // If no relationship is found, check the other way around
                if (relationship == null) {
                    relationship = relationshipRepository
                                    .findRelationshipByUserOneIdAndUserTwoId(friendUserId, loggedInUser.getId());
                }
        
                if (relationship == null || relationship.getStatus() != 0) {
                    return ResponseEntity.badRequest()
                                    .body(EntityModel.of(new MessageResponse("No friend request to cancel")));
                }
        
                // Set status to 2 to indicate canceled
                relationship.setStatus(2);
                relationshipRepository.save(relationship);
        
                MessageResponse messageResponse = new MessageResponse("Friend request cancelled successfully");
                EntityModel<MessageResponse> entityModel = EntityModel.of(messageResponse);
                entityModel.add(linkTo(methodOn(RelationshipController.class)
                                .cancelFriendshipRequest(request, friendUserId)).withSelfRel());
                entityModel.add(linkTo(methodOn(RelationshipController.class).addFriend(request, friendUserId))
                                .withRel("addFriend"));
        
                return ResponseEntity.ok(entityModel);
        
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(EntityModel.of(new MessageResponse("Error cancelling friend request: " + e.getMessage())));
            }
        }
        
        

        private boolean changeStatusAndSave(Long loggedInUserId, Long friendId, int fromStatus, int toStatus)
                        throws Exception {
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
