package org.apache.hadoop.conf;

public interface Configuration extends ConfigurationSource {
  void setBoolean(String sslRequireClientCertKey, boolean aBoolean);

  String[] getStrings(String sslEnabledProtocolsKey, String sslEnabledProtocolsDefault);

  void setClass(String s, Class<?> engine, Class<?> clazz);

  IntegerRanges getRange(String rangeConf, String s);

  void setInt(String ipcPingIntervalKey, int pingInterval);
}
