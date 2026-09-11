package com.example.chessforge.service.game.engine.notation;

import com.example.chessforge.service.game.engine.model.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class FenConverter {

    private static final int FEN_FIELD_COUNT = 6;

    public String toFen(GameState state) {

        Objects.requireNonNull(
                state,
                "Game state cannot be null."
        );

        return String.join(
                " ",
                createPiecePlacement(state.getBoard()),
                createSideToMove(state.getSideToMove()),
                createCastlingRights(state),
                createEnPassantTarget(state),
                String.valueOf(state.getHalfMoveClock()),
                String.valueOf(state.getFullMoveNumber())
        );
    }

    public GameState fromFen(String fen) {

        Objects.requireNonNull(
                fen,
                "FEN cannot be null."
        );

        String[] fields =
                fen.trim().split("\\s+");

        if (fields.length != FEN_FIELD_COUNT) {

            throw new IllegalArgumentException(
                    "FEN must contain exactly 6 fields."
            );
        }

        Board board =
                parseBoard(fields[0]);

        PieceColor sideToMove =
                parseSideToMove(fields[1]);

        CastlingRights castlingRights =
                parseCastlingRights(fields[2]);

        Square enPassantTarget =
                parseEnPassantTarget(
                        fields[3],
                        sideToMove
                );

        int halfMoveClock =
                parseHalfMoveClock(fields[4]);

        int fullMoveNumber =
                parseFullMoveNumber(fields[5]);

        return new GameState(
                board,
                sideToMove,
                castlingRights.whiteKingSide(),
                castlingRights.whiteQueenSide(),
                castlingRights.blackKingSide(),
                castlingRights.blackQueenSide(),
                enPassantTarget,
                halfMoveClock,
                fullMoveNumber
        );
    }

    // =========================================================
    // SERIALIZATION
    // =========================================================

    private String createPiecePlacement(
            Board board
    ) {

        StringBuilder result =
                new StringBuilder();

        /*
         * FEN starts from rank 8 and finishes at rank 1.
         *
         * Our board:
         * rank 0 -> rank 1
         * rank 7 -> rank 8
         */
        for (int rank = Square.BOARD_SIZE - 1;
             rank >= 0;
             rank--) {

            int emptySquares = 0;

            for (int file = 0;
                 file < Square.BOARD_SIZE;
                 file++) {

                Piece piece =
                        board.getPiece(
                                new Square(
                                        file,
                                        rank
                                )
                        );

                if (piece == null) {

                    emptySquares++;
                    continue;
                }

                if (emptySquares > 0) {

                    result.append(
                            emptySquares
                    );

                    emptySquares = 0;
                }

                result.append(
                        pieceToFenSymbol(piece)
                );
            }

            if (emptySquares > 0) {

                result.append(
                        emptySquares
                );
            }

            if (rank > 0) {

                result.append('/');
            }
        }

        return result.toString();
    }

    private char pieceToFenSymbol(
            Piece piece
    ) {

        char symbol =
                switch (piece.type()) {

                    case PAWN -> 'p';
                    case KNIGHT -> 'n';
                    case BISHOP -> 'b';
                    case ROOK -> 'r';
                    case QUEEN -> 'q';
                    case KING -> 'k';
                };

        return piece.color() == PieceColor.WHITE
                ? Character.toUpperCase(symbol)
                : symbol;
    }

    private String createSideToMove(
            PieceColor sideToMove
    ) {

        return sideToMove == PieceColor.WHITE
                ? "w"
                : "b";
    }

    private String createCastlingRights(
            GameState state
    ) {

        StringBuilder rights =
                new StringBuilder();

        if (state.isWhiteKingSideCastlingAllowed()) {
            rights.append('K');
        }

        if (state.isWhiteQueenSideCastlingAllowed()) {
            rights.append('Q');
        }

        if (state.isBlackKingSideCastlingAllowed()) {
            rights.append('k');
        }

        if (state.isBlackQueenSideCastlingAllowed()) {
            rights.append('q');
        }

        return rights.isEmpty()
                ? "-"
                : rights.toString();
    }

    private String createEnPassantTarget(
            GameState state
    ) {

        Square target =
                state.getEnPassantTarget();

        if (target == null) {
            return "-";
        }

        char file =
                (char) ('a' + target.file());

        int rank =
                target.rank() + 1;

        return String.valueOf(file)
                + rank;
    }

    // =========================================================
    // DESERIALIZATION
    // =========================================================

    private Board parseBoard(
            String field
    ) {

        String[] ranks =
                field.split("/");

        if (ranks.length != Square.BOARD_SIZE) {

            throw new IllegalArgumentException(
                    "FEN board must contain 8 ranks."
            );
        }

        Board board =
                new Board();

        for (int fenRank = 0;
             fenRank < Square.BOARD_SIZE;
             fenRank++) {

            int boardRank =
                    Square.BOARD_SIZE - 1 - fenRank;

            int file = 0;

            for (char symbol :
                    ranks[fenRank].toCharArray()) {

                if (Character.isDigit(symbol)) {

                    int emptySquares =
                            symbol - '0';

                    if (emptySquares < 1
                            || emptySquares > 8) {

                        throw new IllegalArgumentException(
                                "Invalid empty-square count in FEN."
                        );
                    }

                    file += emptySquares;

                } else {

                    if (file >= Square.BOARD_SIZE) {

                        throw new IllegalArgumentException(
                                "Too many squares in FEN rank."
                        );
                    }

                    Piece piece =
                            fenSymbolToPiece(symbol);

                    board.setPiece(
                            new Square(
                                    file,
                                    boardRank
                            ),
                            piece
                    );

                    file++;
                }

                if (file > Square.BOARD_SIZE) {

                    throw new IllegalArgumentException(
                            "Too many squares in FEN rank."
                    );
                }
            }

            if (file != Square.BOARD_SIZE) {

                throw new IllegalArgumentException(
                        "Each FEN rank must contain exactly 8 squares."
                );
            }
        }

        return board;
    }

    private Piece fenSymbolToPiece(
            char symbol
    ) {

        PieceColor color =
                Character.isUpperCase(symbol)
                        ? PieceColor.WHITE
                        : PieceColor.BLACK;

        PieceType type =
                switch (Character.toLowerCase(symbol)) {

                    case 'p' -> PieceType.PAWN;
                    case 'n' -> PieceType.KNIGHT;
                    case 'b' -> PieceType.BISHOP;
                    case 'r' -> PieceType.ROOK;
                    case 'q' -> PieceType.QUEEN;
                    case 'k' -> PieceType.KING;

                    default ->
                            throw new IllegalArgumentException(
                                    "Invalid piece symbol in FEN: "
                                            + symbol
                            );
                };

        return new Piece(
                type,
                color
        );
    }

    private PieceColor parseSideToMove(
            String field
    ) {

        return switch (field) {

            case "w" -> PieceColor.WHITE;
            case "b" -> PieceColor.BLACK;

            default ->
                    throw new IllegalArgumentException(
                            "Invalid side to move in FEN."
                    );
        };
    }

    private CastlingRights parseCastlingRights(
            String field
    ) {

        if ("-".equals(field)) {

            return new CastlingRights(
                    false,
                    false,
                    false,
                    false
            );
        }

        boolean whiteKingSide = false;
        boolean whiteQueenSide = false;
        boolean blackKingSide = false;
        boolean blackQueenSide = false;

        Set<Character> seen =
                new HashSet<>();

        for (char symbol :
                field.toCharArray()) {

            if (!seen.add(symbol)) {

                throw new IllegalArgumentException(
                        "Duplicate castling right in FEN."
                );
            }

            switch (symbol) {

                case 'K' ->
                        whiteKingSide = true;

                case 'Q' ->
                        whiteQueenSide = true;

                case 'k' ->
                        blackKingSide = true;

                case 'q' ->
                        blackQueenSide = true;

                default ->
                        throw new IllegalArgumentException(
                                "Invalid castling rights in FEN."
                        );
            }
        }

        return new CastlingRights(
                whiteKingSide,
                whiteQueenSide,
                blackKingSide,
                blackQueenSide
        );
    }

    private Square parseEnPassantTarget(
            String field,
            PieceColor sideToMove
    ) {

        if ("-".equals(field)) {
            return null;
        }

        if (!field.matches("[a-h][36]")) {

            throw new IllegalArgumentException(
                    "Invalid en passant target in FEN."
            );
        }

        Square target =
                Square.fromAlgebraic(
                        field
                );

        /*
         * If White is to move, Black has just moved
         * two squares -> target must be on rank 6.
         *
         * If Black is to move, White has just moved
         * two squares -> target must be on rank 3.
         */
        if (sideToMove == PieceColor.WHITE
                && target.rank() != 5) {

            throw new IllegalArgumentException(
                    "Invalid en passant target for side to move."
            );
        }

        if (sideToMove == PieceColor.BLACK
                && target.rank() != 2) {

            throw new IllegalArgumentException(
                    "Invalid en passant target for side to move."
            );
        }

        return target;
    }

    private int parseHalfMoveClock(
            String field
    ) {

        int value =
                parseInteger(
                        field,
                        "halfmove clock"
                );

        if (value < 0) {

            throw new IllegalArgumentException(
                    "Halfmove clock cannot be negative."
            );
        }

        return value;
    }

    private int parseFullMoveNumber(
            String field
    ) {

        int value =
                parseInteger(
                        field,
                        "fullmove number"
                );

        if (value < 1) {

            throw new IllegalArgumentException(
                    "Fullmove number must be at least 1."
            );
        }

        return value;
    }

    private int parseInteger(
            String value,
            String fieldName
    ) {

        try {

            return Integer.parseInt(
                    value
            );

        } catch (NumberFormatException exception) {

            throw new IllegalArgumentException(
                    "Invalid " + fieldName
                            + " in FEN.",
                    exception
            );
        }
    }

    private record CastlingRights(
            boolean whiteKingSide,
            boolean whiteQueenSide,
            boolean blackKingSide,
            boolean blackQueenSide
    ) {
    }
}