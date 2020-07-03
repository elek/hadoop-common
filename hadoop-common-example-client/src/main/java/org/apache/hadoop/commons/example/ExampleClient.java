package org.apache.hadoop.commons.example;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.apache.hadoop.common.example.HelloWorldProtocolProto;
import org.apache.hadoop.common.example.HelloWorldProtocolProto.HelloRequest;
import org.apache.hadoop.common.example.HelloWorldProtocolProto.HelloResponse;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.ConfigurationFactory;
import org.apache.hadoop.io.retry.RetryPolicies;
import org.apache.hadoop.io.retry.RetryPolicy;
import org.apache.hadoop.ipc.Client;
import org.apache.hadoop.ipc.ProtobufRpcEngine;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.net.StandardSocketFactory;
import org.apache.hadoop.security.UserGroupInformation;

public class ExampleClient {
  public static void main(String[] args) throws Exception {
    new ExampleClient().run();
  }

  private void run() throws Exception {

    Configuration conf = ConfigurationFactory.newInstance();

    RPC.setProtocolEngine(conf, HelloWorldServicePB.class,
        ProtobufRpcEngine.class);

    long scmVersion =
        RPC.getProtocolVersion(HelloWorldServicePB.class);

    RetryPolicy retryPolicy =
        RetryPolicies.retryForeverWithFixedSleep(
            1000, TimeUnit.MILLISECONDS);

    final HelloWorldServicePB proxy = RPC.getProtocolProxy(
        HelloWorldServicePB.class,
        scmVersion,
        new InetSocketAddress(1234),
        UserGroupInformation.getCurrentUser(),
        conf,
        new StandardSocketFactory(),
        Client.getRpcTimeout(conf),
        retryPolicy).getProxy();

    final HelloResponse test =
        proxy.submitRequest(null, HelloRequest.newBuilder()
            .setName("test")
            .build());

    System.out.println(test);
  }
}
