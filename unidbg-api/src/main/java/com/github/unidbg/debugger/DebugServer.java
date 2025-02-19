package com.github.unidbg.debugger;

public interface DebugServer extends Debugger, Runnable {

    int DEFAULT_PORT = 23946;

    int PACKET_SIZE = 1024;

    byte IDA_PROTOCOL_VERSION_V7 = 0x1a; // IDA Pro v7.x
    byte IDA_PROTOCOL_VERSION_V8 = 0x1b; // IDA Pro v8.0
    byte IDA_DEBUGGER_ID = 0xb; // armlinux

    String DEBUG_EXEC_NAME = "unidbg";

}
