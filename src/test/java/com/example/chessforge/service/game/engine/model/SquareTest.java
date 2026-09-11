package com.example.chessforge.service.game.engine.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SquareTest {

    @Test
    void shouldCreateSquareWithValidCoordinates() {

        Square square =
                new Square(
                        4,
                        3
                );

        assertEquals(
                4,
                square.file()
        );

        assertEquals(
                3,
                square.rank()
        );
    }

    @Test
    void shouldRejectFileBelowBoard() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new Square(
                                -1,
                                0
                        )
        );
    }

    @Test
    void shouldRejectFileAboveBoard() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new Square(
                                8,
                                0
                        )
        );
    }

    @Test
    void shouldRejectRankBelowBoard() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new Square(
                                0,
                                -1
                        )
        );
    }

    @Test
    void shouldRejectRankAboveBoard() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new Square(
                                0,
                                8
                        )
        );
    }

    @Test
    void shouldCreateSquareFromAlgebraicNotation() {

        Square square =
                Square.fromAlgebraic(
                        "e4"
                );

        assertEquals(
                4,
                square.file()
        );

        assertEquals(
                3,
                square.rank()
        );
    }

    @Test
    void shouldAcceptUppercaseFileNotation() {

        Square square =
                Square.fromAlgebraic(
                        "E4"
                );

        assertEquals(
                new Square(
                        4,
                        3
                ),
                square
        );
    }

    @Test
    void shouldConvertSquareToAlgebraicNotation() {

        Square square =
                new Square(
                        7,
                        7
                );

        assertEquals(
                "h8",
                square.toAlgebraic()
        );
    }

    @Test
    void toStringShouldUseAlgebraicNotation() {

        Square square =
                new Square(
                        0,
                        0
                );

        assertEquals(
                "a1",
                square.toString()
        );
    }

    @Test
    void shouldRejectInvalidAlgebraicNotation() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        Square.fromAlgebraic(
                                "i4"
                        )
        );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        Square.fromAlgebraic(
                                "a9"
                        )
        );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        Square.fromAlgebraic(
                                "abc"
                        )
        );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        Square.fromAlgebraic(
                                null
                        )
        );
    }
}