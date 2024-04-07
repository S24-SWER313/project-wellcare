package com.wellcare.wellcare.Repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wellcare.wellcare.Models.Relationship;

@Repository
public interface RelationshipRepository extends JpaRepository<Relationship, Long> {

        List<Relationship> findAllByUserOneIdAndStatus(Long id, int status);

        List<Relationship> findAllByUserOneIdAndStatusOrUserTwoIdAndStatus(Long id1, int status1, Long id2,
                        int status2);

        List<Relationship> findAllByUserOneIdOrUserTwoIdAndStatusNot(Long id1, Long id2, int status);

        Relationship findByUserOneIdAndUserTwoId(Long userOneId, Long userTwoId);

        List<Relationship> findAllByUserOneIdOrUserTwoId(Long userOneId, Long userTwoId);

        @Query("SELECT r FROM Relationship r WHERE (r.userOne.id = :userOneId AND r.userTwo.id = :userTwoId) OR (r.userOne.id = :userTwoId AND r.userTwo.id = :userOneId) AND r.status = :status")
        Relationship findRelationshipWithFriendWithStatus(@Param("userOneId") Long userOneId,
                        @Param("userTwoId") Long userTwoId, @Param("status") int status);

        @Query(value = "" +
                        "SELECT r FROM Relationship AS r " +
                        "WHERE ((r.userOne.id = :id1 AND r.userTwo.id = :id2) " +
                        "OR ( r.userTwo.id = :id1 AND r.userOne.id = :id2)) ")
        Relationship findRelationshipByUserOneIdAndUserTwoId(@Param(value = "id1") Long userOneId,
                        @Param(value = "id2") Long userTwoId);

        @Query("SELECT r FROM Relationship r WHERE (r.userOne.id = :id OR r.userTwo.id = :id) AND r.status NOT IN (0, 2)")
        List<Relationship> findAllNotCandidatesForFriends(Long id);

        @Query("SELECT r FROM Relationship AS r WHERE (r.userOne.id = :id OR r.userTwo.id = :id) AND r.status = 0")
        List<Relationship> findAllRequestedForFriendUsers(Long id);

        List<Relationship> findRelationshipsByUserTwoIdAndStatus(Long userTwoId, int status);

}

