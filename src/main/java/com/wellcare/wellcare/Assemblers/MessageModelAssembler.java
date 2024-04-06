package com.wellcare.wellcare.Assemblers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import com.wellcare.wellcare.Controllers.MessageController;
import com.wellcare.wellcare.Exceptions.UserException;
import com.wellcare.wellcare.Models.Message;

@Component
public class MessageModelAssembler implements RepresentationModelAssembler<Message, EntityModel<Message>> {

    @Override
    public EntityModel<Message> toModel(Message message) {
        try {
            EntityModel<Message> messageModel = EntityModel.of(message,
                linkTo(methodOn(MessageController.class).getMessagesWithUser(message.getToUser().getId(), null)).withRel("messages"),
                linkTo(methodOn(MessageController.class).getUnreadMessages(null, null)).withRel("unreadMessages"));
            
            return messageModel;

        } catch (UserException e) {
            e.printStackTrace();
            return EntityModel.of(message);
        }
    }
}

