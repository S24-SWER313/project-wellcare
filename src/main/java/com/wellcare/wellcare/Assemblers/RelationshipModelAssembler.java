package com.wellcare.wellcare.Assemblers;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.hateoas.CollectionModel;

import com.wellcare.wellcare.Controllers.RelationshipController;
import com.wellcare.wellcare.Controllers.RelationshipController;
import com.wellcare.wellcare.Models.Relationship;


@Component
public class RelationshipModelAssembler implements RepresentationModelAssembler<Relationship, EntityModel<Relationship>> {

    @Override
    public EntityModel<Relationship> toModel(Relationship relationship) {
        return EntityModel.of(relationship,
                        linkTo(methodOn(RelationshipController.class).getFriendRequests(null)).withRel("friendRequests"),
                        linkTo(methodOn(RelationshipController.class).acceptFriend(null, relationship.getUserOne().getUsername())).withRel("acceptFriend"),
                        linkTo(methodOn(RelationshipController.class).cancelFriendshipRequest(null, relationship.getUserOne().getUsername())).withRel("cancelFriendshipRequest")
        );
    }

    
}
