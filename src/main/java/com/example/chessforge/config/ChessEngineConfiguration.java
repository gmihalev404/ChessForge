package com.example.chessforge.config;

import com.example.chessforge.service.game.engine.history.PositionKeyFactory;
import com.example.chessforge.service.game.engine.move.LegalMoveGenerator;
import com.example.chessforge.service.game.engine.move.MoveApplier;
import com.example.chessforge.service.game.engine.move.MoveGenerator;
import com.example.chessforge.service.game.engine.notation.FenConverter;
import com.example.chessforge.service.game.engine.rule.AttackDetector;
import com.example.chessforge.service.game.engine.rule.DrawEvaluator;
import com.example.chessforge.service.game.engine.rule.PositionEvaluator;
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

    @Bean
    public PositionEvaluator positionEvaluator(
            LegalMoveGenerator legalMoveGenerator,
            AttackDetector attackDetector
    ) {

        return new PositionEvaluator(
                legalMoveGenerator,
                attackDetector
        );
    }

    @Bean
    public PositionKeyFactory positionKeyFactory(
            LegalMoveGenerator legalMoveGenerator
    ) {

        return new PositionKeyFactory(
                legalMoveGenerator
        );
    }

    @Bean
    public DrawEvaluator drawEvaluator(
            PositionEvaluator positionEvaluator
    ) {

        return new DrawEvaluator(
                positionEvaluator
        );
    }

    @Bean
    public FenConverter fenConverter() {
        return new FenConverter();
    }
}