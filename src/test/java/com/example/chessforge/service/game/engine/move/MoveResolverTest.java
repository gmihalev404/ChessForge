package com.example.chessforge.service.game.engine.move;

import com.example.chessforge.service.game.engine.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoveResolverTest {

    @Mock
    private LegalMoveGenerator legalMoveGenerator;

    private MoveResolver moveResolver;

    private GameState state;

    @BeforeEach
    void setUp() {

        moveResolver =
                new MoveResolver(
                        legalMoveGenerator
                );

        state =
                GameState.initial();
    }

    @Test
    void shouldResolveNormalMove() {

        Square from =
                Square.fromAlgebraic("e2");

        Square to =
                Square.fromAlgebraic("e4");

        Move legalMove =
                new Move(
                        from,
                        to,
                        MoveType.NORMAL,
                        null
                );

        when(legalMoveGenerator
                .generateLegalMoves(
                        state,
                        from
                ))
                .thenReturn(
                        List.of(legalMove)
                );

        Move result =
                moveResolver.resolve(
                        state,
                        from,
                        to,
                        null
                );

        assertSame(
                legalMove,
                result
        );
    }

    @Test
    void shouldResolveKingSideCastling() {

        Square from =
                Square.fromAlgebraic("e1");

        Square to =
                Square.fromAlgebraic("g1");

        Move castling =
                new Move(
                        from,
                        to,
                        MoveType.CASTLE_KING_SIDE,
                        null
                );

        when(legalMoveGenerator
                .generateLegalMoves(
                        state,
                        from
                ))
                .thenReturn(
                        List.of(castling)
                );

        Move result =
                moveResolver.resolve(
                        state,
                        from,
                        to,
                        null
                );

        assertEquals(
                MoveType.CASTLE_KING_SIDE,
                result.type()
        );
    }

    @Test
    void shouldResolveEnPassant() {

        Square from =
                Square.fromAlgebraic("e5");

        Square to =
                Square.fromAlgebraic("d6");

        Move enPassant =
                new Move(
                        from,
                        to,
                        MoveType.EN_PASSANT,
                        null
                );

        when(legalMoveGenerator
                .generateLegalMoves(
                        state,
                        from
                ))
                .thenReturn(
                        List.of(enPassant)
                );

        Move result =
                moveResolver.resolve(
                        state,
                        from,
                        to,
                        null
                );

        assertEquals(
                MoveType.EN_PASSANT,
                result.type()
        );
    }

    @Test
    void shouldResolveRequestedPromotionPiece() {

        Square from =
                Square.fromAlgebraic("e7");

        Square to =
                Square.fromAlgebraic("e8");

        Move queenPromotion =
                new Move(
                        from,
                        to,
                        MoveType.PROMOTION,
                        PieceType.QUEEN
                );

        Move rookPromotion =
                new Move(
                        from,
                        to,
                        MoveType.PROMOTION,
                        PieceType.ROOK
                );

        Move bishopPromotion =
                new Move(
                        from,
                        to,
                        MoveType.PROMOTION,
                        PieceType.BISHOP
                );

        Move knightPromotion =
                new Move(
                        from,
                        to,
                        MoveType.PROMOTION,
                        PieceType.KNIGHT
                );

        when(legalMoveGenerator
                .generateLegalMoves(
                        state,
                        from
                ))
                .thenReturn(
                        List.of(
                                queenPromotion,
                                rookPromotion,
                                bishopPromotion,
                                knightPromotion
                        )
                );

        Move result =
                moveResolver.resolve(
                        state,
                        from,
                        to,
                        PieceType.QUEEN
                );

        assertSame(
                queenPromotion,
                result
        );

        assertEquals(
                PieceType.QUEEN,
                result.promotion()
        );
    }

    @Test
    void shouldRejectPromotionWithoutPromotionPiece() {

        Square from =
                Square.fromAlgebraic("e7");

        Square to =
                Square.fromAlgebraic("e8");

        Move promotion =
                new Move(
                        from,
                        to,
                        MoveType.PROMOTION,
                        PieceType.QUEEN
                );

        when(legalMoveGenerator
                .generateLegalMoves(
                        state,
                        from
                ))
                .thenReturn(
                        List.of(promotion)
                );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        moveResolver.resolve(
                                state,
                                from,
                                to,
                                null
                        )
        );
    }

    @Test
    void shouldRejectIllegalTargetSquare() {

        Square from =
                Square.fromAlgebraic("e2");

        Square legalTarget =
                Square.fromAlgebraic("e4");

        Square requestedTarget =
                Square.fromAlgebraic("e5");

        when(legalMoveGenerator
                .generateLegalMoves(
                        state,
                        from
                ))
                .thenReturn(
                        List.of(
                                new Move(
                                        from,
                                        legalTarget,
                                        MoveType.NORMAL,
                                        null
                                )
                        )
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                moveResolver.resolve(
                                        state,
                                        from,
                                        requestedTarget,
                                        null
                                )
                );

        assertEquals(
                "No legal move matches the request.",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectPromotionPieceForNormalMove() {

        Square from =
                Square.fromAlgebraic("e2");

        Square to =
                Square.fromAlgebraic("e4");

        when(legalMoveGenerator
                .generateLegalMoves(
                        state,
                        from
                ))
                .thenReturn(
                        List.of(
                                new Move(
                                        from,
                                        to,
                                        MoveType.NORMAL,
                                        null
                                )
                        )
                );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        moveResolver.resolve(
                                state,
                                from,
                                to,
                                PieceType.QUEEN
                        )
        );
    }
}