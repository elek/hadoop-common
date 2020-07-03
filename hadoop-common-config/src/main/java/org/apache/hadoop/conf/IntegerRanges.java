package org.apache.hadoop.conf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * A class that represents a set of positive integer ranges. It parses.
 * strings of the form: "2-3,5,7-" where ranges are separated by comma and.
 * the lower/upper bounds are separated by dash. Either the lower or upper.
 * bound may be omitted meaning all values up to or over. So the string.
 * above means 2, 3, 5, and 7, 8, 9, ...
 */
public class IntegerRanges implements Iterable<Integer>{
  private static class Range {
    int start;
    int end;
  }
  private static class RangeNumberIterator implements Iterator<Integer> {
    Iterator<Range> internal;
    int at;
    int end;

    public RangeNumberIterator(List<Range> ranges) {
      if (ranges != null) {
        internal = ranges.iterator();
      }
      at = -1;
      end = -2;
    }

    @Override
    public boolean hasNext() {
      if (at <= end) {
        return true;
      } else if (internal != null){
        return internal.hasNext();
      }
      return false;
    }

    @Override
    public Integer next() {
      if (at <= end) {
        at++;
        return at - 1;
      } else if (internal != null){
        Range found = internal.next();
        if (found != null) {
          at = found.start;
          end = found.end;
          at++;
          return at - 1;
        }
      }
      return null;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  };

  List<Range> ranges = new ArrayList<Range>();

  public IntegerRanges() {
  }

  public IntegerRanges(String newValue) {
    StringTokenizer itr = new StringTokenizer(newValue, ",");
    while (itr.hasMoreTokens()) {
      String rng = itr.nextToken().trim();
      String[] parts = rng.split("-", 3);
      if (parts.length < 1 || parts.length > 2) {
        throw new IllegalArgumentException("integer range badly formed: " +
            rng);
      }
      Range r = new Range();
      r.start = convertToInt(parts[0], 0);
      if (parts.length == 2) {
        r.end = convertToInt(parts[1], Integer.MAX_VALUE);
      } else {
        r.end = r.start;
      }
      if (r.start > r.end) {
        throw new IllegalArgumentException("IntegerRange from " + r.start +
        " to " + r.end + " is invalid");
      }
      ranges.add(r);
    }
  }

  /**
   * Convert a string to an int treating empty strings as the default value.
   * @param value the string value
   * @param defaultValue the value for if the string is empty
   * @return the desired integer
   */
  private static int convertToInt(String value, int defaultValue) {
    String trim = value.trim();
    if (trim.length() == 0) {
      return defaultValue;
    }
    return Integer.parseInt(trim);
  }

  /**
   * Is the given value in the set of ranges
   * @param value the value to check
   * @return is the value in the ranges?
   */
  public boolean isIncluded(int value) {
    for(Range r: ranges) {
      if (r.start <= value && value <= r.end) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return true if there are no values in this range, else false.
   */
  public boolean isEmpty() {
    return ranges == null || ranges.isEmpty();
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    boolean first = true;
    for(Range r: ranges) {
      if (first) {
        first = false;
      } else {
        result.append(',');
      }
      result.append(r.start);
      result.append('-');
      result.append(r.end);
    }
    return result.toString();
  }

  /**
   * Get range start for the first integer range.
   * @return range start.
   */
  public int getRangeStart() {
    if (ranges == null || ranges.isEmpty()) {
      return -1;
    }
    Range r = ranges.get(0);
    return r.start;
  }

  @Override
  public Iterator<Integer> iterator() {
    return new RangeNumberIterator(ranges);
  }

}
