package com.example.chessforge.repository.friendship;

import com.example.chessforge.model.entity.friendship.Friendship;
import com.example.chessforge.model.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository
        extends JpaRepository<Friendship, Long> {

    boolean existsByUser1AndUser2(User user1, User user2);

    List<Friendship> findByUser1OrUser2(User user1, User user2);

    Optional<Friendship> findByUser1AndUser2(User user1, User user2);
}
