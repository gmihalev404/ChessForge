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

    @Test
    void shouldDetectFiftyMoveRuleDraw() {

        GameState state =
                GameState.initial();

        state.setHalfMoveClock(
                100
        );

        assertTrue(
                positionEvaluator
                        .isFiftyMoveRuleDraw(
                                state
                        )
        );
    }

    @Test
    void shouldNotDetectFiftyMoveRuleBeforeOneHundredHalfMoves() {

        GameState state =
                GameState.initial();

        state.setHalfMoveClock(
                99
        );

        assertFalse(
                positionEvaluator
                        .isFiftyMoveRuleDraw(
                                state
                        )
        );
    }

    @Test
    void kingVersusKingShouldBeInsufficientMaterial() {

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

        GameState state =
                createState(
                        board,
                        PieceColor.WHITE
                );

        assertTrue(
                positionEvaluator
                        .isInsufficientMaterial(
                                state
                        )
        );
    }

    @Test
    void kingAndBishopVersusKingShouldBeInsufficientMaterial() {

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
                Square.fromAlgebraic("c1"),
                new Piece(
                        PieceType.BISHOP,
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

        GameState state =
                createState(
                        board,
                        PieceColor.WHITE
                );

        assertTrue(
                positionEvaluator
                        .isInsufficientMaterial(
                                state
                        )
        );
    }

    @Test
    void kingAndKnightVersusKingShouldBeInsufficientMaterial() {

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
                Square.fromAlgebraic("g1"),
                new Piece(
                        PieceType.KNIGHT,
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

        GameState state =
                createState(
                        board,
                        PieceColor.WHITE
                );

        assertTrue(
                positionEvaluator
                        .isInsufficientMaterial(
                                state
                        )
        );
    }

    @Test
    void bishopsOnSameColoredSquaresShouldBeInsufficientMaterial() {

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
                Square.fromAlgebraic("c1"),
                new Piece(
                        PieceType.BISHOP,
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
                Square.fromAlgebraic("f8"),
                new Piece(
                        PieceType.BISHOP,
                        PieceColor.BLACK
                )
        );

        GameState state =
                createState(
                        board,
                        PieceColor.WHITE
                );

        assertTrue(
                positionEvaluator
                        .isInsufficientMaterial(
                                state
                        )
        );
    }

    @Test
    void bishopsOnOppositeColoredSquaresShouldNotBeMarkedAsInsufficientMaterial() {

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
                Square.fromAlgebraic("c1"),
                new Piece(
                        PieceType.BISHOP,
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
                Square.fromAlgebraic("c8"),
                new Piece(
                        PieceType.BISHOP,
                        PieceColor.BLACK
                )
        );

        GameState state =
                createState(
                        board,
                        PieceColor.WHITE
                );

        assertFalse(
                positionEvaluator
                        .isInsufficientMaterial(
                                state
                        )
        );
    }

    @Test
    void queenShouldMeanMaterialIsSufficient() {

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
                Square.fromAlgebraic("d1"),
                new Piece(
                        PieceType.QUEEN,
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

        GameState state =
                createState(
                        board,
                        PieceColor.WHITE
                );

        assertFalse(
                positionEvaluator
                        .isInsufficientMaterial(
                                state
                        )
        );
    }

    @Test
    void bishopAndKnightShouldNotBeInsufficientMaterial() {

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
                Square.fromAlgebraic("c1"),
                new Piece(
                        PieceType.BISHOP,
                        PieceColor.WHITE
                )
        );

        board.setPiece(
                Square.fromAlgebraic("g1"),
                new Piece(
                        PieceType.KNIGHT,
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

        GameState state =
                createState(
                        board,
                        PieceColor.WHITE
                );

        assertFalse(
                positionEvaluator
                        .isInsufficientMaterial(
                                state
                        )
        );
    }

    @Test
    void shouldDetectSeventyFiveMoveRuleAtOneHundredFiftyHalfMoves() {

        GameState state =
                GameState.initial();

        state.setHalfMoveClock(150);

        assertTrue(
                positionEvaluator
                        .isSeventyFiveMoveRuleDraw(
                                state
                        )
        );
    }

    @Test
    void shouldNotDetectSeventyFiveMoveRuleBeforeOneHundredFiftyHalfMoves() {

        GameState state =
                GameState.initial();

        state.setHalfMoveClock(149);

        assertFalse(
                positionEvaluator
                        .isSeventyFiveMoveRuleDraw(
                                state
                        )
        );
    }

    @Test
    void shouldHaveInsufficientMatingMaterialWithKingOnly() {

        GameState state =
                createEmptyState();

        place(
                state,
                "e1",
                PieceType.KING,
                PieceColor.WHITE
        );

        place(
                state,
                "e8",
                PieceType.KING,
                PieceColor.BLACK
        );

        assertTrue(
                positionEvaluator
                        .hasInsufficientMatingMaterial(
                                state,
                                PieceColor.WHITE
                        )
        );
    }

    @Test
    void shouldHaveInsufficientMatingMaterialWithBishopAgainstBareKing() {

        GameState state =
                createEmptyState();

        place(
                state,
                "e1",
                PieceType.KING,
                PieceColor.WHITE
        );

        place(
                state,
                "c1",
                PieceType.BISHOP,
                PieceColor.WHITE
        );

        place(
                state,
                "e8",
                PieceType.KING,
                PieceColor.BLACK
        );

        assertTrue(
                positionEvaluator
                        .hasInsufficientMatingMaterial(
                                state,
                                PieceColor.WHITE
                        )
        );
    }

    @Test
    void shouldHaveInsufficientMatingMaterialWithKnightAgainstBareKing() {

        GameState state =
                createEmptyState();

        place(
                state,
                "e1",
                PieceType.KING,
                PieceColor.WHITE
        );

        place(
                state,
                "g1",
                PieceType.KNIGHT,
                PieceColor.WHITE
        );

        place(
                state,
                "e8",
                PieceType.KING,
                PieceColor.BLACK
        );

        assertTrue(
                positionEvaluator
                        .hasInsufficientMatingMaterial(
                                state,
                                PieceColor.WHITE
                        )
        );
    }

    @Test
    void shouldHaveSufficientMatingMaterialWithQueen() {

        GameState state =
                createEmptyState();

        place(state, "e1", PieceType.KING, PieceColor.WHITE);
        place(state, "d1", PieceType.QUEEN, PieceColor.WHITE);
        place(state, "e8", PieceType.KING, PieceColor.BLACK);

        assertFalse(
                positionEvaluator
                        .hasInsufficientMatingMaterial(
                                state,
                                PieceColor.WHITE
                        )
        );
    }

    @Test
    void shouldHaveSufficientMatingMaterialWithRook() {

        GameState state =
                createEmptyState();

        place(state, "e1", PieceType.KING, PieceColor.WHITE);
        place(state, "a1", PieceType.ROOK, PieceColor.WHITE);
        place(state, "e8", PieceType.KING, PieceColor.BLACK);

        assertFalse(
                positionEvaluator
                        .hasInsufficientMatingMaterial(
                                state,
                                PieceColor.WHITE
                        )
        );
    }

    @Test
    void shouldHaveSufficientMatingMaterialWithPawn() {

        GameState state =
                createEmptyState();

        place(state, "e1", PieceType.KING, PieceColor.WHITE);
        place(state, "e2", PieceType.PAWN, PieceColor.WHITE);
        place(state, "e8", PieceType.KING, PieceColor.BLACK);

        assertFalse(
                positionEvaluator
                        .hasInsufficientMatingMaterial(
                                state,
                                PieceColor.WHITE
                        )
        );
    }

    @Test
    void shouldHaveSufficientMatingMaterialWithBishopAndKnight() {

        GameState state =
                createEmptyState();

        place(state, "e1", PieceType.KING, PieceColor.WHITE);
        place(state, "c1", PieceType.BISHOP, PieceColor.WHITE);
        place(state, "g1", PieceType.KNIGHT, PieceColor.WHITE);

        place(state, "e8", PieceType.KING, PieceColor.BLACK);

        assertFalse(
                positionEvaluator
                        .hasInsufficientMatingMaterial(
                                state,
                                PieceColor.WHITE
                        )
        );
    }

    @Test
    void shouldHaveSufficientMatingMaterialWithTwoKnights() {

        GameState state =
                createEmptyState();

        place(state, "e1", PieceType.KING, PieceColor.WHITE);

        place(state, "b1", PieceType.KNIGHT, PieceColor.WHITE);
        place(state, "g1", PieceType.KNIGHT, PieceColor.WHITE);

        place(state, "e8", PieceType.KING, PieceColor.BLACK);

        assertFalse(
                positionEvaluator
                        .hasInsufficientMatingMaterial(
                                state,
                                PieceColor.WHITE
                        )
        );
    }

    @Test
    void bishopShouldHavePossibleMateWhenOpponentHasKnight() {

        GameState state =
                createEmptyState();

        place(state, "e1", PieceType.KING, PieceColor.WHITE);
        place(state, "c1", PieceType.BISHOP, PieceColor.WHITE);

        place(state, "e8", PieceType.KING, PieceColor.BLACK);
        place(state, "g8", PieceType.KNIGHT, PieceColor.BLACK);

        assertFalse(
                positionEvaluator
                        .hasInsufficientMatingMaterial(
                                state,
                                PieceColor.WHITE
                        )
        );
    }

    @Test
    void knightShouldHavePossibleMateWhenOpponentHasBishop() {

        GameState state =
                createEmptyState();

        place(state, "e1", PieceType.KING, PieceColor.WHITE);
        place(state, "g1", PieceType.KNIGHT, PieceColor.WHITE);

        place(state, "e8", PieceType.KING, PieceColor.BLACK);
        place(state, "c8", PieceType.BISHOP, PieceColor.BLACK);

        assertFalse(
                positionEvaluator
                        .hasInsufficientMatingMaterial(
                                state,
                                PieceColor.WHITE
                        )
        );
    }

    @Test
    void bishopsOnBothSquareColorsShouldBeSufficient() {

        GameState state =
                createEmptyState();

        place(state, "e1", PieceType.KING, PieceColor.WHITE);

        place(state, "c1", PieceType.BISHOP, PieceColor.WHITE);
        place(state, "f1", PieceType.BISHOP, PieceColor.WHITE);

        place(state, "e8", PieceType.KING, PieceColor.BLACK);

        assertFalse(
                positionEvaluator
                        .hasInsufficientMatingMaterial(
                                state,
                                PieceColor.WHITE
                        )
        );
    }

    @Test
    void sameColoredBishopsAgainstBareKingShouldBeInsufficient() {

        GameState state =
                createEmptyState();

        place(state, "e1", PieceType.KING, PieceColor.WHITE);

        place(state, "c1", PieceType.BISHOP, PieceColor.WHITE);
        place(state, "e3", PieceType.BISHOP, PieceColor.WHITE);

        place(state, "e8", PieceType.KING, PieceColor.BLACK);

        assertTrue(
                positionEvaluator
                        .hasInsufficientMatingMaterial(
                                state,
                                PieceColor.WHITE
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

    private GameState createEmptyState() {

        GameState state =
                GameState.initial();

        Board board =
                state.getBoard();

        for (int rank = 0; rank < 8; rank++) {

            for (int file = 0; file < 8; file++) {

                board.clearSquare(
                        new Square(
                                file,
                                rank
                        )
                );
            }
        }

        return state;
    }

    private void place(
            GameState state,
            String square,
            PieceType type,
            PieceColor color
    ) {

        state.getBoard()
                .setPiece(
                        Square.fromAlgebraic(
                                square
                        ),
                        new Piece(
                                type,
                                color
                        )
                );
    }
}