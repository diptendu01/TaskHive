package com.taskhive.backend;

import com.taskhive.backend.security.Card;
import com.taskhive.backend.service.FractionalIndexService;
import com.taskhive.backend.service.FractionalIndexService.PositionResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FractionalIndexServiceTest {

    private final FractionalIndexService service = new FractionalIndexService();

    private Card card(double pos) {
        Card c = new Card();
        c.setId(UUID.randomUUID());
        c.setTitle("Card " + pos);
        c.setPosition(pos);
        return c;
    }

    @Test
    void insertsAtExactMidpointBetweenNeighbors() {
        Card first = card(1024.0);
        Card second = card(2048.0);
        Card moving = card(3072.0);

        PositionResult<Card> result = service.computePosition(
                List.of(first, second, moving),
                moving,
                first.getId(),
                second.getId(),
                null,
                Card::getId,
                Card::getPosition,
                Card::setPosition
        );

        assertThat(result.rebalanced()).isFalse();
        assertThat(result.position()).isEqualTo(1536.0);
        assertThat(result.orderedItems()).containsExactly(first, moving, second);
    }

    @Test
    void insertsBeforeHeadAtHalfPosition() {
        Card first = card(1024.0);
        Card moving = card(2048.0);

        PositionResult<Card> result = service.computePosition(
                List.of(first, moving),
                moving,
                null,
                first.getId(),
                null,
                Card::getId,
                Card::getPosition,
                Card::setPosition
        );

        assertThat(result.rebalanced()).isFalse();
        assertThat(result.position()).isEqualTo(512.0);
        assertThat(result.orderedItems()).containsExactly(moving, first);
    }

    @Test
    void automaticallyRebalancesWhenGapDropsBelowThreshold() {
        Card first = card(1000.0);
        Card second = card(1000.00001);
        Card moving = card(2048.0);

        PositionResult<Card> result = service.computePosition(
                List.of(first, second, moving),
                moving,
                first.getId(),
                second.getId(),
                null,
                Card::getId,
                Card::getPosition,
                Card::setPosition
        );

        assertThat(result.rebalanced()).isTrue();
        assertThat(result.orderedItems()).containsExactly(first, moving, second);
        assertThat(first.getPosition()).isEqualTo(1024.0);
        assertThat(moving.getPosition()).isEqualTo(2048.0);
        assertThat(second.getPosition()).isEqualTo(3072.0);
    }
}
