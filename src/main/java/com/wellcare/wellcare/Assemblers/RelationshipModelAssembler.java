package com.wellcare.wellcare.Assemblers;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;
import com.wellcare.wellcare.Controllers.RelationshipController;
import com.wellcare.wellcare.Controllers.RelationshipController;
import com.wellcare.wellcare.Models.Relationship;


@Component
public class RelationshipModelAssembler implements RepresentationModelAssembler<Relationship, EntityModel<Relationship>> {

    @Override
    public EntityModel<Relationship> toModel(Relationship relationship) {
        return EntityModel.of(relationship,
                linkTo(methodOn(RelationshipController.class).addFriend(null, null)).withRel("addFriend"),
                linkTo(methodOn(RelationshipController.class).removeFriend(null, null)).withRel("removeFriend"),
                linkTo(methodOn(RelationshipController.class).acceptFriend(null, null)).withRel("acceptFriend"),
                linkTo(methodOn(RelationshipController.class).cancelFriendshipRequest(null, null)).withRel("cancelFriendshipRequest"));
    }
}
