/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;

import org.slf4j.Logger;

/**
 * General reflection utils
 */
@InterfaceAudience.Public
@InterfaceStability.Evolving
public class ReflectionUtils {

  private static final Class<?>[] EMPTY_ARRAY = new Class[] {};

  /**
   * Cache of constructors for each class. Pins the classes so they
   * can't be garbage collected until ReflectionUtils can be collected.
   */
  private static final Map<Class<?>, Constructor<?>> CONSTRUCTOR_CACHE =
      new ConcurrentHashMap<Class<?>, Constructor<?>>();

  static private ThreadMXBean threadBean =
      ManagementFactory.getThreadMXBean();
  private static long previousLogTime = 0;

  public static void setContentionTracing(boolean val) {
    threadBean.setThreadContentionMonitoringEnabled(val);
  }

  private static String getTaskName(long id, String name) {
    if (name == null) {
      return Long.toString(id);
    }
    return id + " (" + name + ")";
  }

  /**
   * Print all of the thread's information and stack traces.
   *
   * @param stream the stream to
   * @param title a string title for the stack trace
   */
  public synchronized static void printThreadInfo(
      PrintStream stream,
      String title
  ) {
    final int STACK_DEPTH = 20;
    boolean contention = threadBean.isThreadContentionMonitoringEnabled();
    long[] threadIds = threadBean.getAllThreadIds();
    stream.println("Process Thread Dump: " + title);
    stream.println(threadIds.length + " active threads");
    for (long tid : threadIds) {
      ThreadInfo info = threadBean.getThreadInfo(tid, STACK_DEPTH);
      if (info == null) {
        stream.println("  Inactive");
        continue;
      }
      stream.println("Thread " +
          getTaskName(info.getThreadId(),
              info.getThreadName()) + ":");
      Thread.State state = info.getThreadState();
      stream.println("  State: " + state);
      stream.println("  Blocked count: " + info.getBlockedCount());
      stream.println("  Waited count: " + info.getWaitedCount());
      if (contention) {
        stream.println("  Blocked time: " + info.getBlockedTime());
        stream.println("  Waited time: " + info.getWaitedTime());
      }
      if (state == Thread.State.WAITING) {
        stream.println("  Waiting on " + info.getLockName());
      } else if (state == Thread.State.BLOCKED) {
        stream.println("  Blocked on " + info.getLockName());
        stream.println("  Blocked by " +
            getTaskName(info.getLockOwnerId(),
                info.getLockOwnerName()));
      }
      stream.println("  Stack:");
      for (StackTraceElement frame : info.getStackTrace()) {
        stream.println("    " + frame.toString());
      }
    }
    stream.flush();
  }


  /**
   * Log the current thread stacks at INFO level.
   * @param log the logger that logs the stack trace
   * @param title a descriptive title for the call stacks
   * @param minInterval the minimum time from the last
   */
  public static void logThreadInfo(
      Logger log,
      String title,
      long minInterval
  ) {
    boolean dumpStack = false;
    if (log.isInfoEnabled()) {
      synchronized (ReflectionUtils.class) {
        long now = Time.monotonicNow();
        if (now - previousLogTime >= minInterval * 1000) {
          previousLogTime = now;
          dumpStack = true;
        }
      }
      if (dumpStack) {
        try {
          ByteArrayOutputStream buffer = new ByteArrayOutputStream();
          printThreadInfo(new PrintStream(buffer, false, "UTF-8"), title);
          log.info(buffer.toString(Charset.defaultCharset().name()));
        } catch (UnsupportedEncodingException ignored) {
        }
      }
    }
  }

  /**
   * Return the correctly-typed {@link Class} of the given object.
   *
   * @param o object whose correctly-typed <code>Class</code> is to be obtained
   * @return the correctly typed <code>Class</code> of the given object.
   */
  @SuppressWarnings("unchecked")
  public static <T> Class<T> getClass(T o) {
    return (Class<T>) o.getClass();
  }

  // methods to support testing
  static void clearCache() {
    CONSTRUCTOR_CACHE.clear();
  }

  static int getCacheSize() {
    return CONSTRUCTOR_CACHE.size();
  }

  /**
   * Gets all the declared fields of a class including fields declared in
   * superclasses.
   */
  public static List<Field> getDeclaredFieldsIncludingInherited(Class<?> clazz) {
    List<Field> fields = new ArrayList<Field>();
    while (clazz != null) {
      Field[] sortedFields = clazz.getDeclaredFields();
      Arrays.sort(sortedFields, new Comparator<Field>() {
        public int compare(Field a, Field b) {
          return a.getName().compareTo(b.getName());
        }
      });
      for (Field field : sortedFields) {
        fields.add(field);
      }
      clazz = clazz.getSuperclass();
    }

    return fields;
  }

  /**
   * Gets all the declared methods of a class including methods declared in
   * superclasses.
   */
  public static List<Method> getDeclaredMethodsIncludingInherited(Class<?> clazz) {
    List<Method> methods = new ArrayList<Method>();
    while (clazz != null) {
      for (Method method : clazz.getDeclaredMethods()) {
        methods.add(method);
      }
      clazz = clazz.getSuperclass();
    }

    return methods;
  }


  /** Create an object for the given class and initialize it from conf
   *
   * @param theClass class of which an object is created
   * @param conf Configuration
   * @return a new object
   */
  @SuppressWarnings("unchecked")
  public static <T> T newInstance(Class<T> theClass, Configuration conf) {
    T result;
    try {
      Constructor<T> meth = (Constructor<T>) CONSTRUCTOR_CACHE.get(theClass);
      if (meth == null) {
        meth = theClass.getDeclaredConstructor(EMPTY_ARRAY);
        meth.setAccessible(true);
        CONSTRUCTOR_CACHE.put(theClass, meth);
      }
      result = meth.newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    setConf(result, conf);
    return result;
  }

  /**
   * Check and set 'configuration' if necessary.
   *
   * @param theObject object for which to set configuration
   * @param conf Configuration
   */
  public static void setConf(Object theObject, Configuration conf) {
    if (conf != null) {
      if (theObject instanceof Configurable) {
        ((Configurable) theObject).setConf(conf);
      }
    }
  }

}
