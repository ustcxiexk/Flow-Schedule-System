package org.grpc.app;

import org.onlab.packet.MacAddress;

public class Request {
    private int ChunkIndex;
    private MacAddress MacSrc;
    private MacAddress MacDst;
    private int StartTime, EndTime;

    public Request(int chunk, MacAddress src, MacAddress dst, int t1, int t2){
        ChunkIndex = chunk;
        MacSrc = src;
        MacDst = dst;
        StartTime = t1;
        EndTime = t2;
    }

    public int getChunkIndex(){
        return ChunkIndex;
    }

    public MacAddress getMacDst() {
        return MacSrc;
    }

    public MacAddress getMacSrc() {
        return MacDst;
    }

    public int getStartTime() {
        return StartTime;
    }

    public int getEndTime() {
        return EndTime;
    }
}
