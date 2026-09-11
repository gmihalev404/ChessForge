package com.example.chessforge.service.game.engine.model;

public enum PieceColor {

    WHITE,
    BLACK;

    public PieceColor opposite() {

        return this == WHITE
                ? BLACK
                : WHITE;
    }
}