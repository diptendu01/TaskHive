package com.taskhive.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures pre-existing rows in `lists` and `cards` have a non-null optimistic locking
 * `version` column after Hibernate ddl-auto=update adds the column.
 */
@Component
public class DatabaseVersionBackfill {

    private static final Logger log = LoggerFactory.getLogger(DatabaseVersionBackfill.class);
    private final JdbcTemplate jdbcTemplate;

    public DatabaseVersionBackfill(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void backfillNullVersions() {
        try {
            int updatedLists = jdbcTemplate.update("UPDATE lists SET version = 0 WHERE version IS NULL");
            int updatedCards = jdbcTemplate.update("UPDATE cards SET version = 0 WHERE version IS NULL");
            if (updatedLists > 0 || updatedCards > 0) {
                log.info("Backfilled null @Version columns: {} lists, {} cards", updatedLists, updatedCards);
            }
        } catch (Exception ex) {
            log.debug("Skipped version backfill: {}", ex.getMessage());
        }
    }
}
