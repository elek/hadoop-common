package org.apache.hadoop.conf;

public class ConfigurationFactory {

  public static Configuration newInstance() {
    return new InMemoryConfiguration();
  }

  public static Configuration newInstance(Configuration conf) {
    return new InMemoryConfiguration();
  }
}
