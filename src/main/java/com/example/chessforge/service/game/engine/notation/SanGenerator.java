package com.example.chessforge.service.game.engine.notation;

import com.example.chessforge.service.game.engine.model.Board;
import com.example.chessforge.service.game.engine.model.GameState;
import com.example.chessforge.service.game.engine.model.Move;
import com.example.chessforge.service.game.engine.model.MoveType;
import com.example.chessforge.service.game.engine.model.Piece;
import com.example.chessforge.service.game.engine.model.PieceType;
import com.example.chessforge.service.game.engine.model.Square;
import com.example.chessforge.service.game.engine.move.LegalMoveGenerator;
import com.example.chessforge.service.game.engine.rule.AttackDetector;
import com.example.chessforge.service.game.engine.rule.PositionEvaluator;

import java.util.List;
import java.util.Objects;

public class SanGenerator {

    private final LegalMoveGenerator legalMoveGenerator;
    private final AttackDetector attackDetector;
    private final PositionEvaluator positionEvaluator;

    public SanGenerator(
            LegalMoveGenerator legalMoveGenerator,
            AttackDetector attackDetector,
            PositionEvaluator positionEvaluator
    ) {

        this.legalMoveGenerator =
                Objects.requireNonNull(
                        legalMoveGenerator,
                        "Legal move generator cannot be null."
                );

        this.attackDetector =
                Objects.requireNonNull(
                        attackDetector,
                        "Attack detector cannot be null."
                );

        this.positionEvaluator =
                Objects.requireNonNull(
                        positionEvaluator,
                        "Position evaluator cannot be null."
                );
    }

    // =========================================================
    // PUBLIC API
    // =========================================================

    public String generate(
            GameState beforeState,
            Move move,
            GameState afterState
    ) {

        Objects.requireNonNull(
                beforeState,
                "Before state cannot be null."
        );

        Objects.requireNonNull(
                move,
                "Move cannot be null."
        );

        Objects.requireNonNull(
                afterState,
                "After state cannot be null."
        );

        String san;

        if (move.type()
                == MoveType.CASTLE_KING_SIDE) {

            san = "O-O";

        } else if (move.type()
                == MoveType.CASTLE_QUEEN_SIDE) {

            san = "O-O-O";

        } else {

            san = generateNormalMoveSan(
                    beforeState,
                    move
            );
        }

        return san
                + generateCheckSuffix(
                afterState
        );
    }

    // =========================================================
    // NORMAL MOVE
    // =========================================================

    private String generateNormalMoveSan(
            GameState beforeState,
            Move move
    ) {

        Board board =
                beforeState.getBoard();

        Piece movingPiece =
                board.getPiece(
                        move.from()
                );

        if (movingPiece == null) {

            throw new IllegalArgumentException(
                    "There is no piece on the source square."
            );
        }

        boolean capture =
                isCapture(
                        beforeState,
                        move
                );

        StringBuilder san =
                new StringBuilder();

        if (movingPiece.type()
                == PieceType.PAWN) {

            appendPawnPrefix(
                    san,
                    move,
                    capture
            );

        } else {

            san.append(
                    pieceLetter(
                            movingPiece.type()
                    )
            );

            san.append(
                    generateDisambiguation(
                            beforeState,
                            move,
                            movingPiece
                    )
            );
        }

        if (capture) {

            san.append("x");
        }

        san.append(
                toAlgebraic(
                        move.to()
                )
        );

        if (move.type()
                == MoveType.PROMOTION) {

            san.append("=");

            san.append(
                    pieceLetter(
                            move.promotion()
                    )
            );
        }

        return san.toString();
    }

    // =========================================================
    // CAPTURE
    // =========================================================

    private boolean isCapture(
            GameState state,
            Move move
    ) {

        if (move.type()
                == MoveType.EN_PASSANT) {

            return true;
        }

        return state.getBoard()
                .getPiece(
                        move.to()
                ) != null;
    }

    private void appendPawnPrefix(
            StringBuilder san,
            Move move,
            boolean capture
    ) {

        if (!capture) {
            return;
        }

        san.append(
                fileCharacter(
                        move.from()
                )
        );
    }

    // =========================================================
    // DISAMBIGUATION
    // =========================================================

    private String generateDisambiguation(
            GameState state,
            Move move,
            Piece movingPiece
    ) {

        List<Move> competingMoves =
                legalMoveGenerator
                        .generateAllLegalMoves(
                                state
                        )
                        .stream()
                        .filter(candidate ->
                                !candidate.from()
                                        .equals(
                                                move.from()
                                        )
                        )
                        .filter(candidate ->
                                candidate.to()
                                        .equals(
                                                move.to()
                                        )
                        )
                        .filter(candidate ->
                                isSamePieceType(
                                        state,
                                        candidate,
                                        movingPiece
                                )
                        )
                        .toList();

        if (competingMoves.isEmpty()) {

            return "";
        }

        boolean sameFile =
                competingMoves
                        .stream()
                        .anyMatch(candidate ->
                                candidate.from()
                                        .file()
                                        == move.from()
                                        .file()
                        );

        boolean sameRank =
                competingMoves
                        .stream()
                        .anyMatch(candidate ->
                                candidate.from()
                                        .rank()
                                        == move.from()
                                        .rank()
                        );

        if (!sameFile) {

            return String.valueOf(
                    fileCharacter(
                            move.from()
                    )
            );
        }

        if (!sameRank) {

            return String.valueOf(
                    rankCharacter(
                            move.from()
                    )
            );
        }

        return toAlgebraic(
                move.from()
        );
    }

    private boolean isSamePieceType(
            GameState state,
            Move candidate,
            Piece movingPiece
    ) {

        Piece candidatePiece =
                state.getBoard()
                        .getPiece(
                                candidate.from()
                        );

        return candidatePiece != null
                && candidatePiece.type()
                == movingPiece.type();
    }

    // =========================================================
    // CHECK / CHECKMATE
    // =========================================================

    private String generateCheckSuffix(
            GameState afterState
    ) {

        if (positionEvaluator.isCheckmate(
                afterState
        )) {

            return "#";
        }

        if (attackDetector.isInCheck(
                afterState,
                afterState.getSideToMove()
        )) {

            return "+";
        }

        return "";
    }

    // =========================================================
    // NOTATION HELPERS
    // =========================================================

    private String pieceLetter(
            PieceType type
    ) {

        return switch (type) {

            case KING -> "K";
            case QUEEN -> "Q";
            case ROOK -> "R";
            case BISHOP -> "B";
            case KNIGHT -> "N";

            case PAWN ->
                    throw new IllegalArgumentException(
                            "Pawn does not have a SAN piece letter."
                    );
        };
    }

    private String toAlgebraic(
            Square square
    ) {

        return ""
                + fileCharacter(square)
                + rankCharacter(square);
    }

    private char fileCharacter(
            Square square
    ) {

        return (char) (
                'a'
                        + square.file()
        );
    }

    private char rankCharacter(
            Square square
    ) {

        return (char) (
                '1'
                        + square.rank()
        );
    }
}