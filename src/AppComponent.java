/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grpc.app;

import com.control.demo.*;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.felix.scr.annotations.*;
import org.onlab.graph.*;
import org.onlab.packet.*;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.PortStatisticsService;
import org.onosproject.net.*;
import org.onosproject.net.Path;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.host.HostService;
import org.onosproject.net.meter.*;
import org.onosproject.net.topology.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.crypto.Mac;
import java.io.IOException;
import java.util.*;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
public class AppComponent {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private ApplicationId appId;
    private static final String APP_TEST = "org.rpc.app";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PortStatisticsService portStatisticsService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MeterService meterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PathAdminService pathAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;


    private int port = 36200;
    private Server server;
    private DeviceId deviceIdSrc, deviceIdDst, deviceIdOVS;
    private LinkWeigher weigher = new DefineLinkWeigher();
    private static GraphPathSearch<TopologyVertex, TopologyEdge> BellmanGraphPathSearch = new BellmanFordGraphSearch<>();

    private int flowPriority = 5200;
    private int flowTimeout = 300;
    private final ConnectPoint cp1 = new ConnectPoint(DeviceId.deviceId("of:8f0e486e730203a7"), PortNumber.portNumber(11));
    private final ConnectPoint cp2 = new ConnectPoint(DeviceId.deviceId("of:8f0e486e730203a7"), PortNumber.portNumber(15));
    private final ConnectPoint cp3 = new ConnectPoint(DeviceId.deviceId("of:8f0e486e730203a7"), PortNumber.portNumber(13));
    private Timer timer = new Timer(true);
    private static final long SampleRate = 1;

    private int[][] OptionalPorts = {{30000, 31000, 32000, 33000, 34000, 35000, 36000, 37500, 38500, 39500},
                                    {40000, 41000, 42000, 43000, 44000, 45000, 46000, 47000, 48000, 49000},
                                    {50000, 51000, 52000,53000, 54000, 55000, 56000, 57000, 58000, 59000}};

    private Meter meter;
    private MeterRequest meterRequest;
    private Band band;
    private Collection<Band> bands = new HashSet<>();
    private int ConnectedClientNum = 3;
    private int ConnectedClientCount = 0;
    private List<ControlRequestDatabase> controlRequestDatabases = new ArrayList<>();


    @Activate
    protected void activate() {
        log.info("Started");
        log.info("Started hahaha");
        log.info("Hello World");

        appId = coreService.registerApplication(APP_TEST);

        try {
            start();
        } catch (IOException e) {
            log.warn("exception: {}", e);
        }
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
        try {
            blockUntilShutdown();
        } catch (InterruptedException e) {
            log.warn("exception: {}", e);
        }
    }

    public Meter buildMeter(DeviceId deviceID, long rate) {
//        long burstSize = 10000;

        band = DefaultBand.builder()
                .burstSize(10000)
                .ofType(Band.Type.DROP)
                .withRate(1000000)
                .build();
        bands.add(band);

        meterRequest = DefaultMeterRequest.builder()
                .forDevice(deviceID)
                .burst()
                .fromApp(appId)
                .withUnit(Meter.Unit.KB_PER_SEC)
                .withBands(bands)
                .add();
        meter = meterService.submit(meterRequest);
        log.info("Meter Built!");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("meter has been built!");
        return meter;
    }


    public void distributeRules(ControlResponseDatabase responses) {
        HashMap<String, String> ip2mac = new HashMap<>();
        HashMap<String, Integer> ip2port = new HashMap<>();
        HashMap<Integer, Integer> chunk2rate = new HashMap<>();
        HashMap<String, Host> hosts = new HashMap<>();
        int[] chunks = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        int[] chunkSize = {200, 200, 900, 300, 200, 300, 400, 500, 900, 1000};
        ip2mac.put("10.0.0.1", "f8:0f:41:f4:2a:9b");
        ip2mac.put("10.0.0.2", "7c:d3:0a:e5:73:3b");
        ip2mac.put("10.0.0.3", "f8:0f:41:f4:26:5d");
        ip2port.put("10.0.0.1", 21);
        ip2port.put("10.0.0.2", 25);
        ip2port.put("10.0.0.3", 23);
//        hosts.put("10.0.0.1", hostService.getHost(HostId.hostId("f8:0f:41:f4:2a:9b/None")));
//        hosts.put("10.0.0.2", hostService.getHost(HostId.hostId("7c:d3:0a:e5:73:3b")));
//        hosts.put("10.0.0.3", hostService.getHost(HostId.hostId("f8:0f:41:f4:26:5d")));
        for (int i = 0; i < chunks.length; i++) {
            chunk2rate.put(chunks[i], chunkSize[i]);
        }

        deviceIdOVS = DeviceId.deviceId("of:8f0e486e73020572");

        for (int i = 0; i < responses.getControlResponseCount(); i++) {

            pathAdminService.setDefaultGraphPathSearch(BellmanGraphPathSearch);
            pathAdminService.setDefaultLinkWeigher(weigher);

            Set<Host> srcHostSet = hostService.getHostsByMac(MacAddress.valueOf(ip2mac.get(responses.getControlResponse(i).getSrc())));
            Host srcHost = null;
            if (srcHostSet.iterator().hasNext()) {
                srcHost = srcHostSet.iterator().next();
            }
            Set<Host> dstHostSet = hostService.getHostsByMac(MacAddress.valueOf(ip2mac.get(responses.getControlResponse(i).getDst())));
            Host dstHost = null;
            if (dstHostSet.iterator().hasNext()) {
                dstHost = dstHostSet.iterator().next();
            }

            deviceIdSrc = srcHost.location().deviceId();
            deviceIdDst = dstHost.location().deviceId();

            Set<Path> paths = topologyService.getPaths(topologyService.currentTopology(), deviceIdSrc, deviceIdDst, weigher);
            Path path = null;
            for (Path optPath : paths) {
                path = optPath;
            }
            log.info("get path");
            log.info(paths.toString());

//            MacAddress srcMac = MacAddress.valueOf(ip2mac.get(responses.getControlResponse(i).getSrc()));
//            MacAddress dstMac = MacAddress.valueOf(ip2mac.get(responses.getControlResponse(i).getDst()));
            MacAddress srcMac = srcHost.mac();
            MacAddress dstMac = dstHost.mac();

            int tcpPort = responses.getControlResponse(i).getTcpport();
//            int inPort = ip2port.get(responses.getControlResponse(i).getSrc());
//            int outPort = ip2port.get(responses.getControlResponse(i).getDst());

            PortNumber inPortNumber = srcHost.location().port();

            List<Link> links = path.links();
            Link previousLink = null;
            for (Link link:links){
                if (previousLink != null) {
                    inPortNumber = previousLink.dst().port();
                }
                installRules1(srcMac, dstMac, link.src().deviceId(), tcpPort, inPortNumber, link.src().port());
                installRules2(dstMac, srcMac, link.src().deviceId(), tcpPort, link.src().port(), inPortNumber);
                previousLink = link;
            }
            installRules1(srcMac, dstMac, previousLink.dst().deviceId(), tcpPort, previousLink.dst().port(), dstHost.location().port());
            installRules2(dstMac, srcMac, previousLink.dst().deviceId(), tcpPort, dstHost.location().port(), previousLink.dst().port());

//            long meterRate = (int) chunk2rate.get(responses.getControlResponse(i).getChunkIndex()) * 800;
//            log.info("meterRate ", meterRate);
//            Meter meter1 = buildMeter(deviceIdOVS, 350000);
//            Meter meter2 = buildMeter(deviceIdOVS, 350000);

        }
    }

    public ControlResponseDatabase Algorithm(List<ControlRequestDatabase> userRequests) {

        int tcpPortCount = 0;
        ControlResponseDatabase responses;
        ControlResponseDatabase.Builder responsesBuild = ControlResponseDatabase.newBuilder();
        responsesBuild.setTimeslot(1);
        for (int j = 0; j < userRequests.size(); j++) {
            for (int i = 0; i < userRequests.get(j).getControlRequestCount(); i++) {
                responsesBuild.addControlResponse(ControlResponse.newBuilder().setChunkIndex(userRequests.get(j).getControlRequest(i).getChunkIndex())
                        .setSrc(userRequests.get(j).getControlRequest(i).getSrc()).setDst(userRequests.get(j).getControlRequest(i).getDst())
                        .setTcpport(OptionalPorts[userRequests.get(j).getTimeslot()][tcpPortCount]).setRate(userRequests.get(j).getControlRequest(i).getChunkSize()/10));
                tcpPortCount += 1;
            }
        }
        responses = responsesBuild.build();

        return responses;
    }

    private class DefineLinkWeigher
            extends DefaultEdgeWeigher<TopologyVertex, TopologyEdge>
            implements LinkWeigher {

        @Override
        public Weight weight(TopologyEdge topologyEdge) {
            //   return ScalarWeight.toWeight(1 + portStatisticsService.load(topologyEdge.link().src()).rate());
            return ScalarWeight.toWeight(2);

        }

        @Override
        public Weight getInitialWeight() {
            return DEFAULT_INITIAL_WEIGHT;
        }

        @Override
        public Weight getNonViableWeight() {
            return ScalarWeight.NON_VIABLE_WEIGHT;
        }
    }


    private void installRules1(MacAddress src, MacAddress dst, DeviceId deviceId, int tcpport, PortNumber InPort, PortNumber portNumber) {

        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchEthSrc(src)
                .matchEthDst(dst)
                .matchInPort(InPort)
                .matchIPProtocol((byte) 6)
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(IpPrefix.valueOf("10.0.0.1/24"))
                .matchIPDst(IpPrefix.valueOf("10.0.0.1/24"))
                .matchTcpDst(TpPort.tpPort(tcpport));


        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(portNumber)
//                .meter(meterId)
                .build();


        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilder.build())
                .withTreatment(treatment)
                .withPriority(flowPriority)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .makeTemporary(flowTimeout)
                .add();
        flowObjectiveService.forward(deviceId, forwardingObjective);
        log.info("FlowRule has been successfully distributed");
    }

    private void start() throws IOException {
        server = NettyServerBuilder.forPort(port)
                .addService(new ControlServiceImp())
                .build()
                .start();

        System.out.println("service start...");

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {

                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                AppComponent.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    private void installRules2(MacAddress src, MacAddress dst, DeviceId deviceId, int tcpport, PortNumber InPort, PortNumber OutPort) {

        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchEthSrc(src)
                .matchEthDst(dst)
                .matchInPort(InPort)
                .matchIPProtocol((byte) 6)
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(IpPrefix.valueOf("10.0.0.1/24"))
                .matchIPDst(IpPrefix.valueOf("10.0.0.1/24"))
                .matchTcpSrc(TpPort.tpPort(tcpport));


        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(OutPort)
//                .meter(meterId)
                .build();


        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilder.build())
                .withTreatment(treatment)
                .withPriority(flowPriority)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .makeTemporary(flowTimeout)
                .add();
        flowObjectiveService.forward(deviceId, forwardingObjective);
        log.info("FlowRule has been successfully distributed");
    }

//    public ControlResponseDatabase generateResponse(ControlRequestDatabase requests) {
//        log.info("The time slot is %s, and the requests are: %s\n",
//                requests.getTimeslot(), requests.getControlRequestList());
//        ControlResponseDatabase controlResponseDatabase = ControlResponseDatabase.newBuilder().setTimeslot(1)
//                .addControlResponse(ControlResponse.newBuilder().setChunkIndex(10).setSrc("10.0.0.1")
//                        .setDst("10.0.0.3").setTcpport(37500))
//                .addControlResponse(ControlResponse.newBuilder().setChunkIndex(11).setSrc("10.0.0.5")
//                        .setDst("10.0.0.9").setTcpport(39500)).build();
//        return controlResponseDatabase;
//    }

    private class ControlServiceImp extends ControlServiceGrpc.ControlServiceImplBase {

        public void requestResponse(ControlRequestDatabase requests, StreamObserver<ControlResponseDatabase> responseObserver) {

            log.info("The time slot is %s, and the requests are: %s\n",
                    requests.getTimeslot(), requests.getControlRequestList());
            ConnectedClientCount += 1;
            controlRequestDatabases.add(requests);

            while (true) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (ConnectedClientCount == ConnectedClientNum) {
                    ControlResponseDatabase controlResponseDatabase = Algorithm(controlRequestDatabases);
                    distributeRules(controlResponseDatabase);
                    responseObserver.onNext(controlResponseDatabase);
                    responseObserver.onCompleted();

                    ConnectedClientCount = 0;
                    controlRequestDatabases.clear();
                    System.out.println("This time slot is over");

                    break;
                }
            }
        }
    }
}
