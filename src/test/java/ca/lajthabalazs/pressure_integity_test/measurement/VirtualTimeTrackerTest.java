package ca.lajthabalazs.pressure_integity_test.measurement;

import ca.lajthabalazs.pressure_integrity_test.measurement.VirtualTimeTracker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for {@link VirtualTimeTracker}. */
public class VirtualTimeTrackerTest {

  private VirtualTimeTracker tracker;

  @BeforeEach
  public void setUp() {
    tracker = new VirtualTimeTracker();
  }

  @Test
  public void start_initializesTracking() {
    long startTime = 1000L;
    tracker.start(startTime);

    Assertions.assertEquals(0L, tracker.getCurrentVirtualTime(startTime));
    Assertions.assertFalse(tracker.isPaused());
    Assertions.assertEquals(1.0, tracker.getSpeedFactor());
  }

  @Test
  public void getCurrentVirtualTime_whenPlaying_advancesWithSpeed() {
    long startTime = 1000L;
    tracker.start(startTime);
    tracker.setSpeed(startTime, 2.0); // 2x speed

    // After 100ms real time at 2x speed, virtual time should advance 200ms
    long now = startTime + 100;
    Assertions.assertEquals(200L, tracker.getCurrentVirtualTime(now));
  }

  @Test
  public void getCurrentVirtualTime_whenPaused_remainsFrozen() {
    long startTime = 1000L;
    tracker.start(startTime);
    tracker.setSpeed(startTime, 2.0);

    // Play for 100ms real time (200ms virtual)
    long pauseTime = startTime + 100;
    tracker.pause(pauseTime);
    Assertions.assertEquals(200L, tracker.getCurrentVirtualTime(pauseTime));

    // After pause, virtual time should remain frozen
    long later = pauseTime + 500;
    Assertions.assertEquals(200L, tracker.getCurrentVirtualTime(later));
    Assertions.assertTrue(tracker.isPaused());
  }

  @Test
  public void resume_continuesFromFrozenPosition() {
    long startTime = 1000L;
    tracker.start(startTime);
    tracker.setSpeed(startTime, 2.0);

    // Play for 100ms real time (200ms virtual)
    long pauseTime = startTime + 100;
    tracker.pause(pauseTime);

    // Resume after 200ms pause (virtual time stays at 200ms)
    long resumeTime = pauseTime + 200;
    tracker.resume(resumeTime);
    Assertions.assertFalse(tracker.isPaused());

    // After resuming, play for another 50ms real time at 2x (100ms virtual)
    long later = resumeTime + 50;
    Assertions.assertEquals(300L, tracker.getCurrentVirtualTime(later)); // 200 + 100
  }

  @Test
  public void setSpeed_capturesElapsedTimeBeforeChange() {
    long startTime = 1000L;
    tracker.start(startTime);
    tracker.setSpeed(startTime, 2.0); // Start at 2x

    // Play for 100ms at 2x speed (200ms virtual)
    long changeTime = startTime + 100;
    tracker.setSpeed(changeTime, 4.0); // Change to 4x

    // Virtual time should be 200ms (captured before speed change)
    Assertions.assertEquals(200L, tracker.getCurrentVirtualTime(changeTime));

    // After speed change, continue playing at 4x
    long later = changeTime + 50; // 50ms real time at 4x = 200ms virtual
    Assertions.assertEquals(400L, tracker.getCurrentVirtualTime(later)); // 200 + 200
  }

  @Test
  public void setSpeed_whenPaused_onlyChangesSpeed() {
    long startTime = 1000L;
    tracker.start(startTime);
    tracker.setSpeed(startTime, 2.0);

    // Play for 100ms at 2x (200ms virtual)
    long pauseTime = startTime + 100;
    tracker.pause(pauseTime);

    // Change speed while paused
    long changeTime = pauseTime + 50;
    tracker.setSpeed(changeTime, 4.0);

    // Virtual time should still be frozen at 200ms
    Assertions.assertEquals(200L, tracker.getCurrentVirtualTime(changeTime));
    Assertions.assertEquals(4.0, tracker.getSpeedFactor());
    Assertions.assertTrue(tracker.isPaused());
  }

  @Test
  public void calculateDelay_computesCorrectDelay() {
    long startTime = 1000L;
    tracker.start(startTime);
    tracker.setSpeed(startTime, 2.0);

    // After 100ms real time at 2x, virtual time is 200ms
    long now = startTime + 100;

    // Vector at 500ms virtual time: need 300ms more virtual time
    // At 2x speed, that's 150ms real time delay
    long delay = tracker.calculateDelay(500L, now);
    Assertions.assertEquals(150L, delay);
  }

  @Test
  public void calculateDelay_whenVectorAlreadyPassed_returnsZero() {
    long startTime = 1000L;
    tracker.start(startTime);
    tracker.setSpeed(startTime, 2.0);

    // After 100ms real time at 2x, virtual time is 200ms
    long now = startTime + 100;

    // Vector at 100ms virtual time (already passed)
    long delay = tracker.calculateDelay(100L, now);
    Assertions.assertEquals(0L, delay);
  }

  @Test
  public void calculateDelay_accountsForCurrentPlaySession() {
    long startTime = 1000L;
    tracker.start(startTime);
    tracker.setSpeed(startTime, 2.0);

    // Play for 100ms at 2x (200ms virtual)
    long pauseTime = startTime + 100;
    tracker.pause(pauseTime);

    // Resume after pause
    long resumeTime = pauseTime + 200;
    tracker.resume(resumeTime);

    // After resuming, play for 25ms at 2x (50ms virtual, total 250ms)
    long now = resumeTime + 25;

    // Vector at 500ms virtual time: need 250ms more virtual time
    // At 2x speed, that's 125ms real time delay
    long delay = tracker.calculateDelay(500L, now);
    Assertions.assertEquals(125L, delay);
  }

  @Test
  public void pause_whenNotStarted_doesNothing() {
    long time = 1000L;
    tracker.pause(time);
    Assertions.assertFalse(tracker.isPaused());
    Assertions.assertEquals(0L, tracker.getCurrentVirtualTime(time));
  }

  @Test
  public void pause_whenAlreadyPaused_doesNothing() {
    long startTime = 1000L;
    tracker.start(startTime);
    tracker.pause(startTime + 100);
    Assertions.assertTrue(tracker.isPaused());
    // After 100ms at 1.0x speed, virtual time should be 100ms
    long virtualTimeAtFirstPause = tracker.getCurrentVirtualTime(startTime + 100);
    Assertions.assertEquals(100L, virtualTimeAtFirstPause);

    long pauseAgain = startTime + 200;
    tracker.pause(pauseAgain);
    Assertions.assertTrue(tracker.isPaused());
    // Virtual time should still be frozen at the first pause point (100ms)
    Assertions.assertEquals(100L, tracker.getCurrentVirtualTime(pauseAgain));
  }

  @Test
  public void resume_whenNotPaused_doesNothing() {
    long startTime = 1000L;
    tracker.start(startTime);
    tracker.resume(startTime + 100); // Not paused, should do nothing
    Assertions.assertFalse(tracker.isPaused());
  }

  @Test
  public void setSpeed_invalidFactor_throwsException() {
    long startTime = 1000L;
    tracker.start(startTime);

    Assertions.assertThrows(IllegalArgumentException.class, () -> tracker.setSpeed(startTime, 0.0));
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> tracker.setSpeed(startTime, -1.0));
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> tracker.setSpeed(startTime, Double.NaN));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> tracker.setSpeed(startTime, Double.POSITIVE_INFINITY));
  }

  @Test
  public void reset_clearsAllState() {
    long startTime = 1000L;
    tracker.start(startTime);
    tracker.setSpeed(startTime, 2.0);
    tracker.pause(startTime + 100);

    tracker.reset();

    Assertions.assertEquals(0L, tracker.getCurrentVirtualTime(startTime + 200));
    Assertions.assertFalse(tracker.isPaused());
    Assertions.assertEquals(1.0, tracker.getSpeedFactor());
  }

  @Test
  public void multipleSpeedChanges_capturesCorrectly() {
    long startTime = 1000L;
    tracker.start(startTime);
    tracker.setSpeed(startTime, 1.0); // 1x

    // Play 100ms at 1x (100ms virtual)
    long t1 = startTime + 100;
    tracker.setSpeed(t1, 2.0); // Change to 2x
    Assertions.assertEquals(100L, tracker.getCurrentVirtualTime(t1));

    // Play 50ms at 2x (100ms virtual, total 200ms)
    long t2 = t1 + 50;
    tracker.setSpeed(t2, 0.5); // Change to 0.5x
    Assertions.assertEquals(200L, tracker.getCurrentVirtualTime(t2));

    // Play 200ms at 0.5x (100ms virtual, total 300ms)
    long t3 = t2 + 200;
    Assertions.assertEquals(300L, tracker.getCurrentVirtualTime(t3));
  }

  @Test
  public void pauseResume_multipleTimes_maintainsCorrectVirtualTime() {
    long startTime = 1000L;
    tracker.start(startTime);
    tracker.setSpeed(startTime, 2.0);

    // First play session: 100ms at 2x = 200ms virtual
    long pause1 = startTime + 100;
    tracker.pause(pause1);
    Assertions.assertEquals(200L, tracker.getCurrentVirtualTime(pause1));

    // Resume and play 50ms at 2x = 100ms virtual (total 300ms)
    long resume1 = pause1 + 50;
    tracker.resume(resume1);
    long pause2 = resume1 + 50;
    tracker.pause(pause2);
    Assertions.assertEquals(300L, tracker.getCurrentVirtualTime(pause2));

    // Resume and play 25ms at 2x = 50ms virtual (total 350ms)
    long resume2 = pause2 + 25;
    tracker.resume(resume2);
    long later = resume2 + 25;
    Assertions.assertEquals(350L, tracker.getCurrentVirtualTime(later));
  }

  @Test
  public void getCurrentVirtualTime_beforeStart_returnsZero() {
    long time = 1000L;
    Assertions.assertEquals(0L, tracker.getCurrentVirtualTime(time));
  }

  @Test
  public void calculateDelay_beforeStart_usesZeroVirtualTime() {
    long startTime = 1000L;
    tracker.setSpeed(startTime, 2.0);
    // Don't call start() - tracker not initialized

    // Vector at 200ms virtual time, at 2x speed = 100ms delay
    long delay = tracker.calculateDelay(200L, startTime);
    Assertions.assertEquals(100L, delay);
  }

  @Test
  public void fractionalSpeedFactors_handledCorrectly() {
    long startTime = 1000L;
    tracker.start(startTime);
    tracker.setSpeed(startTime, 1.5); // 1.5x speed

    // After 100ms real time at 1.5x, virtual time should advance 150ms
    long now = startTime + 100;
    Assertions.assertEquals(150L, tracker.getCurrentVirtualTime(now));
  }

  @Test
  public void verySlowSpeed_handledCorrectly() {
    long startTime = 1000L;
    tracker.start(startTime);
    tracker.setSpeed(startTime, 0.1); // 0.1x speed (10x slower)

    // After 1000ms real time at 0.1x, virtual time should advance 100ms
    long now = startTime + 1000;
    Assertions.assertEquals(100L, tracker.getCurrentVirtualTime(now));
  }
}
