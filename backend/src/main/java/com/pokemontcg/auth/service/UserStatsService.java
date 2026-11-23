package com.pokemontcg.auth.service;

import com.pokemontcg.auth.domain.UserEntity;
import com.pokemontcg.auth.repository.UserRepository;
import com.pokemontcg.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserStatsService {

    private final UserRepository userRepository;

    public UserStatsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void recordMatchResult(UUID userId, boolean won) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setMatchesPlayed(user.getMatchesPlayed() + 1);
        if (won) {
            user.setMatchesWon(user.getMatchesWon() + 1);
        }
        userRepository.save(user);
    }
}
