package com.taskhive.backend.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * O(1) fractional indexing with midpoint insertion and automatic rebalancing
 * whenever adjacent gaps drop below MIN_GAP or floating-point precision saturates.
 */
@Service
public class FractionalIndexService {

    public static final double DEFAULT_GAP = 1024.0;
    public static final double MIN_GAP = 1e-4;

    public record PositionResult<T>(double position, boolean rebalanced, List<T> orderedItems) {
    }

    public <T> PositionResult<T> computePosition(
            List<T> currentOrderedItems,
            T movingItem,
            UUID beforeId,
            UUID afterId,
            Double explicitPosition,
            Function<T, UUID> idExtractor,
            Function<T, Double> positionExtractor,
            BiConsumer<T, Double> positionSetter
    ) {
        UUID movingId = idExtractor.apply(movingItem);
        List<T> others = new ArrayList<>();
        for (T item : currentOrderedItems) {
            if (!Objects.equals(idExtractor.apply(item), movingId)) {
                others.add(item);
            }
        }

        if (others.isEmpty()) {
            positionSetter.accept(movingItem, DEFAULT_GAP);
            return new PositionResult<>(DEFAULT_GAP, false, List.of(movingItem));
        }

        int insertIndex = resolveInsertIndex(others, beforeId, afterId, explicitPosition, idExtractor, positionExtractor);
        Double prevPos = insertIndex > 0 ? positionExtractor.apply(others.get(insertIndex - 1)) : null;
        Double nextPos = insertIndex < others.size() ? positionExtractor.apply(others.get(insertIndex)) : null;

        double candidate;
        boolean needsRebalance = false;

        if (prevPos != null && nextPos != null) {
            double gap = nextPos - prevPos;
            candidate = prevPos + (gap / 2.0);
            if (gap < MIN_GAP || candidate <= prevPos || candidate >= nextPos) {
                needsRebalance = true;
            }
        } else if (prevPos != null) {
            candidate = prevPos + DEFAULT_GAP;
        } else {
            // Inserting before the first item
            candidate = nextPos / 2.0;
            if (nextPos < MIN_GAP || candidate <= 0.0 || candidate >= nextPos) {
                needsRebalance = true;
            }
        }

        List<T> finalOrder = new ArrayList<>(others.size() + 1);
        finalOrder.addAll(others.subList(0, insertIndex));
        finalOrder.add(movingItem);
        finalOrder.addAll(others.subList(insertIndex, others.size()));

        if (needsRebalance) {
            double pos = DEFAULT_GAP;
            for (T item : finalOrder) {
                positionSetter.accept(item, pos);
                pos += DEFAULT_GAP;
            }
            return new PositionResult<>(positionExtractor.apply(movingItem), true, finalOrder);
        }

        positionSetter.accept(movingItem, candidate);
        return new PositionResult<>(candidate, false, finalOrder);
    }

    private <T> int resolveInsertIndex(
            List<T> others,
            UUID beforeId,
            UUID afterId,
            Double explicitPosition,
            Function<T, UUID> idExtractor,
            Function<T, Double> positionExtractor
    ) {
        if (beforeId != null) {
            for (int i = 0; i < others.size(); i++) {
                if (Objects.equals(idExtractor.apply(others.get(i)), beforeId)) {
                    return i + 1;
                }
            }
        }
        if (afterId != null) {
            for (int i = 0; i < others.size(); i++) {
                if (Objects.equals(idExtractor.apply(others.get(i)), afterId)) {
                    return i;
                }
            }
        }
        if (explicitPosition != null && Double.isFinite(explicitPosition)) {
            for (int i = 0; i < others.size(); i++) {
                if (explicitPosition < positionExtractor.apply(others.get(i))) {
                    return i;
                }
            }
            return others.size();
        }
        return others.size();
    }
}
