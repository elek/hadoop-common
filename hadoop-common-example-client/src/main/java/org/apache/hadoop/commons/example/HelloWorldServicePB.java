package org.apache.hadoop.commons.example;

import org.apache.hadoop.common.example.HelloWorldProtocolProto.HelloWorldService.BlockingInterface;
import org.apache.hadoop.ipc.ProtocolInfo;

@ProtocolInfo(protocolName =
    "org.apache.hadoop.ozone.om.protocol.OzoneManagerProtocol",
    protocolVersion = 1)
public interface HelloWorldServicePB
    extends BlockingInterface {

}
