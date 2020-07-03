package org.apache.hadoop.conf;

public interface Configuration
    extends ConfigurationSource, ConfigurationTarget {

  String[] getStrings(
      String sslEnabledProtocolsKey,
      String sslEnabledProtocolsDefault
  );


  IntegerRanges getRange(String rangeConf, String s);

}
