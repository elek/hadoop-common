package org.apache.hadoop.common.example;

import java.io.IOException;

import org.apache.hadoop.common.example.HelloWorldProtocolProto.HelloWorldService;
import org.apache.hadoop.commons.example.HelloWorldServicePB;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.ConfigurationFactory;
import org.apache.hadoop.ipc.ProtobufRpcEngine;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.thirdparty.protobuf.BlockingService;

public class ExampleServer {

  public static void main(String[] args)
      throws IOException, InterruptedException {
    new ExampleServer().run();
  }

  private void run() throws IOException, InterruptedException {

    Configuration configuration = ConfigurationFactory.newInstance();

    RPC.setProtocolEngine(configuration, HelloWorldServicePB.class,
        ProtobufRpcEngine.class);

    final HelloWorldServiceServerSideTranslator
        service = new HelloWorldServiceServerSideTranslator();

    final BlockingService instance =
        HelloWorldService.newReflectiveBlockingService(service);

    RPC.Server rpcServer = new RPC.Builder(configuration)
        .setProtocol(HelloWorldServicePB.class)
        .setInstance(service)
        .setBindAddress("0.0.0.0")
        .setPort(1234)
        .setNumHandlers(30)
        .setVerbose(false)
        .build();

    rpcServer
        .addProtocol(RPC.RpcKind.RPC_PROTOCOL_BUFFER, HelloWorldServicePB.class,
            instance);

    rpcServer.start();
    Thread.sleep(100000L);
  }
}
