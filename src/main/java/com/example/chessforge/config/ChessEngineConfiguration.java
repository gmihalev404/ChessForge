package com.example.chessforge.config;

import com.example.chessforge.service.game.engine.move.LegalMoveGenerator;
import com.example.chessforge.service.game.engine.move.MoveApplier;
import com.example.chessforge.service.game.engine.move.MoveGenerator;
import com.example.chessforge.service.game.engine.rule.AttackDetector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChessEngineConfiguration {

    @Bean
    public MoveGenerator moveGenerator() {

        return new MoveGenerator();
    }

    @Bean
    public MoveApplier moveApplier() {

        return new MoveApplier();
    }

    @Bean
    public AttackDetector attackDetector() {

        return new AttackDetector();
    }

    @Bean
    public LegalMoveGenerator legalMoveGenerator(
            MoveGenerator moveGenerator,
            MoveApplier moveApplier,
            AttackDetector attackDetector
    ) {

        return new LegalMoveGenerator(
                moveGenerator,
                moveApplier,
                attackDetector
        );
    }
}