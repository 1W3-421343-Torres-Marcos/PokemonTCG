package com.pokemontcg.match.domain;

import com.pokemontcg.bot.BotDifficulty;
import com.pokemontcg.engine.model.GameState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "matches")
public class MatchEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "player_deck_id")
    private UUID playerDeckId;

    @Column(name = "cpu_deck_id")
    private UUID cpuDeckId;

    @Column(name = "player_deck_name", nullable = false, length = 120)
    private String playerDeckName;

    @Column(name = "cpu_deck_name", nullable = false, length = 120)
    private String cpuDeckName;

    @Enumerated(EnumType.STRING)
    @Column(name = "bot_difficulty", nullable = false, length = 20)
    private BotDifficulty botDifficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchStatus status;

    @Column(length = 20)
    private String winner;

    @Column(name = "win_reason", length = 40)
    private String winReason;

    @Column(name = "turn_number", nullable = false)
    private int turnNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "state_json", columnDefinition = "jsonb", nullable = false)
    private GameState stateJson;

    @Column(name = "event_seq", nullable = false)
    private long eventSeq;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getPlayerDeckId() {
        return playerDeckId;
    }

    public void setPlayerDeckId(UUID playerDeckId) {
        this.playerDeckId = playerDeckId;
    }

    public UUID getCpuDeckId() {
        return cpuDeckId;
    }

    public void setCpuDeckId(UUID cpuDeckId) {
        this.cpuDeckId = cpuDeckId;
    }

    public String getPlayerDeckName() {
        return playerDeckName;
    }

    public void setPlayerDeckName(String playerDeckName) {
        this.playerDeckName = playerDeckName;
    }

    public String getCpuDeckName() {
        return cpuDeckName;
    }

    public void setCpuDeckName(String cpuDeckName) {
        this.cpuDeckName = cpuDeckName;
    }

    public BotDifficulty getBotDifficulty() {
        return botDifficulty;
    }

    public void setBotDifficulty(BotDifficulty botDifficulty) {
        this.botDifficulty = botDifficulty;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public void setStatus(MatchStatus status) {
        this.status = status;
    }

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public String getWinReason() {
        return winReason;
    }

    public void setWinReason(String winReason) {
        this.winReason = winReason;
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public void setTurnNumber(int turnNumber) {
        this.turnNumber = turnNumber;
    }

    public GameState getStateJson() {
        return stateJson;
    }

    public void setStateJson(GameState stateJson) {
        this.stateJson = stateJson;
    }

    public long getEventSeq() {
        return eventSeq;
    }

    public void setEventSeq(long eventSeq) {
        this.eventSeq = eventSeq;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
