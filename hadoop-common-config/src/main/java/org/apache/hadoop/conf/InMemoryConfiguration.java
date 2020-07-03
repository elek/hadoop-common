package org.apache.hadoop.conf;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class InMemoryConfiguration implements Configuration {

  private Map<String, String> configurations = new HashMap();

  @Override
  public String[] getStrings(
      String sslEnabledProtocolsKey, String sslEnabledProtocolsDefault
  ) {
    return new String[0];
  }


  @Override
  public IntegerRanges getRange(String rangeConf, String s) {
    return null;
  }


  @Override
  public void set(String key, String value) {
    configurations.put(key, value);
  }


  @Override
  public String get(String key) {
    return configurations.get(key);
  }

  @Override
  public Collection<String> getConfigKeys() {
    return configurations.keySet();
  }

  @Override
  public char[] getPassword(String key) throws IOException {
    return new char[0];
  }

  @Override
  public Map<String, String> getValByRegex(String usersGroupsRegEx) {
    return null;
  }

  @Override
  public String[] getStringCollection(String credentialProviderPath) {
    return new String[0];
  }

  @Override
  public Class<?> getClassByName(String protocolName) {
    return null;
  }

  @Override
  public <T> List<T> getInstances(String s, Class<T> costProviderClass) {
    return null;
  }

  @Override
  public long[] getTimeDurations(
      String s, TimeUnit milliseconds
  ) {
    return new long[0];
  }
}
