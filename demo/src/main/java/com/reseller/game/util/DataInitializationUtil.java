package com.reseller.game.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Utility class for standardized data initialization across repository services.
 * Provides consistent logging and error handling for data loading operations.
 */
@Slf4j
public class DataInitializationUtil {

    private DataInitializationUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Initializes data in a repository by saving all entities.
     * Swallows exceptions after logging them.
     *
     * @param <T> the entity type
     * @param repository the JPA repository to save to
     * @param entities the list of entities to save
     * @param entityName the name of the entity type for logging (e.g., "cars", "clients")
     */
    public static <T> void initializeData(JpaRepository<T, ?> repository, List<T> entities, String entityName) {
        initializeData(repository, entities, entityName, false, false);
    }

    /**
     * Initializes data in a repository with optional deletion before saving.
     * Swallows exceptions after logging them.
     *
     * @param <T> the entity type
     * @param repository the JPA repository to save to
     * @param entities the list of entities to save
     * @param entityName the name of the entity type for logging (e.g., "cars", "clients")
     * @param clearBeforeSave if true, deletes all existing data before saving
     */
    public static <T> void initializeData(JpaRepository<T, ?> repository, List<T> entities,
                                          String entityName, boolean clearBeforeSave) {
        initializeData(repository, entities, entityName, clearBeforeSave, false);
    }

    /**
     * Initializes data in a repository with full control over behavior.
     *
     * @param <T> the entity type
     * @param repository the JPA repository to save to
     * @param entities the list of entities to save
     * @param entityName the name of the entity type for logging (e.g., "cars", "clients")
     * @param clearBeforeSave if true, deletes all existing data before saving
     * @param rethrowExceptions if true, rethrows exceptions after logging; if false, swallows them
     */
    public static <T> void initializeData(JpaRepository<T, ?> repository, List<T> entities,
                                          String entityName, boolean clearBeforeSave,
                                          boolean rethrowExceptions) {
        try {
            if (clearBeforeSave) {
                log.debug("Clearing existing {} data", entityName);
                repository.deleteAllInBatch();
            }

            repository.saveAll(entities);
            log.info("Successfully initialized {} {}", entities.size(), entityName);

        } catch (Exception e) {
            log.error("Error initializing {} table", entityName, e);
            if (rethrowExceptions) {
                throw e;
            }
        }
    }
}
