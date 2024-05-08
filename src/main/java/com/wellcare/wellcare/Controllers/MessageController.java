package com.wellcare.wellcare.Controllers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
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
import jakarta.validation.Valid;

/* message status = 0 => message is unread
 * message status = 1 => message is read
 * users can send messages to each other and update them regardless if they're friends or not.
 * getMessagesWithUser: user1 can see the chat between him and a specific user, if the status = 0, the status will become 1
 * getRecentConversations: user1 can see the chat between him and many users, status can be 0 or 1, but if status is 0, it doesn't change to 1 in it
 * getUnreadMessages: user1 can see all the unread messages where status = 0 between him and many users, status remain 0 here
 */

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageRepository messageRepository;

      
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
    @CrossOrigin(origins = "http://localhost:3000") // Allow requests from localhost:3000
    @PostMapping("/new-message/{userId}")
    public ResponseEntity<EntityModel<?>> sendMessage(@Valid @PathVariable Long userId,
            @ModelAttribute Message message,
            @RequestParam(value = "file", required = false) MultipartFile[] files,
            HttpServletRequest request) {
        try {
            String jwtToken = authTokenFilter.parseJwt(request);
            Long loggedInUserId = jwtUtils.getUserIdFromJwtToken(jwtToken);
    
            User loggedInUser = userRepository.findById(loggedInUserId)
                    .orElseThrow(() -> new UserException("User not found"));
    
            Optional<User> toUserOptional = userRepository.findById(userId);
            if (toUserOptional.isEmpty()) {
                        return ResponseEntity.badRequest()
                                             .body(EntityModel.of(new MessageResponse("Recipient not found")));
            }
              
            User toUser = toUserOptional.get();
            
            List<String> attachmentUrls = storeAttachments(files);
    
            message.setFromUser(loggedInUser);
            message.setToUser(toUser);
            message.setTime(LocalDateTime.now());
            message.setAttachment(attachmentUrls);

            Message savedMessage = messageRepository.save(message);
    
            if (savedMessage != null) {
                EntityModel<Message> messageModel = messageModelAssembler.toModel(savedMessage);
                return ResponseEntity.ok(messageModel);
            }
    
            throw new MessageException("Error sending message");
    
        } catch (NumberFormatException e) {
            Link link = linkTo(methodOn(MessageController.class).getUnreadMessages(request, null)).withSelfRel();
            EntityModel<?> errorModel = EntityModel.of(new Message(), link);
            return ResponseEntity.badRequest().body(errorModel);
        } catch (UserException e) {
            Link link = linkTo(methodOn(MessageController.class).getUnreadMessages(request, null)).withSelfRel();
            EntityModel<?> errorModel = EntityModel.of(new Message(), link);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorModel);
        } catch (MessageException e) {
            Link link = linkTo(methodOn(MessageController.class).getUnreadMessages(request, null)).withSelfRel();
            EntityModel<?> errorModel = EntityModel.of(new Message(), link);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorModel);
        }
    }
    

    @PutMapping("/{messageId}")
public ResponseEntity<MessageResponse> updateMessage(@Valid @PathVariable Long messageId,
                                                     @RequestBody Message updatedMessage,
                                                     HttpServletRequest request) {
    try {
        String jwtToken = authTokenFilter.parseJwt(request);
        Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);

        Optional<Message> messageOptional = messageRepository.findById(messageId);

        if (messageOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(new MessageResponse("Message not found"));
        }

        Message message = messageOptional.get();

        if (!message.getFromUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                 .body(new MessageResponse("Forbidden: You cannot update this message"));
        }

        // Update the message content
        message.setContent(updatedMessage.getContent());

        Message savedMessage = messageRepository.save(message);

        EntityModel<Message> messageModel = messageModelAssembler.toModel(savedMessage);

        return ResponseEntity.ok(new MessageResponse("Message updated successfully", messageModel));

    } catch (Exception e) {
        Link link = linkTo(methodOn(MessageController.class).getUnreadMessages(request, null)).withSelfRel();
        EntityModel<Message> errorModel = EntityModel.of(new Message(), link);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body(new MessageResponse("Error updating message", errorModel));
    }
}

    



    @GetMapping("/chat/{chatUserId}")
    public ResponseEntity<CollectionModel<EntityModel<Message>>> getMessagesWithUser(@PathVariable Long chatUserId,
            HttpServletRequest request) throws UserException {
        try {
            String jwtToken = authTokenFilter.parseJwt(request);
            Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);

            User loggedInUser = userRepository.findById(userId)
                    .orElseThrow(() -> new UserException("User not found"));

            User chatUser = userRepository.findById(chatUserId)
                    .orElseThrow(() -> new UserException("Chat user not found"));

            updateMessageStatus(loggedInUser.getId(), chatUserId);

            List<Message> allMessagesBetweenTwoUsers = messageRepository
                    .findAllMessagesBetweenTwoUsers(loggedInUser.getId(), chatUserId);


            // allMessagesBetweenTwoUsers
            //         .forEach(message -> Hibernate.initialize(message.getRelationship().getMessageList()));


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
    public ResponseEntity<CollectionModel<EntityModel<Message>>> getUnreadMessages(HttpServletRequest request,
            @PageableDefault(size = 10) Pageable pageable) {
        try {
            String jwtToken = authTokenFilter.parseJwt(request);
            Long userId = jwtUtils.getUserIdFromJwtToken(jwtToken);

            User loggedInUser = userRepository.findById(userId)
                    .orElseThrow(() -> new UserException("User not found"));

            Page<Message> allUnreadMessages = messageRepository.getAllUnreadMessages(loggedInUser.getId(), pageable);


            List<EntityModel<Message>> messageModels = allUnreadMessages.getContent().stream()
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