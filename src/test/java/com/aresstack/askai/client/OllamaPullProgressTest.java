package com.aresstack.askai.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the pull-progress percentage used to drive the Actions panel progress bar.
 */
class OllamaPullProgressTest {

    @Test
    void computesPercentWhenMeasurable() {
        OllamaPullProgress progress = new OllamaPullProgress("pulling", 50L, 200L);
        assertTrue(progress.hasMeasurableProgress());
        assertEquals(25, progress.percent());
    }

    @Test
    void returnsMinusOneWhenTotalUnknown() {
        OllamaPullProgress progress = new OllamaPullProgress("verifying", 0L, 0L);
        assertFalse(progress.hasMeasurableProgress());
        assertEquals(-1, progress.percent());
    }

    @Test
    void clampsToHundred() {
        OllamaPullProgress progress = new OllamaPullProgress("pulling", 300L, 200L);
        assertEquals(100, progress.percent());
    }
}
