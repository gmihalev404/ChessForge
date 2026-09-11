package com.example.chessforge.service.game.engine.model;

public enum DrawReason {

    STALEMATE,
    INSUFFICIENT_MATERIAL,

    THREEFOLD_REPETITION,
    FIVEFOLD_REPETITION,

    FIFTY_MOVE_RULE,

    SEVENTY_FIVE_MOVE_RULE
}