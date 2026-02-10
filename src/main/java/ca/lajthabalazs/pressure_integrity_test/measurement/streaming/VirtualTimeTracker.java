package ca.lajthabalazs.pressure_integrity_test.measurement.streaming;

/**
 * Tracks virtual time progression for playback scenarios. Virtual time represents elapsed time in
 * the original timeline, accounting for pauses and speed changes.
 *
 * <p>This class does not use system resources directly. All time values are provided by the caller,
 * making it testable and decoupled from system time.
 *
 * <p>Virtual time advances based on real time elapsed multiplied by the speed factor. When paused,
 * virtual time is frozen. When speed changes, the current virtual time position is captured before
 * applying the new speed.
 */
public class VirtualTimeTracker {
  /** Total virtual time elapsed (in ms of original timeline), accounting for pauses. */
  private volatile long virtualTimeElapsed = 0;

  /** Real time when current play session started (null if paused or not started). */
  private volatile Long realTimeStart = null;

  /** Real time when current pause started (null if not paused). */
  private volatile Long realTimePauseStart = null;

  /** Current speed factor. Values > 1 speed up, values < 1 slow down. */
  private volatile double speedFactor = 1.0;

  /**
   * Starts tracking virtual time. Resets all state and begins a new tracking session.
   *
   * @param realTimeNow the current real time (ms since epoch)
   */
  public void start(long realTimeNow) {
    this.virtualTimeElapsed = 0;
    this.realTimeStart = realTimeNow;
    this.realTimePauseStart = null;
  }

  /**
   * Pauses virtual time tracking. Captures elapsed virtual time up to this point and freezes it.
   *
   * @param realTimeNow the current real time (ms since epoch)
   */
  public void pause(long realTimeNow) {
    if (realTimeStart == null || realTimePauseStart != null) {
      return; // Already paused or not started
    }
    updateVirtualTimeElapsed(realTimeNow);
    realTimePauseStart = realTimeNow;
    realTimeStart = null; // No active play session
  }

  /**
   * Resumes virtual time tracking. Virtual time remains frozen at its current value, and a new play
   * session begins.
   *
   * @param realTimeNow the current real time (ms since epoch)
   */
  public void resume(long realTimeNow) {
    if (realTimePauseStart == null) {
      return; // Not paused
    }
    // Virtual time is already frozen at the correct value (no update needed)
    // Start new play session
    realTimeStart = realTimeNow;
    realTimePauseStart = null;
  }

  /**
   * Sets the speed factor. If currently playing, captures elapsed virtual time before changing
   * speed.
   *
   * @param realTimeNow the current real time (ms since epoch)
   * @param factor speed factor (e.g. 2.0 = twice as fast, 0.5 = half speed). Must be positive.
   * @throws IllegalArgumentException if factor is not positive and finite
   */
  public void setSpeed(long realTimeNow, double factor) {
    if (factor <= 0 || !Double.isFinite(factor)) {
      throw new IllegalArgumentException("Speed factor must be positive and finite");
    }
    // Update virtual time before changing speed
    updateVirtualTimeElapsed(realTimeNow);
    this.speedFactor = factor;
    // If playing, reset play session start time for new speed
    if (realTimeStart != null) {
      realTimeStart = realTimeNow;
    }
  }

  /**
   * Gets the current virtual time, accounting for any elapsed play time since the last update.
   *
   * @param realTimeNow the current real time (ms since epoch)
   * @return the current virtual time (ms in original timeline)
   */
  public long getCurrentVirtualTime(long realTimeNow) {
    long current = virtualTimeElapsed;
    if (realTimeStart != null && realTimePauseStart == null) {
      // Currently playing: add elapsed real time * speed
      long elapsedRealTime = realTimeNow - realTimeStart;
      current += (long) (elapsedRealTime * speedFactor);
    }
    return current;
  }

  /**
   * Calculates the delay (in real time) until a vector at the given virtual time should be
   * published.
   *
   * @param vectorVirtualTime the virtual time when the vector should appear (ms in original
   *     timeline)
   * @param realTimeNow the current real time (ms since epoch)
   * @return the delay in milliseconds (real time) until the vector should be published
   */
  public long calculateDelay(long vectorVirtualTime, long realTimeNow) {
    long currentVirtualTime = getCurrentVirtualTime(realTimeNow);
    long virtualTimeUntilVector = vectorVirtualTime - currentVirtualTime;
    return Math.max(0, (long) (virtualTimeUntilVector / speedFactor));
  }

  /**
   * Returns true if tracking is currently paused.
   *
   * @return true if paused, false otherwise
   */
  public boolean isPaused() {
    return realTimePauseStart != null;
  }

  /** Resets all tracking state. Virtual time, speed, and session state are cleared. */
  public void reset() {
    virtualTimeElapsed = 0;
    realTimeStart = null;
    realTimePauseStart = null;
    speedFactor = 1.0;
  }

  /**
   * Gets the current speed factor.
   *
   * @return the current speed factor
   */
  public double getSpeedFactor() {
    return speedFactor;
  }

  /**
   * Updates virtualTimeElapsed based on current play session. If playing, adds elapsed real time
   * (scaled by speed) to virtual time. If paused, virtual time remains frozen.
   *
   * @param realTimeNow the current real time (ms since epoch)
   */
  private void updateVirtualTimeElapsed(long realTimeNow) {
    if (realTimeStart != null && realTimePauseStart == null) {
      // Currently playing: add elapsed real time * speed to virtual time
      long elapsedRealTime = realTimeNow - realTimeStart;
      virtualTimeElapsed += (long) (elapsedRealTime * speedFactor);
      realTimeStart = realTimeNow; // Reset for next calculation
    }
    // If paused, virtual time is already frozen, no update needed
  }
}
