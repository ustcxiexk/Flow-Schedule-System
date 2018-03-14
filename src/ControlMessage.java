package org.grpc.app;

import org.onlab.packet.MacAddress;

public class ControlMessage {
    private int[] tcpPorts = {37500, 38500, 39500, 40000, 41500};
    private int[] chunks = {100, 200, 350, 400, 500, 650, 700};
    private int chunkIndex;
    private MacAddress MacSrc;
    private MacAddress MacDst;
    private int tcpPort;
    private int meterSize;
    public ControlMessage(int chunkId, MacAddress src, MacAddress dst, int count){
        chunkIndex = chunkId;
        MacSrc = src;
        MacDst = dst;
        tcpPort = tcpPorts[count];
        meterSize = chunks[chunkId];
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public MacAddress getSrcIp() {
        return MacSrc;
    }

    public MacAddress getDstIp() {
        return MacDst;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    public int getMeterSize() {
        return meterSize;
    }
}
