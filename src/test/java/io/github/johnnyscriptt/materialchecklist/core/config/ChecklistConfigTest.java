package io.github.johnnyscriptt.materialchecklist.core.config;

import io.github.johnnyscriptt.materialchecklist.core.search.SearchRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChecklistConfigTest {

    @Test
    void defaultsAreSensible() {
        ChecklistConfig cfg = new ChecklistConfig();
        assertTrue(cfg.hudVisible);
        assertTrue(cfg.glowMissingSlots);
        assertEquals(ChecklistConfig.DEFAULT_ROWS, cfg.hudRows);
        assertEquals(SearchRules.DEFAULT_RADIUS, cfg.radius);
        assertEquals(ChecklistConfig.Corner.TOP_RIGHT, cfg.hudCorner);
    }

    @Test
    void clampPullsValuesBackIntoRange() {
        ChecklistConfig cfg = new ChecklistConfig();
        cfg.hudRows = 500;
        cfg.radius = 1;
        cfg.clamp();
        assertEquals(ChecklistConfig.MAX_ROWS, cfg.hudRows);
        assertEquals(SearchRules.MIN_RADIUS, cfg.radius);
    }

    @Test
    void zeroAndMissingFieldsFromAnOlderFileMeanDefault() {
        ChecklistConfig cfg = new ChecklistConfig();
        cfg.hudRows = 0;
        cfg.radius = 0;
        cfg.hudCorner = null;
        cfg.clamp();
        assertEquals(ChecklistConfig.DEFAULT_ROWS, cfg.hudRows);
        assertEquals(SearchRules.DEFAULT_RADIUS, cfg.radius);
        assertEquals(ChecklistConfig.Corner.TOP_RIGHT, cfg.hudCorner);
    }

    @Test
    void cornersCycleThroughAllFour() {
        ChecklistConfig.Corner corner = ChecklistConfig.Corner.TOP_LEFT;
        for (int i = 0; i < 4; i++) {
            corner = corner.next();
        }
        assertEquals(ChecklistConfig.Corner.TOP_LEFT, corner);
        assertEquals(ChecklistConfig.Corner.TOP_RIGHT, ChecklistConfig.Corner.TOP_LEFT.next());
    }
}
