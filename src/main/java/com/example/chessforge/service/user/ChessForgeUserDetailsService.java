package com.example.chessforge.service.user;

import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.model.enums.user.UserStatus;
import com.example.chessforge.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChessForgeUserDetailsService
        implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(
            String username
    ) throws UsernameNotFoundException {

        User user =
                userRepository.findByUsername(
                        username
                ).orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found."
                        )
                );

        boolean active =
                user.getStatus()
                        == UserStatus.ACTIVE;

        return org.springframework.security.core.userdetails.User
                .withUsername(
                        user.getUsername()
                )
                .password(
                        user.getPassword()
                )
                .authorities(
                        List.of(
                                new SimpleGrantedAuthority(
                                        user.getRole().name()
                                )
                        )
                )
                .disabled(
                        !active
                )
                .build();
    }
}