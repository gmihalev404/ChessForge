package com.example.chessforge.service;

import com.example.chessforge.model.entity.FriendRequest;
import com.example.chessforge.model.entity.Friendship;
import com.example.chessforge.model.entity.User;
import com.example.chessforge.model.enums.FriendRequestStatus;
import com.example.chessforge.repository.FriendRequestRepository;
import com.example.chessforge.repository.FriendshipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendshipServiceTest {

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    private FriendshipService friendshipService;

    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        friendshipService = new FriendshipService(
                friendRequestRepository,
                friendshipRepository
        );

        user1 = createUser(1L, "user1");
        user2 = createUser(2L, "user2");
        user3 = createUser(3L, "user3");
    }

    // =========================================================
    // SEND REQUEST
    // =========================================================

    @Test
    void sendRequestShouldCreateFriendRequest() {

        when(friendshipRepository.existsByUser1AndUser2(user1, user2))
                .thenReturn(false);

        when(friendRequestRepository.existsBySenderAndReceiverAndStatus(
                user1,
                user2,
                FriendRequestStatus.PENDING
        )).thenReturn(false);

        when(friendRequestRepository.existsBySenderAndReceiverAndStatus(
                user2,
                user1,
                FriendRequestStatus.PENDING
        )).thenReturn(false);

        when(friendRequestRepository.save(any(FriendRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FriendRequest result =
                friendshipService.sendRequest(user1, user2);

        assertNotNull(result);
        assertEquals(user1, result.getSender());
        assertEquals(user2, result.getReceiver());

        verify(friendRequestRepository)
                .save(any(FriendRequest.class));
    }

    @Test
    void sendRequestShouldThrowWhenSenderAndReceiverAreSameUser() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> friendshipService.sendRequest(user1, user1)
                );

        assertEquals(
                "You cannot send a friend request to yourself.",
                exception.getMessage()
        );

        verifyNoInteractions(friendRequestRepository);
        verifyNoInteractions(friendshipRepository);
    }

    @Test
    void sendRequestShouldThrowWhenUsersAreAlreadyFriends() {

        when(friendshipRepository.existsByUser1AndUser2(user1, user2))
                .thenReturn(true);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> friendshipService.sendRequest(user1, user2)
                );

        assertEquals(
                "Users are already friends.",
                exception.getMessage()
        );

        verify(friendRequestRepository, never())
                .save(any());
    }

    @Test
    void sendRequestShouldThrowWhenPendingRequestExistsInSameDirection() {

        when(friendshipRepository.existsByUser1AndUser2(user1, user2))
                .thenReturn(false);

        when(friendRequestRepository.existsBySenderAndReceiverAndStatus(
                user1,
                user2,
                FriendRequestStatus.PENDING
        )).thenReturn(true);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> friendshipService.sendRequest(user1, user2)
                );

        assertEquals(
                "A pending friend request already exists between these users.",
                exception.getMessage()
        );

        verify(friendRequestRepository, never())
                .save(any());
    }

    @Test
    void sendRequestShouldThrowWhenPendingRequestExistsInOppositeDirection() {

        when(friendshipRepository.existsByUser1AndUser2(user1, user2))
                .thenReturn(false);

        when(friendRequestRepository.existsBySenderAndReceiverAndStatus(
                user1,
                user2,
                FriendRequestStatus.PENDING
        )).thenReturn(false);

        when(friendRequestRepository.existsBySenderAndReceiverAndStatus(
                user2,
                user1,
                FriendRequestStatus.PENDING
        )).thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> friendshipService.sendRequest(user1, user2)
        );

        verify(friendRequestRepository, never())
                .save(any());
    }

    // =========================================================
    // ACCEPT REQUEST
    // =========================================================

    @Test
    void acceptRequestShouldAcceptRequestAndCreateFriendship() {

        FriendRequest request = createPendingRequest(user1, user2);

        friendshipService.acceptRequest(request, user2);

        assertEquals(
                FriendRequestStatus.ACCEPTED,
                request.getStatus()
        );

        assertNotNull(request.getResolvedAt());

        ArgumentCaptor<Friendship> captor =
                ArgumentCaptor.forClass(Friendship.class);

        verify(friendshipRepository)
                .save(captor.capture());

        Friendship friendship = captor.getValue();

        assertEquals(user1, friendship.getUser1());
        assertEquals(user2, friendship.getUser2());
    }

    @Test
    void acceptRequestShouldOrderUsersById() {

        User higherIdUser = createUser(10L, "higher");
        User lowerIdUser = createUser(4L, "lower");

        FriendRequest request =
                createPendingRequest(higherIdUser, lowerIdUser);

        friendshipService.acceptRequest(
                request,
                lowerIdUser
        );

        ArgumentCaptor<Friendship> captor =
                ArgumentCaptor.forClass(Friendship.class);

        verify(friendshipRepository)
                .save(captor.capture());

        Friendship friendship = captor.getValue();

        assertEquals(lowerIdUser, friendship.getUser1());
        assertEquals(higherIdUser, friendship.getUser2());
    }

    @Test
    void acceptRequestShouldThrowWhenUserIsNotReceiver() {

        FriendRequest request =
                createPendingRequest(user1, user2);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> friendshipService.acceptRequest(
                                request,
                                user3
                        )
                );

        assertEquals(
                "Only the receiver can accept this friend request.",
                exception.getMessage()
        );

        verify(friendshipRepository, never())
                .save(any());
    }

    @Test
    void acceptRequestShouldThrowWhenRequestIsNotPending() {

        FriendRequest request =
                createPendingRequest(user1, user2);

        request.setStatus(FriendRequestStatus.DECLINED);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> friendshipService.acceptRequest(
                                request,
                                user2
                        )
                );

        assertEquals(
                "Friend request is no longer pending.",
                exception.getMessage()
        );

        verify(friendshipRepository, never())
                .save(any());
    }

    // =========================================================
    // DECLINE REQUEST
    // =========================================================

    @Test
    void declineRequestShouldDeclinePendingRequest() {

        FriendRequest request =
                createPendingRequest(user1, user2);

        friendshipService.declineRequest(
                request,
                user2
        );

        assertEquals(
                FriendRequestStatus.DECLINED,
                request.getStatus()
        );

        assertNotNull(request.getResolvedAt());
    }

    @Test
    void declineRequestShouldThrowWhenUserIsNotReceiver() {

        FriendRequest request =
                createPendingRequest(user1, user2);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> friendshipService.declineRequest(
                                request,
                                user3
                        )
                );

        assertEquals(
                "Only the receiver can decline this friend request.",
                exception.getMessage()
        );
    }

    @Test
    void declineRequestShouldThrowWhenRequestIsNotPending() {

        FriendRequest request =
                createPendingRequest(user1, user2);

        request.setStatus(FriendRequestStatus.ACCEPTED);

        assertThrows(
                IllegalStateException.class,
                () -> friendshipService.declineRequest(
                        request,
                        user2
                )
        );
    }

    // =========================================================
    // CANCEL REQUEST
    // =========================================================

    @Test
    void cancelRequestShouldCancelPendingRequest() {

        FriendRequest request =
                createPendingRequest(user1, user2);

        friendshipService.cancelRequest(
                request,
                user1
        );

        assertEquals(
                FriendRequestStatus.CANCELLED,
                request.getStatus()
        );

        assertNotNull(request.getResolvedAt());
    }

    @Test
    void cancelRequestShouldThrowWhenUserIsNotSender() {

        FriendRequest request =
                createPendingRequest(user1, user2);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> friendshipService.cancelRequest(
                                request,
                                user2
                        )
                );

        assertEquals(
                "Only the sender can cancel this friend request.",
                exception.getMessage()
        );
    }

    @Test
    void cancelRequestShouldThrowWhenRequestIsNotPending() {

        FriendRequest request =
                createPendingRequest(user1, user2);

        request.setStatus(FriendRequestStatus.ACCEPTED);

        assertThrows(
                IllegalStateException.class,
                () -> friendshipService.cancelRequest(
                        request,
                        user1
                )
        );
    }

    // =========================================================
    // REMOVE FRIEND
    // =========================================================

    @Test
    void removeFriendShouldDeleteExistingFriendship() {

        Friendship friendship = Friendship.builder()
                .user1(user1)
                .user2(user2)
                .build();

        when(friendshipRepository.findByUser1AndUser2(
                user1,
                user2
        )).thenReturn(Optional.of(friendship));

        friendshipService.removeFriend(
                user1,
                user2
        );

        verify(friendshipRepository)
                .delete(friendship);
    }

    @Test
    void removeFriendShouldNormalizeUserOrder() {

        Friendship friendship = Friendship.builder()
                .user1(user1)
                .user2(user2)
                .build();

        when(friendshipRepository.findByUser1AndUser2(
                user1,
                user2
        )).thenReturn(Optional.of(friendship));

        friendshipService.removeFriend(
                user2,
                user1
        );

        verify(friendshipRepository)
                .findByUser1AndUser2(user1, user2);

        verify(friendshipRepository)
                .delete(friendship);
    }

    @Test
    void removeFriendShouldThrowWhenFriendshipDoesNotExist() {

        when(friendshipRepository.findByUser1AndUser2(
                user1,
                user2
        )).thenReturn(Optional.empty());

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> friendshipService.removeFriend(
                                user1,
                                user2
                        )
                );

        assertEquals(
                "Users are not friends.",
                exception.getMessage()
        );

        verify(friendshipRepository, never())
                .delete(any());
    }

    // =========================================================
    // ARE FRIENDS
    // =========================================================

    @Test
    void areFriendsShouldReturnTrueWhenFriendshipExists() {

        when(friendshipRepository.existsByUser1AndUser2(
                user1,
                user2
        )).thenReturn(true);

        boolean result =
                friendshipService.areFriends(user1, user2);

        assertTrue(result);
    }

    @Test
    void areFriendsShouldReturnFalseWhenFriendshipDoesNotExist() {

        when(friendshipRepository.existsByUser1AndUser2(
                user1,
                user2
        )).thenReturn(false);

        boolean result =
                friendshipService.areFriends(user1, user2);

        assertFalse(result);
    }

    @Test
    void areFriendsShouldNormalizeUserOrder() {

        when(friendshipRepository.existsByUser1AndUser2(
                user1,
                user2
        )).thenReturn(true);

        boolean result =
                friendshipService.areFriends(user2, user1);

        assertTrue(result);

        verify(friendshipRepository)
                .existsByUser1AndUser2(user1, user2);
    }

    // =========================================================
    // GET REQUESTS
    // =========================================================

    @Test
    void getIncomingPendingRequestsShouldReturnReceiverRequests() {

        FriendRequest request =
                createPendingRequest(user1, user2);

        when(friendRequestRepository.findByReceiverAndStatus(
                user2,
                FriendRequestStatus.PENDING
        )).thenReturn(List.of(request));

        List<FriendRequest> result =
                friendshipService.getIncomingPendingRequests(user2);

        assertEquals(1, result.size());
        assertEquals(request, result.get(0));
    }

    @Test
    void getOutgoingPendingRequestsShouldReturnSenderRequests() {

        FriendRequest request =
                createPendingRequest(user1, user2);

        when(friendRequestRepository.findBySenderAndStatus(
                user1,
                FriendRequestStatus.PENDING
        )).thenReturn(List.of(request));

        List<FriendRequest> result =
                friendshipService.getOutgoingPendingRequests(user1);

        assertEquals(1, result.size());
        assertEquals(request, result.get(0));
    }

    // =========================================================
    // GET FRIENDSHIPS
    // =========================================================

    @Test
    void getFriendshipsShouldReturnAllFriendshipsForUser() {

        Friendship friendship1 = Friendship.builder()
                .user1(user1)
                .user2(user2)
                .build();

        Friendship friendship2 = Friendship.builder()
                .user1(user1)
                .user2(user3)
                .build();

        when(friendshipRepository.findByUser1OrUser2(
                user1,
                user1
        )).thenReturn(
                List.of(friendship1, friendship2)
        );

        List<Friendship> result =
                friendshipService.getFriendships(user1);

        assertEquals(2, result.size());
        assertTrue(result.contains(friendship1));
        assertTrue(result.contains(friendship2));
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private User createUser(Long id, String username) {

        User user = User.builder()
                .username(username)
                .email(username + "@test.com")
                .password("password")
                .build();

        ReflectionTestUtils.setField(user, "id", id);

        return user;
    }

    private FriendRequest createPendingRequest(
            User sender,
            User receiver
    ) {

        return FriendRequest.builder()
                .sender(sender)
                .receiver(receiver)
                .status(FriendRequestStatus.PENDING)
                .build();
    }
}