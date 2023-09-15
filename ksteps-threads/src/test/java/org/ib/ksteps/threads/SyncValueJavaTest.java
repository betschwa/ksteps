package org.ib.ksteps.threads;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;

public class SyncValueJavaTest {

    private static final Logger LOG = LoggerFactory.getLogger(SyncValueJavaTest.class);

    @Test
    void setValue() {
        final SyncValue<Boolean> value = new SyncValue<>(false,
                                                         "TV");

        Assertions.assertFalse(value.getValue(),
                               "Value not initialised");

        value.setValue(true);

        Assertions.assertTrue(value.getValue(),
                              "Value not changed");
    }

    @Test
    void waitFor() {
        final SyncValue<Boolean> value = new SyncValue<>(false,
                                                         "TV");

        Assertions.assertFalse(value.getValue(),
                               "Value not initialised");

        value.setValue(true);

        Assertions.assertTrue(value.waitFor("Wait for TV...",
                                            (newValue) -> newValue),
                              "Value not changed");
    }

    @Test
    void waitForWithDuration() {
        final SyncValue<Boolean> value = new SyncValue<>(false,
                                                         "TV");

        Assertions.assertFalse(value.getValue(),
                               "Value not initialised");

        new Timer("Set value timer",
                  true).schedule(new TimerTask() {
                                     @Override
                                     public void run() {
                                         value.setValue(true);
                                     }
                                 },
                                 10_000L);

        Assertions.assertTrue(value.waitFor(Duration.ofSeconds(30L),
                                            "Wait for TV...",
                                            (newValue) -> newValue),
                              "Value not changed");
    }
}
