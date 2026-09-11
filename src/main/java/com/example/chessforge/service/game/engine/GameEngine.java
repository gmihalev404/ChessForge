package com.example.chessforge.service.game.engine;

import com.example.chessforge.service.game.engine.move.LegalMoveGenerator;
import com.example.chessforge.service.game.engine.move.MoveApplier;
import com.example.chessforge.service.game.engine.rule.AttackDetector;
import org.springframework.stereotype.Component;

@Component
public class GameEngine {

    private final LegalMoveGenerator legalMoveGenerator;
    private final MoveApplier moveApplier;
    private final AttackDetector attackDetector;

    public GameEngine(
            LegalMoveGenerator legalMoveGenerator,
            MoveApplier moveApplier,
            AttackDetector attackDetector
    ) {

        this.legalMoveGenerator =
                legalMoveGenerator;

        this.moveApplier =
                moveApplier;

        this.attackDetector =
                attackDetector;
    }
}