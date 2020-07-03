package org.apache.hadoop.io.retry;

import java.util.concurrent.TimeUnit;

import com.google.common.annotations.VisibleForTesting;

/**
 * Retry up to maxRetries.
 * The actual sleep time of the n-th retry is f(n, sleepTime),
 * where f is a function provided by the subclass implementation.
 *
 * The object of the subclasses should be immutable;
 * otherwise, the subclass must override hashCode(), equals(..) and toString().
 */
public abstract class RetryLimited implements RetryPolicy {
  final int maxRetries;
  final long sleepTime;
  final TimeUnit timeUnit;

  private String myString;

  RetryLimited(int maxRetries, long sleepTime, TimeUnit timeUnit) {
    if (maxRetries < 0) {
      throw new IllegalArgumentException("maxRetries = " + maxRetries+" < 0");
    }
    if (sleepTime < 0) {
      throw new IllegalArgumentException("sleepTime = " + sleepTime + " < 0");
    }

    this.maxRetries = maxRetries;
    this.sleepTime = sleepTime;
    this.timeUnit = timeUnit;
  }

  @Override
  public RetryAction shouldRetry(Exception e, int retries, int failovers,
      boolean isIdempotentOrAtMostOnce) throws Exception {
    if (retries >= maxRetries) {
      return new RetryAction(RetryAction.RetryDecision.FAIL, 0 , getReason());
    }
    return new RetryAction(RetryAction.RetryDecision.RETRY,
        timeUnit.toMillis(calculateSleepTime(retries)), getReason());
  }

  protected String getReason() {
    return constructReasonString(maxRetries);
  }

  @VisibleForTesting
  public static String constructReasonString(int retries) {
    return "retries get failed due to exceeded maximum allowed retries " +
        "number: " + retries;
  }

  protected abstract long calculateSleepTime(int retries);

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public boolean equals(final Object that) {
    if (this == that) {
      return true;
    } else if (that == null || this.getClass() != that.getClass()) {
      return false;
    }
    return this.toString().equals(that.toString());
  }

  @Override
  public String toString() {
    if (myString == null) {
      myString = getClass().getSimpleName() + "(maxRetries=" + maxRetries
          + ", sleepTime=" + sleepTime + " " + timeUnit + ")";
    }
    return myString;
  }
}
