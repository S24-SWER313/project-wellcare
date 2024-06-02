package com.wellcare.wellcare.Repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wellcare.wellcare.Models.Relationship;
import com.wellcare.wellcare.Models.User;

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

        @Query("SELECT r FROM Relationship r " +
                        "JOIN r.userOne u1 " +
                        "JOIN r.userTwo u2 " +
                        "WHERE (u1.username = :username1 AND u2.username = :username2) " +
                        "OR (u1.username = :username2 AND u2.username = :username1)")
        Relationship findRelationshipByUserOneUsernameAndUserTwoUsername(
                        @Param("username1") String username1,
                        @Param("username2") String username2);

        @Query("SELECT r FROM Relationship r WHERE (r.userOne.id = :id OR r.userTwo.id = :id) AND r.status NOT IN (0, 2)")
        List<Relationship> findAllNotCandidatesForFriends(Long id);

        @Query("SELECT r FROM Relationship AS r WHERE (r.userOne.id = :id OR r.userTwo.id = :id) AND r.status = 0")
        List<Relationship> findAllRequestedForFriendUsers(Long id);

        List<Relationship> findRelationshipsByUserTwoIdAndStatus(Long userTwoId, int status);

        List<Relationship> findRelationshipsByUserOneIdAndStatus(Long userId, int status);

        boolean existsByUserOneIdAndUserTwoIdAndStatus(Long userOneId, Long userTwoId, int status);

        @Query("SELECT COUNT(r) FROM Relationship r WHERE (r.userOne = :user OR r.userTwo = :user) AND r.status = 1")
        long countFriends(@Param("user") User user);

        @Query("SELECT CASE WHEN r.userOne.username = :username THEN r.userTwo ELSE r.userOne END FROM Relationship r WHERE (r.userOne.username = :username OR r.userTwo.username = :username) AND r.status = 1")
        List<User> findFriendsByUsername(@Param("username") String username);
}