package org.apache.hadoop.common.example;

import org.apache.hadoop.common.example.HelloWorldProtocolProto.HelloRequest;
import org.apache.hadoop.common.example.HelloWorldProtocolProto.HelloResponse;
import org.apache.hadoop.commons.example.HelloWorldServicePB;
import org.apache.hadoop.thirdparty.protobuf.ServiceException;

import com.google.protobuf.RpcController;

public class HelloWorldServiceServerSideTranslator implements
    HelloWorldServicePB {

  @Override
  public HelloResponse submitRequest(
      org.apache.hadoop.thirdparty.protobuf.RpcController controller,
      HelloRequest request
  ) throws ServiceException {
    return HelloResponse.newBuilder()
        .setResponse("Hello " + request.getName()).build();
  }
}
