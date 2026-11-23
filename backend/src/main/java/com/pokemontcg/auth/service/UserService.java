package com.pokemontcg.auth.service;

import com.pokemontcg.auth.domain.UserEntity;
import com.pokemontcg.auth.domain.UserFavoriteCardEntity;
import com.pokemontcg.auth.domain.UserFavoriteCardId;
import com.pokemontcg.auth.dto.UserProfileDTO;
import com.pokemontcg.auth.repository.UserFavoriteCardRepository;
import com.pokemontcg.auth.repository.UserRepository;
import com.pokemontcg.catalog.repository.CardRepository;
import com.pokemontcg.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserFavoriteCardRepository favoriteCardRepository;
    private final CardRepository cardRepository;

    public UserService(
            UserRepository userRepository,
            UserFavoriteCardRepository favoriteCardRepository,
            CardRepository cardRepository
    ) {
        this.userRepository = userRepository;
        this.favoriteCardRepository = favoriteCardRepository;
        this.cardRepository = cardRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileDTO getProfile(UUID userId) {
        UserEntity user = findUserOrThrow(userId);
        List<String> favorites = favoriteCardRepository.findCardIdsByUserId(userId);
        return AuthService.toProfileDto(user, favorites);
    }

    @Transactional
    public UserProfileDTO addFavorite(UUID userId, String cardId) {
        UserEntity user = findUserOrThrow(userId);
        if (!cardRepository.existsById(cardId)) {
            throw new ResourceNotFoundException("Card not found: " + cardId);
        }
        UserFavoriteCardId id = new UserFavoriteCardId(userId, cardId);
        if (!favoriteCardRepository.existsById(id)) {
            favoriteCardRepository.save(new UserFavoriteCardEntity(userId, cardId, Instant.now()));
        }
        return AuthService.toProfileDto(user, favoriteCardRepository.findCardIdsByUserId(userId));
    }

    @Transactional
    public UserProfileDTO removeFavorite(UUID userId, String cardId) {
        UserEntity user = findUserOrThrow(userId);
        UserFavoriteCardId id = new UserFavoriteCardId(userId, cardId);
        if (favoriteCardRepository.existsById(id)) {
            favoriteCardRepository.deleteById(id);
        }
        return AuthService.toProfileDto(user, favoriteCardRepository.findCardIdsByUserId(userId));
    }

    private UserEntity findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
