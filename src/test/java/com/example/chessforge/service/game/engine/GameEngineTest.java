package com.example.chessforge.service.game.engine;

import com.example.chessforge.service.game.engine.history.PositionKeyFactory;
import com.example.chessforge.service.game.engine.history.RepetitionTracker;
import com.example.chessforge.service.game.engine.model.*;
import com.example.chessforge.service.game.engine.move.LegalMoveGenerator;
import com.example.chessforge.service.game.engine.move.MoveApplier;
import com.example.chessforge.service.game.engine.move.MoveGenerator;
import com.example.chessforge.service.game.engine.move.MoveResolver;
import com.example.chessforge.service.game.engine.notation.FenConverter;
import com.example.chessforge.service.game.engine.notation.SanGenerator;
import com.example.chessforge.service.game.engine.rule.AttackDetector;
import com.example.chessforge.service.game.engine.rule.DrawEvaluator;
import com.example.chessforge.service.game.engine.rule.PositionEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameEngineTest {

    private GameEngine gameEngine;

    private PositionKeyFactory positionKeyFactory;

    @Mock
    private SanGenerator sanGenerator;

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

        PositionEvaluator positionEvaluator =
                new PositionEvaluator(
                        legalMoveGenerator,
                        attackDetector
                );

        DrawEvaluator drawEvaluator =
                new DrawEvaluator(
                        positionEvaluator
                );

        FenConverter fenConverter =
                new FenConverter();

        positionKeyFactory =
                new PositionKeyFactory(legalMoveGenerator);

        MoveResolver moveResolver =
                new MoveResolver(
                        legalMoveGenerator
                );

        gameEngine =
                new GameEngine(
                        legalMoveGenerator,
                        moveApplier,
                        attackDetector,
                        positionEvaluator,
                        drawEvaluator,
                        fenConverter,
                        positionKeyFactory,
                        sanGenerator,
                        moveResolver
                );
    }

    @Test
    void initialPositionShouldHaveTwentyLegalMoves() {

        GameState state =
                GameState.initial();

        List<Move> moves =
                gameEngine
                        .getAllLegalMoves(
                                state
                        );

        assertEquals(
                20,
                moves.size()
        );
    }

    @Test
    void shouldRecognizeLegalMove() {

        GameState state =
                GameState.initial();

        Move move =
                Move.normal(
                        "e2",
                        "e4"
                );

        assertTrue(
                gameEngine.isLegalMove(
                        state,
                        move
                )
        );
    }

    @Test
    void shouldRejectIllegalMove() {

        GameState state =
                GameState.initial();

        Move move =
                Move.normal(
                        "e2",
                        "e5"
                );

        assertFalse(
                gameEngine.isLegalMove(
                        state,
                        move
                )
        );
    }

    @Test
    void makeMoveShouldApplyLegalMove() {

        GameState state =
                GameState.initial();

        gameEngine.makeMove(
                state,
                Move.normal(
                        "e2",
                        "e4"
                )
        );

        assertTrue(
                state.getBoard()
                        .isEmpty(
                                Square.fromAlgebraic(
                                        "e2"
                                )
                        )
        );

        assertEquals(
                new Piece(
                        PieceType.PAWN,
                        PieceColor.WHITE
                ),
                state.getBoard()
                        .getPiece(
                                Square.fromAlgebraic(
                                        "e4"
                                )
                        )
        );

        assertEquals(
                PieceColor.BLACK,
                state.getSideToMove()
        );

        assertEquals(
                Square.fromAlgebraic("e3"),
                state.getEnPassantTarget()
        );
    }

    @Test
    void makeMoveShouldRejectIllegalMoveWithoutChangingState() {

        GameState state =
                GameState.initial();

        Move illegalMove =
                Move.normal(
                        "e2",
                        "e5"
                );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        gameEngine.makeMove(
                                state,
                                illegalMove
                        )
        );

        assertEquals(
                new Piece(
                        PieceType.PAWN,
                        PieceColor.WHITE
                ),
                state.getBoard()
                        .getPiece(
                                Square.fromAlgebraic(
                                        "e2"
                                )
                        )
        );

        assertTrue(
                state.getBoard()
                        .isEmpty(
                                Square.fromAlgebraic(
                                        "e5"
                                )
                        )
        );

        assertEquals(
                PieceColor.WHITE,
                state.getSideToMove()
        );
    }

    @Test
    void makeMoveShouldRejectMoveThatExposesOwnKing() {

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
                Square.fromAlgebraic("e2"),
                new Piece(
                        PieceType.KNIGHT,
                        PieceColor.WHITE
                )
        );

        board.setPiece(
                Square.fromAlgebraic("e8"),
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

        GameState state =
                createState(
                        board,
                        PieceColor.WHITE
                );

        Move move =
                Move.normal(
                        "e2",
                        "f4"
                );

        assertFalse(
                gameEngine.isLegalMove(
                        state,
                        move
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        gameEngine.makeMove(
                                state,
                                move
                        )
        );
    }

    @Test
    void shouldDetectCheck() {

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

        GameState state =
                createState(
                        board,
                        PieceColor.WHITE
                );

        assertTrue(
                gameEngine.isInCheck(
                        state
                )
        );

        assertTrue(
                gameEngine.isInCheck(
                        state,
                        PieceColor.WHITE
                )
        );

        assertFalse(
                gameEngine.isInCheck(
                        state,
                        PieceColor.BLACK
                )
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
                gameEngine.isCheckmate(
                        state
                )
        );

        assertFalse(
                gameEngine.isStalemate(
                        state
                )
        );
    }

    @Test
    void shouldReturnLegalMovesForPiece() {

        GameState state =
                GameState.initial();

        List<Move> moves =
                gameEngine.getLegalMoves(
                        state,
                        Square.fromAlgebraic("e2")
                );

        assertEquals(
                2,
                moves.size()
        );

        assertTrue(
                moves.contains(
                        Move.normal("e2", "e3")
                )
        );

        assertTrue(
                moves.contains(
                        Move.normal("e2", "e4")
                )
        );
    }

    // =========================================================
    // DRAW
    // =========================================================

    @Test
    void shouldExposeClaimableThreefoldRepetition() {

        GameState state =
                GameState.initial();

        RepetitionTracker tracker =
                new RepetitionTracker(
                        positionKeyFactory,
                        state
                );

        tracker.recordPosition(state);
        tracker.recordPosition(state);

        assertTrue(
                gameEngine.canClaimDraw(
                        state,
                        tracker
                )
        );

        assertTrue(
                gameEngine.getClaimableDrawReasons(
                        state,
                        tracker
                ).contains(
                        DrawReason.THREEFOLD_REPETITION
                )
        );
    }

    @Test
    void shouldExposeAutomaticFivefoldRepetition() {

        GameState state =
                GameState.initial();

        RepetitionTracker tracker =
                new RepetitionTracker(
                        positionKeyFactory,
                        state
                );

        tracker.recordPosition(state);
        tracker.recordPosition(state);
        tracker.recordPosition(state);
        tracker.recordPosition(state);

        assertTrue(
                gameEngine.isAutomaticDraw(
                        state,
                        tracker
                )
        );

        assertTrue(
                gameEngine.getAutomaticDrawReasons(
                        state,
                        tracker
                ).contains(
                        DrawReason.FIVEFOLD_REPETITION
                )
        );
    }

    @Test
    void shouldConvertGameStateToFenAndBack() {

        GameState original =
                GameState.initial();

        String fen =
                gameEngine.toFen(
                        original
                );

        assertEquals(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                fen
        );

        GameState restored =
                gameEngine.fromFen(
                        fen
                );

        assertEquals(
                fen,
                gameEngine.toFen(
                        restored
                )
        );
    }

    @Test
    void shouldGenerateSan() {

        GameState before =
                GameState.initial();

        GameState after =
                GameState.initial();

        Move move =
                new Move(
                        Square.fromAlgebraic("e2"),
                        Square.fromAlgebraic("e4")
                );

        when(sanGenerator.generate(
                before,
                move,
                after
        )).thenReturn(
                "e4"
        );

        assertEquals(
                "e4",
                gameEngine.generateSan(
                        before,
                        move,
                        after
                )
        );

        verify(sanGenerator)
                .generate(
                        before,
                        move,
                        after
                );
    }

    @Test
    void shouldResolveRequestedMove() {

        GameState state =
                GameState.initial();

        Move move =
                gameEngine.resolveMove(
                        state,
                        Square.fromAlgebraic("e2"),
                        Square.fromAlgebraic("e4"),
                        null
                );

        assertEquals(
                Square.fromAlgebraic("e2"),
                move.from()
        );

        assertEquals(
                Square.fromAlgebraic("e4"),
                move.to()
        );

        assertEquals(
                MoveType.NORMAL,
                move.type()
        );

        assertNull(
                move.promotion()
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