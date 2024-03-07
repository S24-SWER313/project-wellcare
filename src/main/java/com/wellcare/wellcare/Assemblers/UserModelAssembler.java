package com.wellcare.wellcare.Assemblers;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import com.wellcare.wellcare.Controllers.UserController;
import com.wellcare.wellcare.Models.User;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;


@Component
public class UserModelAssembler implements RepresentationModelAssembler<User, EntityModel<User>> {
    
    @SuppressWarnings("null")
    @Override
    public EntityModel<User> toModel(User user) {
        try{
        return EntityModel.of(user, 
        linkTo(methodOn(UserController.class).getUserProfile(user.getId())).withSelfRel(),
        linkTo(methodOn(UserController.class).getUserById(user.getId())).withRel("userById"),
        linkTo(methodOn(UserController.class).getUserByRole(user.getRole().getName())).withRel("userByRole"),
        linkTo(methodOn(UserController.class).getAllUsersByRole(user.getRole().getName())).withRel("allUsersByRole"),
        linkTo(methodOn(UserController.class).updateUserProfile(null, null)).withRel("updateProfile"));
        }catch(Exception e){
            return EntityModel.of(user);
        }
        

    }
}
