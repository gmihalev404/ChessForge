package com.example.chessforge.service.game.engine.model;

public record Square(
        int file,
        int rank
) {

    public static final int BOARD_SIZE = 8;

    public Square {

        if (!isValidCoordinate(file)
                || !isValidCoordinate(rank)) {

            throw new IllegalArgumentException(
                    "Square coordinates must be between 0 and 7."
            );
        }
    }

    public static Square fromAlgebraic(
            String notation
    ) {

        if (notation == null
                || notation.length() != 2) {

            throw new IllegalArgumentException(
                    "Square notation must contain exactly two characters."
            );
        }

        char fileCharacter =
                Character.toLowerCase(
                        notation.charAt(0)
                );

        char rankCharacter =
                notation.charAt(1);

        if (fileCharacter < 'a'
                || fileCharacter > 'h'
                || rankCharacter < '1'
                || rankCharacter > '8') {

            throw new IllegalArgumentException(
                    "Invalid square notation: "
                            + notation
            );
        }

        int file =
                fileCharacter - 'a';

        int rank =
                rankCharacter - '1';

        return new Square(
                file,
                rank
        );
    }

    public String toAlgebraic() {

        char fileCharacter =
                (char) ('a' + file);

        char rankCharacter =
                (char) ('1' + rank);

        return String.valueOf(
                new char[]{
                        fileCharacter,
                        rankCharacter
                }
        );
    }

    private static boolean isValidCoordinate(
            int coordinate
    ) {

        return coordinate >= 0
                && coordinate < BOARD_SIZE;
    }

    @Override
    public String toString() {

        return toAlgebraic();
    }
}