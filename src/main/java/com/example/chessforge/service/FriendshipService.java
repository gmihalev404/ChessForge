package com.example.chessforge.service;

import com.example.chessforge.model.entity.FriendRequest;
import com.example.chessforge.model.entity.Friendship;
import com.example.chessforge.model.entity.User;
import com.example.chessforge.model.enums.FriendRequestStatus;
import com.example.chessforge.repository.FriendRequestRepository;
import com.example.chessforge.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendshipService {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;

    @Transactional
    public FriendRequest sendRequest(User sender, User receiver) {

        if (sender.getId().equals(receiver.getId())) {
            throw new IllegalArgumentException(
                    "You cannot send a friend request to yourself."
            );
        }

        if (areFriends(sender, receiver)) {
            throw new IllegalStateException(
                    "Users are already friends."
            );
        }

        if (hasPendingRequest(sender, receiver)) {
            throw new IllegalStateException(
                    "A pending friend request already exists between these users."
            );
        }

        FriendRequest request = FriendRequest.builder()
                .sender(sender)
                .receiver(receiver)
                .build();

        return friendRequestRepository.save(request);
    }

    @Transactional
    public void acceptRequest(FriendRequest request, User receiver) {

        validatePendingRequest(request);

        if (!request.getReceiver().getId().equals(receiver.getId())) {
            throw new IllegalStateException(
                    "Only the receiver can accept this friend request."
            );
        }

        request.setStatus(FriendRequestStatus.ACCEPTED);
        request.setResolvedAt(LocalDateTime.now());

        User user1 = getFirstUser(
                request.getSender(),
                request.getReceiver()
        );

        User user2 = getSecondUser(
                request.getSender(),
                request.getReceiver()
        );

        Friendship friendship = Friendship.builder()
                .user1(user1)
                .user2(user2)
                .build();

        friendshipRepository.save(friendship);
    }

    @Transactional
    public void declineRequest(FriendRequest request, User receiver) {

        validatePendingRequest(request);

        if (!request.getReceiver().getId().equals(receiver.getId())) {
            throw new IllegalStateException(
                    "Only the receiver can decline this friend request."
            );
        }

        request.setStatus(FriendRequestStatus.DECLINED);
        request.setResolvedAt(LocalDateTime.now());
    }

    @Transactional
    public void cancelRequest(FriendRequest request, User sender) {

        validatePendingRequest(request);

        if (!request.getSender().getId().equals(sender.getId())) {
            throw new IllegalStateException(
                    "Only the sender can cancel this friend request."
            );
        }

        request.setStatus(FriendRequestStatus.CANCELLED);
        request.setResolvedAt(LocalDateTime.now());
    }

    @Transactional
    public void removeFriend(User userA, User userB) {

        User user1 = getFirstUser(userA, userB);
        User user2 = getSecondUser(userA, userB);

        Friendship friendship = friendshipRepository
                .findByUser1AndUser2(user1, user2)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Users are not friends."
                        )
                );

        friendshipRepository.delete(friendship);
    }

    public boolean areFriends(User userA, User userB) {

        User user1 = getFirstUser(userA, userB);
        User user2 = getSecondUser(userA, userB);

        return friendshipRepository
                .existsByUser1AndUser2(user1, user2);
    }

    public List<FriendRequest> getIncomingPendingRequests(User user) {
        return friendRequestRepository
                .findByReceiverAndStatus(
                        user,
                        FriendRequestStatus.PENDING
                );
    }

    public List<FriendRequest> getOutgoingPendingRequests(User user) {
        return friendRequestRepository
                .findBySenderAndStatus(
                        user,
                        FriendRequestStatus.PENDING
                );
    }

    public List<Friendship> getFriendships(User user) {
        return friendshipRepository
                .findByUser1OrUser2(user, user);
    }

    private boolean hasPendingRequest(User userA, User userB) {

        return friendRequestRepository
                .existsBySenderAndReceiverAndStatus(
                        userA,
                        userB,
                        FriendRequestStatus.PENDING
                )
                ||
                friendRequestRepository
                        .existsBySenderAndReceiverAndStatus(
                                userB,
                                userA,
                                FriendRequestStatus.PENDING
                        );
    }

    private void validatePendingRequest(FriendRequest request) {

        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new IllegalStateException(
                    "Friend request is no longer pending."
            );
        }
    }

    private User getFirstUser(User userA, User userB) {
        return userA.getId() < userB.getId()
                ? userA
                : userB;
    }

    private User getSecondUser(User userA, User userB) {
        return userA.getId() < userB.getId()
                ? userB
                : userA;
    }
}