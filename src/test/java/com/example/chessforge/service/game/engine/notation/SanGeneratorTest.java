package com.example.chessforge.service.game.engine.notation;

import com.example.chessforge.service.game.engine.model.Board;
import com.example.chessforge.service.game.engine.model.GameState;
import com.example.chessforge.service.game.engine.model.Move;
import com.example.chessforge.service.game.engine.model.MoveType;
import com.example.chessforge.service.game.engine.model.Piece;
import com.example.chessforge.service.game.engine.model.PieceColor;
import com.example.chessforge.service.game.engine.model.PieceType;
import com.example.chessforge.service.game.engine.model.Square;
import com.example.chessforge.service.game.engine.move.LegalMoveGenerator;
import com.example.chessforge.service.game.engine.rule.AttackDetector;
import com.example.chessforge.service.game.engine.rule.PositionEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SanGeneratorTest {

    @Mock
    private LegalMoveGenerator legalMoveGenerator;

    @Mock
    private AttackDetector attackDetector;

    @Mock
    private PositionEvaluator positionEvaluator;

    private SanGenerator sanGenerator;

    @BeforeEach
    void setUp() {

        sanGenerator =
                new SanGenerator(
                        legalMoveGenerator,
                        attackDetector,
                        positionEvaluator
                );
    }

    // =========================================================
    // BASIC MOVES
    // =========================================================

    @Test
    void shouldGeneratePawnMove() {

        GameState before =
                emptyState(
                        PieceColor.WHITE
                );

        GameState after =
                emptyState(
                        PieceColor.BLACK
                );

        place(
                before,
                "e2",
                PieceType.PAWN,
                PieceColor.WHITE
        );

        Move move =
                normalMove(
                        "e2",
                        "e4"
                );

        assertEquals(
                "e4",
                sanGenerator.generate(
                        before,
                        move,
                        after
                )
        );
    }

    @Test
    void shouldGenerateKnightMove() {

        GameState before =
                emptyState(
                        PieceColor.WHITE
                );

        GameState after =
                emptyState(
                        PieceColor.BLACK
                );

        place(
                before,
                "g1",
                PieceType.KNIGHT,
                PieceColor.WHITE
        );

        Move move =
                normalMove(
                        "g1",
                        "f3"
                );

        when(legalMoveGenerator
                .generateAllLegalMoves(before))
                .thenReturn(
                        List.of(move)
                );

        assertEquals(
                "Nf3",
                sanGenerator.generate(
                        before,
                        move,
                        after
                )
        );
    }

    // =========================================================
    // CAPTURES
    // =========================================================

    @Test
    void shouldGeneratePieceCapture() {

        GameState before =
                emptyState(
                        PieceColor.WHITE
                );

        GameState after =
                emptyState(
                        PieceColor.BLACK
                );

        place(
                before,
                "g2",
                PieceType.BISHOP,
                PieceColor.WHITE
        );

        place(
                before,
                "c6",
                PieceType.KNIGHT,
                PieceColor.BLACK
        );

        Move move =
                normalMove(
                        "g2",
                        "c6"
                );

        when(legalMoveGenerator
                .generateAllLegalMoves(before))
                .thenReturn(
                        List.of(move)
                );

        assertEquals(
                "Bxc6",
                sanGenerator.generate(
                        before,
                        move,
                        after
                )
        );
    }

    @Test
    void shouldGeneratePawnCapture() {

        GameState before =
                emptyState(
                        PieceColor.WHITE
                );

        GameState after =
                emptyState(
                        PieceColor.BLACK
                );

        place(
                before,
                "e4",
                PieceType.PAWN,
                PieceColor.WHITE
        );

        place(
                before,
                "d5",
                PieceType.PAWN,
                PieceColor.BLACK
        );

        Move move =
                normalMove(
                        "e4",
                        "d5"
                );

        assertEquals(
                "exd5",
                sanGenerator.generate(
                        before,
                        move,
                        after
                )
        );
    }

    @Test
    void shouldGenerateEnPassantCapture() {

        GameState before =
                emptyState(
                        PieceColor.WHITE
                );

        GameState after =
                emptyState(
                        PieceColor.BLACK
                );

        place(
                before,
                "e5",
                PieceType.PAWN,
                PieceColor.WHITE
        );

        Move move =
                new Move(
                        Square.fromAlgebraic("e5"),
                        Square.fromAlgebraic("d6"),
                        MoveType.EN_PASSANT,
                        null
                );

        assertEquals(
                "exd6",
                sanGenerator.generate(
                        before,
                        move,
                        after
                )
        );
    }

    // =========================================================
    // CASTLING
    // =========================================================

    @Test
    void shouldGenerateKingSideCastling() {

        GameState before =
                emptyState(
                        PieceColor.WHITE
                );

        GameState after =
                emptyState(
                        PieceColor.BLACK
                );

        Move move =
                new Move(
                        Square.fromAlgebraic("e1"),
                        Square.fromAlgebraic("g1"),
                        MoveType.CASTLE_KING_SIDE,
                        null
                );

        assertEquals(
                "O-O",
                sanGenerator.generate(
                        before,
                        move,
                        after
                )
        );
    }

    @Test
    void shouldGenerateQueenSideCastling() {

        GameState before =
                emptyState(
                        PieceColor.WHITE
                );

        GameState after =
                emptyState(
                        PieceColor.BLACK
                );

        Move move =
                new Move(
                        Square.fromAlgebraic("e1"),
                        Square.fromAlgebraic("c1"),
                        MoveType.CASTLE_QUEEN_SIDE,
                        null
                );

        assertEquals(
                "O-O-O",
                sanGenerator.generate(
                        before,
                        move,
                        after
                )
        );
    }

    // =========================================================
    // PROMOTION
    // =========================================================

    @Test
    void shouldGeneratePromotion() {

        GameState before =
                emptyState(
                        PieceColor.WHITE
                );

        GameState after =
                emptyState(
                        PieceColor.BLACK
                );

        place(
                before,
                "e7",
                PieceType.PAWN,
                PieceColor.WHITE
        );

        Move move =
                new Move(
                        Square.fromAlgebraic("e7"),
                        Square.fromAlgebraic("e8"),
                        MoveType.PROMOTION,
                        PieceType.QUEEN
                );

        assertEquals(
                "e8=Q",
                sanGenerator.generate(
                        before,
                        move,
                        after
                )
        );
    }

    // =========================================================
    // CHECK / CHECKMATE
    // =========================================================

    @Test
    void shouldAppendCheck() {

        GameState before =
                emptyState(
                        PieceColor.WHITE
                );

        GameState after =
                emptyState(
                        PieceColor.BLACK
                );

        place(
                before,
                "h5",
                PieceType.QUEEN,
                PieceColor.WHITE
        );

        Move move =
                normalMove(
                        "h5",
                        "h7"
                );

        when(legalMoveGenerator
                .generateAllLegalMoves(before))
                .thenReturn(
                        List.of(move)
                );

        when(positionEvaluator
                .isCheckmate(after))
                .thenReturn(false);

        when(attackDetector.isInCheck(
                after,
                PieceColor.BLACK
        )).thenReturn(true);

        assertEquals(
                "Qh7+",
                sanGenerator.generate(
                        before,
                        move,
                        after
                )
        );
    }

    @Test
    void shouldAppendCheckmate() {

        GameState before =
                emptyState(
                        PieceColor.WHITE
                );

        GameState after =
                emptyState(
                        PieceColor.BLACK
                );

        place(
                before,
                "h5",
                PieceType.QUEEN,
                PieceColor.WHITE
        );

        Move move =
                normalMove(
                        "h5",
                        "h7"
                );

        when(legalMoveGenerator
                .generateAllLegalMoves(before))
                .thenReturn(
                        List.of(move)
                );

        when(positionEvaluator
                .isCheckmate(after))
                .thenReturn(true);

        assertEquals(
                "Qh7#",
                sanGenerator.generate(
                        before,
                        move,
                        after
                )
        );

        verify(
                attackDetector,
                never()
        ).isInCheck(
                after,
                PieceColor.BLACK
        );
    }

    // =========================================================
    // DISAMBIGUATION
    // =========================================================

    @Test
    void shouldDisambiguateByFile() {

        GameState before =
                emptyState(
                        PieceColor.WHITE
                );

        GameState after =
                emptyState(
                        PieceColor.BLACK
                );

        place(
                before,
                "b1",
                PieceType.KNIGHT,
                PieceColor.WHITE
        );

        place(
                before,
                "f1",
                PieceType.KNIGHT,
                PieceColor.WHITE
        );

        Move currentMove =
                normalMove(
                        "b1",
                        "d2"
                );

        Move competingMove =
                normalMove(
                        "f1",
                        "d2"
                );

        when(legalMoveGenerator
                .generateAllLegalMoves(before))
                .thenReturn(
                        List.of(
                                currentMove,
                                competingMove
                        )
                );

        assertEquals(
                "Nbd2",
                sanGenerator.generate(
                        before,
                        currentMove,
                        after
                )
        );
    }

    @Test
    void shouldDisambiguateByRank() {

        GameState before =
                emptyState(
                        PieceColor.WHITE
                );

        GameState after =
                emptyState(
                        PieceColor.BLACK
                );

        place(
                before,
                "a1",
                PieceType.ROOK,
                PieceColor.WHITE
        );

        place(
                before,
                "a3",
                PieceType.ROOK,
                PieceColor.WHITE
        );

        Move currentMove =
                normalMove(
                        "a1",
                        "a2"
                );

        Move competingMove =
                normalMove(
                        "a3",
                        "a2"
                );

        when(legalMoveGenerator
                .generateAllLegalMoves(before))
                .thenReturn(
                        List.of(
                                currentMove,
                                competingMove
                        )
                );

        assertEquals(
                "R1a2",
                sanGenerator.generate(
                        before,
                        currentMove,
                        after
                )
        );
    }

    @Test
    void shouldDisambiguateByFileAndRank() {

        GameState before =
                emptyState(
                        PieceColor.WHITE
                );

        GameState after =
                emptyState(
                        PieceColor.BLACK
                );

        place(
                before,
                "b1",
                PieceType.KNIGHT,
                PieceColor.WHITE
        );

        place(
                before,
                "b3",
                PieceType.KNIGHT,
                PieceColor.WHITE
        );

        place(
                before,
                "f1",
                PieceType.KNIGHT,
                PieceColor.WHITE
        );

        Move currentMove =
                normalMove(
                        "b1",
                        "d2"
                );

        Move sameFileMove =
                normalMove(
                        "b3",
                        "d2"
                );

        Move sameRankMove =
                normalMove(
                        "f1",
                        "d2"
                );

        when(legalMoveGenerator
                .generateAllLegalMoves(before))
                .thenReturn(
                        List.of(
                                currentMove,
                                sameFileMove,
                                sameRankMove
                        )
                );

        assertEquals(
                "Nb1d2",
                sanGenerator.generate(
                        before,
                        currentMove,
                        after
                )
        );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private GameState emptyState(
            PieceColor sideToMove
    ) {

        GameState state =
                GameState.initial();

        Board board =
                state.getBoard();

        for (int rank = 0;
             rank < Square.BOARD_SIZE;
             rank++) {

            for (int file = 0;
                 file < Square.BOARD_SIZE;
                 file++) {

                board.clearSquare(
                        new Square(
                                file,
                                rank
                        )
                );
            }
        }

        state.setSideToMove(
                sideToMove
        );

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

    private Move normalMove(
            String from,
            String to
    ) {

        return new Move(
                Square.fromAlgebraic(from),
                Square.fromAlgebraic(to)
        );
    }
}