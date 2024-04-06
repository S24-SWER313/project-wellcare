package com.wellcare.wellcare.Controllers;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;
import com.wellcare.wellcare.Assemblers.MessageModelAssembler;
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

    @Autowired
    MessageModelAssembler messageModelAssembler;


    @Transactional
    @PostMapping("/sending/{userId}")
    public ResponseEntity<EntityModel<Message>> sendMessage(@PathVariable Long userId,
                                                             @ModelAttribute Message message,
                                                             @RequestParam(value = "file", required = false) MultipartFile[] files,
                                                             HttpServletRequest request) {
        try {
            String jwtToken = authTokenFilter.parseJwt(request);
            Long loggedInUserId = jwtUtils.getUserIdFromJwtToken(jwtToken);
    
            User loggedInUser = userRepository.findById(loggedInUserId)
                    .orElseThrow(() -> new UserException("User not found"));
    
            User toUser = userRepository.findById(userId)
                    .orElseThrow(() -> new UserException("Recipient not found"));
    
            Relationship relationship = relationshipRepository
                    .findRelationshipByUserOneIdAndUserTwoId(loggedInUserId, userId);
    
            if (relationship == null) {
                throw new UserException("No valid relationship found with the recipient. The friend request is still pending.");
            }
    
            List<String> attachmentUrls = storeAttachments(files);
    
            message.setContent(message.getContent());
            message.setFromUser(loggedInUser);
            message.setToUser(toUser);
            message.setTime(LocalDateTime.now());
            message.setAttachment(attachmentUrls);
            message.setRelationship(relationship);
    
            Message savedMessage = messageRepository.save(message);
    
            if (savedMessage != null) {
                EntityModel<Message> messageModel = messageModelAssembler.toModel(savedMessage);
                return ResponseEntity.ok(messageModel);
            }
    
            throw new MessageException("Error sending message");
    
        } catch (NumberFormatException e) {
            Link link = linkTo(methodOn(MessageController.class).getUnreadMessages(request, null)).withSelfRel();
            EntityModel<Message> errorModel = EntityModel.of(new Message(), link);
            return ResponseEntity.badRequest().body(errorModel);
        } catch (UserException e) {
            Link link = linkTo(methodOn(MessageController.class).getUnreadMessages(request, null)).withSelfRel();
            EntityModel<Message> errorModel = EntityModel.of(new Message(), link);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorModel);
        } catch (MessageException e) {
            Link link = linkTo(methodOn(MessageController.class).getUnreadMessages(request, null)).withSelfRel();
            EntityModel<Message> errorModel = EntityModel.of(new Message(), link);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorModel);
        }
    }
    

    @PutMapping("/update/{messageId}")
    public ResponseEntity<EntityModel<Message>> updateMessage(@PathVariable Long messageId,
                                                               @ModelAttribute Message updatedMessage,
                                                               HttpServletRequest request) {
        try {
            String jwtToken = authTokenFilter.parseJwt(request);
            Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);
    
            Message message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new MessageException("Message not found"));
    
            if (!message.getFromUser().getId().equals(userId)) {
                Link link = linkTo(methodOn(MessageController.class).getUnreadMessages(request, null)).withSelfRel();
                EntityModel<Message> errorModel = EntityModel.of(new Message(), link);
                return ResponseEntity.badRequest().body(errorModel);
            }
    
            Relationship relationship = message.getRelationship(); // Get the relationship from the existing message
            if (relationship == null) {
                Link link = linkTo(methodOn(MessageController.class).getUnreadMessages(request, null)).withSelfRel();
                EntityModel<Message> errorModel = EntityModel.of(new Message(), link);
                return ResponseEntity.badRequest().body(errorModel);
            }
    
            message.setContent(updatedMessage.getContent());
    
            Message savedMessage = messageRepository.save(message);
    
            EntityModel<Message> messageModel = messageModelAssembler.toModel(savedMessage);
    
            return ResponseEntity.ok(messageModel);
    
        } catch (Exception e) {
            Link link = linkTo(methodOn(MessageController.class).getUnreadMessages(request, null)).withSelfRel();
            EntityModel<Message> errorModel = EntityModel.of(new Message(), link);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorModel);
        }
    }
    

   
    @GetMapping("/{chatUserId}")
    public ResponseEntity<CollectionModel<EntityModel<Message>>> getMessagesWithUser(@PathVariable Long chatUserId,
                                                                                         HttpServletRequest request) throws UserException {
        try {
            String jwtToken = authTokenFilter.parseJwt(request);
            Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);

            User loggedInUser = userRepository.findById(userId)
                    .orElseThrow(() -> new UserException("User not found"));

            User chatUser = userRepository.findById(chatUserId)
                    .orElseThrow(() -> new UserException("Chat user not found"));

            List<Message> allMessagesBetweenTwoUsers = messageRepository
                    .findAllMessagesBetweenTwoUsers(loggedInUser.getId(), chatUserId);

            allMessagesBetweenTwoUsers.forEach(message -> Hibernate.initialize(message.getRelationship().getMessageList()));

            updateMessageStatus(loggedInUser.getId(), chatUserId);

            List<EntityModel<Message>> messageModels = allMessagesBetweenTwoUsers.stream()
                    .map(messageModelAssembler::toModel)
                    .collect(Collectors.toList());

            CollectionModel<EntityModel<Message>> collectionModel = CollectionModel.of(messageModels,
                    linkTo(methodOn(MessageController.class).getMessagesWithUser(chatUserId, request)).withSelfRel());

            return ResponseEntity.ok(collectionModel);

        } catch (Exception e) {
            e.printStackTrace();
            CollectionModel<EntityModel<Message>> errorModel = CollectionModel.of(
                    Collections.emptyList(),
                    linkTo(methodOn(MessageController.class).getMessagesWithUser(chatUserId, request)).withSelfRel());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorModel);
        }
    }


   
    @GetMapping("/recent")
    public ResponseEntity<CollectionModel<EntityModel<Message>>> getRecentConversations(
            HttpServletRequest request,
            @PageableDefault(size = 10) Pageable pageable) {
        try {
            String jwtToken = authTokenFilter.parseJwt(request);
            Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);
    
            Page<Message> recentConversations = messageRepository.findRecentConversations(userId, pageable);
    
            List<EntityModel<Message>> recentConversationsModel = recentConversations.getContent().stream()
                    .map(messageModelAssembler::toModel)
                    .collect(Collectors.toList());
    
            CollectionModel<EntityModel<Message>> collectionModel = CollectionModel.of(recentConversationsModel,
                    linkTo(methodOn(MessageController.class).getRecentConversations(request, pageable)).withSelfRel());
    
            return ResponseEntity.ok(collectionModel);
    
        } catch (Exception e) {
            e.printStackTrace();
    
            CollectionModel<EntityModel<Message>> errorModel = CollectionModel.of(
                    Collections.emptyList(),
                    linkTo(methodOn(MessageController.class).getRecentConversations(request, pageable)).withSelfRel());
    
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorModel);
        }
    }

    
    @GetMapping("/unread")
    public ResponseEntity<CollectionModel<EntityModel<Message>>> getUnreadMessages(HttpServletRequest request, @PageableDefault(size = 10) Pageable pageable) {
        try {
            String jwtToken = authTokenFilter.parseJwt(request);
            Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);
    
            User loggedInUser = userRepository.findById(userId)
                    .orElseThrow(() -> new UserException("User not found"));
    
            Page<Message> allUnreadMessages = messageRepository.getAllUnreadMessages(loggedInUser.getId(), pageable);
    
            List<Message> allFriendsMessages = allUnreadMessages.stream()
                    .filter(message -> message.getRelationship().getStatus() == 1)
                    .collect(Collectors.toList());
    
            List<EntityModel<Message>> messageModels = allFriendsMessages.stream()
                    .map(messageModelAssembler::toModel)
                    .collect(Collectors.toList());
    
            CollectionModel<EntityModel<Message>> collectionModel = CollectionModel.of(messageModels,
                    linkTo(methodOn(MessageController.class).getUnreadMessages(request, pageable)).withSelfRel());
    
            return ResponseEntity.ok(collectionModel);
    
        } catch (Exception e) {
            e.printStackTrace();
    
            CollectionModel<EntityModel<Message>> errorModel = CollectionModel.of(
                    Collections.emptyList(),
                    linkTo(methodOn(MessageController.class).getUnreadMessages(request, pageable)).withSelfRel());
    
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorModel);
        }
    }
    

   
    private List<String> storeAttachments(MultipartFile[] files) {
        List<String> attachmentUrls = new ArrayList<>();

        if (files != null && files.length > 0) {
        for (MultipartFile file : files) {
            storageService.store(file);
            String filename = file.getOriginalFilename();
            String url = "http://localhost:8080/files/" + filename;
            attachmentUrls.add(url);
        }
    }
        return attachmentUrls;
    }

    
    private void updateMessageStatus(Long loggedInUserId, Long friendUserId) {
        messageRepository.updateStatusFromReadMessages(loggedInUserId, friendUserId);
    }
}
