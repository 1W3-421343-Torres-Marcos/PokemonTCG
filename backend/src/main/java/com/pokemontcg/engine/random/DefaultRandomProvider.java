package com.pokemontcg.engine.random;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;

public class DefaultRandomProvider implements RandomProvider {

    private final SecureRandom random;

    public DefaultRandomProvider() {
        this.random = new SecureRandom();
    }

    @Override
    public void shuffle(List<?> items) {
        Collections.shuffle(items, random);
    }

    @Override
    public boolean flipCoin() {
        return random.nextBoolean();
    }
}
