package com.pokemontcg.engine.action;

public sealed interface GameAction permits
        SelectSetupAction,
        PlayBasicAction,
        AttachEnergyAction,
        AttackAction,
        PromoteActiveAction,
        EndTurnAction,
        ConcedeAction,
        EvolveAction,
        PlayTrainerAction,
        RetreatAction {
}
