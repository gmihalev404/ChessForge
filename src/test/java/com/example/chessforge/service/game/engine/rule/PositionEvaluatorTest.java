package com.example.chessforge.service.game.engine.rule;

import com.example.chessforge.service.game.engine.model.*;
import com.example.chessforge.service.game.engine.move.LegalMoveGenerator;
import com.example.chessforge.service.game.engine.move.MoveApplier;
import com.example.chessforge.service.game.engine.move.MoveGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PositionEvaluatorTest {

    private PositionEvaluator positionEvaluator;

    @BeforeEach
    void setUp() {

        AttackDetector attackDetector =
                new AttackDetector();

        MoveApplier moveApplier =
                new MoveApplier();

        LegalMoveGenerator legalMoveGenerator =
                new LegalMoveGenerator(
                        new MoveGenerator(),
                        moveApplier,
                        attackDetector
                );

        positionEvaluator =
                new PositionEvaluator(
                        legalMoveGenerator,
                        attackDetector
                );
    }

    @Test
    void shouldDetectCheckmate() {

        Board board =
                new Board();

        board.setPiece(
                Square.fromAlgebraic("h1"),
                new Piece(
                        PieceType.KING,
                        PieceColor.WHITE
                )
        );

        board.setPiece(
                Square.fromAlgebraic("g2"),
                new Piece(
                        PieceType.QUEEN,
                        PieceColor.BLACK
                )
        );

        board.setPiece(
                Square.fromAlgebraic("f3"),
                new Piece(
                        PieceType.KING,
                        PieceColor.BLACK
                )
        );

        GameState state =
                createState(
                        board,
                        PieceColor.WHITE
                );

        assertTrue(
                positionEvaluator.isCheckmate(
                        state
                )
        );

        assertFalse(
                positionEvaluator.isStalemate(
                        state
                )
        );
    }

    @Test
    void shouldDetectStalemate() {

        Board board =
                new Board();

        board.setPiece(
                Square.fromAlgebraic("h1"),
                new Piece(
                        PieceType.KING,
                        PieceColor.WHITE
                )
        );

        board.setPiece(
                Square.fromAlgebraic("f2"),
                new Piece(
                        PieceType.KING,
                        PieceColor.BLACK
                )
        );

        board.setPiece(
                Square.fromAlgebraic("g3"),
                new Piece(
                        PieceType.QUEEN,
                        PieceColor.BLACK
                )
        );

        GameState state =
                createState(
                        board,
                        PieceColor.WHITE
                );

        assertTrue(
                positionEvaluator.isStalemate(
                        state
                )
        );

        assertFalse(
                positionEvaluator.isCheckmate(
                        state
                )
        );
    }

    @Test
    void initialPositionShouldBeNeitherCheckmateNorStalemate() {

        GameState state =
                GameState.initial();

        assertFalse(
                positionEvaluator.isCheckmate(
                        state
                )
        );

        assertFalse(
                positionEvaluator.isStalemate(
                        state
                )
        );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private GameState createState(
            Board board,
            PieceColor sideToMove
    ) {

        return new GameState(
                board,
                sideToMove,
                false,
                false,
                false,
                false,
                null,
                0,
                1
        );
    }
}