package com.example.chessforge.service.game.engine.model;

public class Board {

    private static final int SIZE = 8;

    /*
     * First index  -> rank
     * Second index -> file
     *
     * squares[0][0] = a1
     * squares[3][4] = e4
     * squares[7][7] = h8
     */
    private final Piece[][] squares;

    public Board() {

        this.squares =
                new Piece[SIZE][SIZE];
    }

    private Board(
            Piece[][] squares
    ) {

        this.squares =
                squares;
    }

    // =========================================================
    // INITIAL POSITION
    // =========================================================

    public static Board initial() {

        Board board =
                new Board();

        board.setupBackRank(
                PieceColor.WHITE,
                0
        );

        board.setupPawns(
                PieceColor.WHITE,
                1
        );

        board.setupPawns(
                PieceColor.BLACK,
                6
        );

        board.setupBackRank(
                PieceColor.BLACK,
                7
        );

        return board;
    }

    // =========================================================
    // ACCESS
    // =========================================================

    public Piece getPiece(
            Square square
    ) {

        requireSquare(square);

        return squares
                [square.rank()]
                [square.file()];
    }

    public void setPiece(
            Square square,
            Piece piece
    ) {

        requireSquare(square);

        squares
                [square.rank()]
                [square.file()] =
                piece;
    }

    public void clearSquare(
            Square square
    ) {

        setPiece(
                square,
                null
        );
    }

    public boolean isEmpty(
            Square square
    ) {

        return getPiece(square)
                == null;
    }

    // =========================================================
    // COPY
    // =========================================================

    public Board copy() {

        Piece[][] copy =
                new Piece[SIZE][SIZE];

        for (int rank = 0;
             rank < SIZE;
             rank++) {

            System.arraycopy(
                    squares[rank],
                    0,
                    copy[rank],
                    0,
                    SIZE
            );
        }

        return new Board(
                copy
        );
    }

    // =========================================================
    // SETUP
    // =========================================================

    private void setupPawns(
            PieceColor color,
            int rank
    ) {

        for (int file = 0;
             file < SIZE;
             file++) {

            squares[rank][file] =
                    new Piece(
                            PieceType.PAWN,
                            color
                    );
        }
    }

    private void setupBackRank(
            PieceColor color,
            int rank
    ) {

        PieceType[] order = {
                PieceType.ROOK,
                PieceType.KNIGHT,
                PieceType.BISHOP,
                PieceType.QUEEN,
                PieceType.KING,
                PieceType.BISHOP,
                PieceType.KNIGHT,
                PieceType.ROOK
        };

        for (int file = 0;
             file < SIZE;
             file++) {

            squares[rank][file] =
                    new Piece(
                            order[file],
                            color
                    );
        }
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    private void requireSquare(
            Square square
    ) {

        if (square == null) {

            throw new IllegalArgumentException(
                    "Square cannot be null."
            );
        }
    }
}