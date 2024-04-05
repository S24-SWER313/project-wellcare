package com.wellcare.wellcare.Controllers;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wellcare.wellcare.Exceptions.MessageException;
import com.wellcare.wellcare.Exceptions.UserException;
import com.wellcare.wellcare.Models.Message;
import com.wellcare.wellcare.Models.Relationship;
import com.wellcare.wellcare.Models.User;
import com.wellcare.wellcare.Repositories.MessageRepository;
import com.wellcare.wellcare.Repositories.RelationshipRepository;
import com.wellcare.wellcare.Repositories.UserRepository;
import com.wellcare.wellcare.Security.jwt.AuthTokenFilter;
import com.wellcare.wellcare.Security.jwt.JwtUtils;
import com.wellcare.wellcare.Storage.StorageService;
import com.wellcare.wellcare.payload.response.MessageResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/message")
public class MessageController {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private RelationshipRepository relationshipRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    JwtUtils jwtUtils;
    @Autowired
    AuthTokenFilter authTokenFilter;

    @Autowired
    StorageService storageService;

    @Transactional
@PostMapping("/sending/{userId}")
public ResponseEntity<MessageResponse> sendMessage(@PathVariable Long userId, 
                                                   @ModelAttribute Message message,  
                                                   @RequestParam(value = "file", required = false) MultipartFile[] files,
                                                   HttpServletRequest request) throws UserException {
    try {
        String jwtToken = authTokenFilter.parseJwt(request);
        System.out.println("Extracted JWT token: " + jwtToken);

        // Parse the JWT token to extract the userId
        Long loggedInUserId = jwtUtils.getUserIdFromJwtToken(jwtToken);
        System.out.println("Extracted userId: " + loggedInUserId);

        // Retrieve the user entity or throw exception if not found
        User loggedInUser = userRepository.findById(loggedInUserId)
                .orElseThrow(() -> new UserException("User not found"));

        String content = message.getContent();

        if (content == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Content is required."));
        }

        List<String> attachmentUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            System.out.println("Received file: " + file.getOriginalFilename());
            storageService.store(file);
            String filename = file.getOriginalFilename();
            String url = "http://localhost:8080/files/" + filename;
            attachmentUrls.add(url);
        }

        User fromUser = userRepository
                .findByUsername(loggedInUser.getUsername())
                .orElseThrow(() -> new UserException("User not found"));

        User toUser = userRepository.findById(userId)
                .orElseThrow(() -> new UserException("Recipient not found"));

        Relationship relationship = relationshipRepository
                .findRelationshipByUserOneIdAndUserTwoId(fromUser.getId(), userId);

        if (relationship == null || relationship.getStatus() != 1) {
            return ResponseEntity.badRequest().body(new MessageResponse("No valid relationship found with the recipient."));
        }

        message.setContent(content);
        message.setFromUser(fromUser);
        message.setToUser(toUser);
        message.setRelationship(relationship);
        message.setTime(LocalDateTime.now());
        message.setAttachment(attachmentUrls);

        Message savedMessage = messageRepository.save(message);

        if (savedMessage != null) {
            return ResponseEntity.ok(new MessageResponse("Message sent successfully", savedMessage));
        }

        throw new MessageException("Error sending message");

    } catch (NumberFormatException e) {
        return ResponseEntity.badRequest().body(new MessageResponse("Invalid user ID format"));
    } catch (MessageException e) {
        // Log the actual error message for debugging
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse(e.getMessage()));
    }
}



    @GetMapping("/{chatUserId}")
    public ResponseEntity<MessageResponse> getAllMessagesWithUser(@PathVariable Long chatUserId, HttpServletRequest request) throws UserException {
        try {
            String jwtToken = authTokenFilter.parseJwt(request);
            System.out.println("Extracted JWT token: " + jwtToken);

            // Parse the JWT token to extract the userId
            Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);
            System.out.println("Extracted userId: " + userId);

            // Retrieve the user entity or throw exception if not found
            User loggedInUser = userRepository.findById(userId)
                    .orElseThrow(() -> new UserException("User not found"));

            User chatUser = userRepository.findById(chatUserId)
                    .orElseThrow(() -> new UserException("Chat user not found"));

            List<Message> allMessagesBetweenTwoUsers = messageRepository
                    .findAllMessagesBetweenTwoUsers(loggedInUser.getId(), chatUserId);

            // Fetch the messageList eagerly
            allMessagesBetweenTwoUsers.forEach(message -> Hibernate.initialize(message.getRelationship().getMessageList()));

            this.updateMessageStatus(loggedInUser.getId(), chatUserId);

            return ResponseEntity.ok(new MessageResponse("Messages retrieved successfully", allMessagesBetweenTwoUsers));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse(e.getMessage()));
        }
    }

    @GetMapping("/unread")
    public ResponseEntity<MessageResponse> getAllFriendMessages(HttpServletRequest request) {
        try {
            String jwtToken = authTokenFilter.parseJwt(request);
            System.out.println("Extracted JWT token: " + jwtToken);

            // Parse the JWT token to extract the userId
            Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);
            System.out.println("Extracted userId: " + userId);

            // Retrieve the user entity or throw exception if not found
            User loggedInUser = userRepository.findById(userId)
                    .orElseThrow(() -> new UserException("User not found"));

            List<Message> allUnreadMessages = messageRepository.getAllUnreadMessages(loggedInUser.getId());

            List<Message> allFriendsMessages = allUnreadMessages.stream()
                    .filter(message -> message.getRelationship().getStatus() == 1)
                    .collect(Collectors.toList());

          
            return ResponseEntity.ok(new MessageResponse("Friend messages retrieved successfully", allFriendsMessages));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse(e.getMessage()));
        }
    }

    private void updateMessageStatus(Long loggedInUserId, Long friendUserId) {
        messageRepository.updateStatusFromReadMessages(loggedInUserId, friendUserId);
    }

   

}
