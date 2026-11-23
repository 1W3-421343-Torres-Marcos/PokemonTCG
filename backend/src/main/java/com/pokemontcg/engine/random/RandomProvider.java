package com.pokemontcg.engine.random;

import java.util.List;

public interface RandomProvider {

    void shuffle(List<?> items);

    boolean flipCoin();
}
