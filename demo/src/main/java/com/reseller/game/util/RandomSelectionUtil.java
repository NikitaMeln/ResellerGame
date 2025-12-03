package com.reseller.game.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class for consistent random selection operations across the application.
 * Uses ThreadLocalRandom for better performance in concurrent environments.
 */
public class RandomSelectionUtil {

    private RandomSelectionUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Selects a random element from the given list.
     *
     * @param <T> the type of elements in the list
     * @param items the list to select from
     * @return Optional containing the selected element, or empty if the list is null or empty
     */
    public static <T> Optional<T> selectRandom(List<T> items) {
        if (items == null || items.isEmpty()) {
            return Optional.empty();
        }
        int randomIndex = ThreadLocalRandom.current().nextInt(items.size());
        return Optional.of(items.get(randomIndex));
    }

    /**
     * Selects multiple random elements from the given list without duplicates.
     *
     * @param <T> the type of elements in the list
     * @param items the list to select from
     * @param count the number of elements to select
     * @return list of randomly selected elements (may be smaller than count if not enough items available)
     */
    public static <T> List<T> selectMultipleRandom(List<T> items, int count) {
        if (items == null || items.isEmpty() || count <= 0) {
            return new ArrayList<>();
        }

        List<T> result = new ArrayList<>();
        List<T> itemsCopy = new ArrayList<>(items);
        int actualCount = Math.min(count, itemsCopy.size());

        for (int i = 0; i < actualCount; i++) {
            int randomIndex = ThreadLocalRandom.current().nextInt(itemsCopy.size());
            result.add(itemsCopy.remove(randomIndex));
        }

        return result;
    }

    /**
     * Generates a random integer between min (inclusive) and max (inclusive).
     *
     * @param min the minimum value (inclusive)
     * @param max the maximum value (inclusive)
     * @return random integer in the specified range
     */
    public static int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
