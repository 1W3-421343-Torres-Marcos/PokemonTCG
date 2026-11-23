package com.pokemontcg.engine.model;

public enum PlayerId {
    PLAYER,
    CPU;

    public PlayerId opponent() {
        return this == PLAYER ? CPU : PLAYER;
    }
}
