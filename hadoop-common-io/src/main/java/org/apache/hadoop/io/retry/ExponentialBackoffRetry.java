package org.apache.hadoop.io.retry;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class ExponentialBackoffRetry extends RetryLimited {

  public ExponentialBackoffRetry(
      int maxRetries, long sleepTime, TimeUnit timeUnit
  ) {
    super(maxRetries, sleepTime, timeUnit);

    if (maxRetries < 0) {
      throw new IllegalArgumentException("maxRetries = " + maxRetries + " < 0");
    } else if (maxRetries >= Long.SIZE - 1) {
      //calculateSleepTime may overflow.
      throw new IllegalArgumentException("maxRetries = " + maxRetries
          + " >= " + (Long.SIZE - 1));
    }
  }

  private static long calculateExponentialTime(long time, int retries) {
    return calculateExponentialTime(time, retries, Long.MAX_VALUE);
  }

  /**
   * Return a value which is <code>time</code> increasing exponentially as a
   * function of <code>retries</code>, +/- 0%-50% of that value, chosen
   * randomly.
   *
   * @param time    the base amount of time to work with
   * @param retries the number of retries that have so occurred so far
   * @param cap     value at which to cap the base sleep time
   * @return an amount of time to sleep
   */
  private static long calculateExponentialTime(
      long time, int retries,
      long cap
  ) {
    long baseTime = Math.min(time * (1L << retries), cap);
    return (long) (baseTime * (ThreadLocalRandom.current().nextDouble() + 0.5));
  }

  @Override
  protected long calculateSleepTime(int retries) {
    return calculateExponentialTime(sleepTime, retries + 1);
  }

}
