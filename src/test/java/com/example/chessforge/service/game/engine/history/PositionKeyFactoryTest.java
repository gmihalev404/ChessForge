package com.example.chessforge.service.game.engine.history;

import com.example.chessforge.service.game.engine.model.*;
import com.example.chessforge.service.game.engine.move.LegalMoveGenerator;
import com.example.chessforge.service.game.engine.move.MoveApplier;
import com.example.chessforge.service.game.engine.move.MoveGenerator;
import com.example.chessforge.service.game.engine.rule.AttackDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PositionKeyFactoryTest {

    private PositionKeyFactory positionKeyFactory;

    @BeforeEach
    void setUp() {

        MoveGenerator moveGenerator =
                new MoveGenerator();

        MoveApplier moveApplier =
                new MoveApplier();

        AttackDetector attackDetector =
                new AttackDetector();

        LegalMoveGenerator legalMoveGenerator =
                new LegalMoveGenerator(
                        moveGenerator,
                        moveApplier,
                        attackDetector
                );

        positionKeyFactory =
                new PositionKeyFactory(
                        legalMoveGenerator
                );
    }

    @Test
    void differentMoveCountersShouldProduceSamePositionKey() {

        GameState first =
                GameState.initial();

        GameState second =
                GameState.initial();

        first.setHalfMoveClock(4);
        first.setFullMoveNumber(10);

        second.setHalfMoveClock(37);
        second.setFullMoveNumber(25);

        assertEquals(
                positionKeyFactory.create(first),
                positionKeyFactory.create(second)
        );
    }

    @Test
    void differentSideToMoveShouldProduceDifferentPositionKey() {

        GameState first =
                GameState.initial();

        GameState second =
                GameState.initial();

        second.setSideToMove(
                PieceColor.BLACK
        );

        assertNotEquals(
                positionKeyFactory.create(first),
                positionKeyFactory.create(second)
        );
    }

    @Test
    void differentCastlingRightsShouldProduceDifferentPositionKey() {

        GameState first =
                GameState.initial();

        GameState second =
                GameState.initial();

        second.setWhiteKingSideCastlingAllowed(
                false
        );

        assertNotEquals(
                positionKeyFactory.create(first),
                positionKeyFactory.create(second)
        );
    }

    @Test
    void differentBoardPositionShouldProduceDifferentPositionKey() {

        GameState first =
                GameState.initial();

        GameState second =
                GameState.initial();

        second.getBoard()
                .clearSquare(
                        Square.fromAlgebraic("e2")
                );

        second.getBoard()
                .setPiece(
                        Square.fromAlgebraic("e4"),
                        new Piece(
                                PieceType.PAWN,
                                PieceColor.WHITE
                        )
                );

        assertNotEquals(
                positionKeyFactory.create(first),
                positionKeyFactory.create(second)
        );
    }

    @Test
    void legalEnPassantTargetShouldBePartOfPositionKey() {

        Board board =
                new Board();

        board.setPiece(
                Square.fromAlgebraic("e1"),
                new Piece(
                        PieceType.KING,
                        PieceColor.WHITE
                )
        );

        board.setPiece(
                Square.fromAlgebraic("e8"),
                new Piece(
                        PieceType.KING,
                        PieceColor.BLACK
                )
        );

        board.setPiece(
                Square.fromAlgebraic("e5"),
                new Piece(
                        PieceType.PAWN,
                        PieceColor.WHITE
                )
        );

        board.setPiece(
                Square.fromAlgebraic("d5"),
                new Piece(
                        PieceType.PAWN,
                        PieceColor.BLACK
                )
        );

        GameState withEnPassant =
                createState(
                        board.copy(),
                        Square.fromAlgebraic("d6")
                );

        GameState withoutEnPassant =
                createState(
                        board.copy(),
                        null
                );

        assertNotEquals(
                positionKeyFactory.create(withEnPassant),
                positionKeyFactory.create(withoutEnPassant)
        );
    }

    @Test
    void illegalEnPassantTargetShouldNotChangePositionKey() {

        Board board =
                new Board();

        board.setPiece(
                Square.fromAlgebraic("e5"),
                new Piece(
                        PieceType.KING,
                        PieceColor.WHITE
                )
        );

        board.setPiece(
                Square.fromAlgebraic("f5"),
                new Piece(
                        PieceType.PAWN,
                        PieceColor.WHITE
                )
        );

        board.setPiece(
                Square.fromAlgebraic("g5"),
                new Piece(
                        PieceType.PAWN,
                        PieceColor.BLACK
                )
        );

        board.setPiece(
                Square.fromAlgebraic("h5"),
                new Piece(
                        PieceType.ROOK,
                        PieceColor.BLACK
                )
        );

        board.setPiece(
                Square.fromAlgebraic("a8"),
                new Piece(
                        PieceType.KING,
                        PieceColor.BLACK
                )
        );

        GameState withTarget =
                createState(
                        board.copy(),
                        Square.fromAlgebraic("g6")
                );

        GameState withoutTarget =
                createState(
                        board.copy(),
                        null
                );

        /*
         * f5xg6 e.p. would expose the white king
         * to the rook on h5, therefore the
         * en-passant right does not change the
         * set of legal moves.
         */
        assertEquals(
                positionKeyFactory.create(withTarget),
                positionKeyFactory.create(withoutTarget)
        );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private GameState createState(
            Board board,
            Square enPassantTarget
    ) {

        return new GameState(
                board,
                PieceColor.WHITE,
                false,
                false,
                false,
                false,
                enPassantTarget,
                0,
                1
        );
    }
}
