package com.sailing.siptool.service;

import com.alibaba.fastjson.JSONObject;
import com.sailing.siptool.common.Constant;
import com.sailing.siptool.util.SshExecuter;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.parser.StringMsgParser;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.sailing.siptool.util.HttpClientUtils.doPostRequest;

/**
 * @author ：qhy
 * @date ：Created in 2021/8/20 16:47
 * @description：
 */
public class SipSend implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(SipSend.class);
    private static final DatagramSocket datagramSocket = SipReceive.getSock();
    private static final String clientIpPort = Constant.getSysProperties().getClientIp() + ":" + Constant.getSysProperties().getClientPort();
    private static final String serverIpPort = Constant.getSysProperties().getServerIp() + ":" + Constant.getSysProperties().getServerPort();
    private final int streamPort;
    private final boolean schedule;
    private String callId;
    private static final int httpPort = 8081;


    public SipSend(int streamPort,boolean schedule) {
        this.streamPort = streamPort;
        this.schedule = schedule;
    }
    /**
     * 发送INVITE信令给客户端
     *
     * @throws Exception
     */
    public void sendInviteMessage() throws Exception {
        //客户端ip port
        String clientIp = Constant.getSysProperties().getClientIp();
        int clientPort = Constant.getSysProperties().getClientPort();
        //生成INVITE信令
        //todo 和引擎一样取流端口
        String sipMessage = getInviteSipMessage(streamPort);
        //todo 通过接口告诉C程序需要监听哪个端口
        openMonitorPort(streamPort,Constant.getSysProperties().getProtocol(),callId);

        byte[] dataBytes = sipMessage.getBytes("gb2312");
        DatagramPacket datagramPacket = new DatagramPacket(dataBytes,
                dataBytes.length,
                InetAddress.getByName(clientIp),
                clientPort);
        datagramSocket.send(datagramPacket);
        log.info("发送" + sipMessage);
//        exeIperfServer(streamPort);
    }

    public void sendByeMessage() throws Exception {
        String rawData = "BYE sip:34020000001320000001@"+Constant.getSysProperties().getClientIp() + ":" + Constant.getSysProperties().getClientPort() + " SIP/2.0\r\n" +
                "Via: SIP/2.0/UDP "+ Constant.getSysProperties().getServerIp() + ":" + Constant.getSysProperties().getServerPort() + ";rport;branch=z9hG4bK293694034\r\n" +
                "From: <sip:34020000001320000001@3402000000>;tag=898683844\r\n" +
                "To: <sip:34020000001310000008@"+Constant.getSysProperties().getClientIp() + ":" + Constant.getSysProperties().getClientPort() +">;tag=477681933\r\n" +
                "Call-ID: " + callId + "\r\n" +
                "CSeq: 179 BYE\r\n" +
                "Contact: <sip:34020000001320000001@"+Constant.getSysProperties().getServerIp() + ":" + Constant.getSysProperties().getServerPort()+">\r\n" +
                "Max-Forwards: 70\r\n" +
                "User-Agent: LiveGBS v230630\r\n" +
                "Content-Length: 0\r\n\r\n";
        byte[] dataBytes = rawData.getBytes("gb2312");
        DatagramPacket datagramPacket = new DatagramPacket(dataBytes,
                dataBytes.length,
                InetAddress.getByName(Constant.getSysProperties().getClientIp()),
                Constant.getSysProperties().getClientPort());
        //todo 通知C把收流端口关了
        closeMonitorPort(callId);
        datagramSocket.send(datagramPacket);
    }

    public static void sendRespToBye(String callId) throws Exception {
        String rawData = "SIP/2.0 200 OK\r\n" +
                "Via: SIP/2.0/UDP " + Constant.getSysProperties().getServerIp() + ":" + Constant.getSysProperties().getServerPort() + ";rport=17000;received=61.20.194.2;branch=z9hG4bK688707988\r\n" +
                "From: <sip:34020000001320000001@3402000000>;tag=452697800\r\n" +
                "To: <sip:34020000001310000009@" + Constant.getSysProperties().getClientIp() + ":" + Constant.getSysProperties().getClientPort() + ">;tag=116695884\r\n" +
                "CSeq: 185 BYE\n" +
                "Call-ID: " + callId + "\r\n" +
                "User-Agent: LiveGBS v230630\r\n" +
                "Contact: <sip:34020000001320000001@" + Constant.getSysProperties().getClientIp() + ":" + Constant.getSysProperties().getClientPort() + ">\n" +
                "Content-Length: 0\r\n\r\n";

        byte[] dataBytes = rawData.getBytes();
        DatagramPacket datagramPacket = new DatagramPacket(dataBytes,
                dataBytes.length,
                InetAddress.getByName(Constant.getSysProperties().getServerIp()), Constant.getSysProperties().getServerPort());
        datagramSocket.send(datagramPacket);
    }


    /**
     * 生成INVITE信令
     *
     * @throws Exception
     */
    public String getInviteSipMessage(int streamPort) {
        String protocol;
        String setup = "passive";
        if (Constant.getSysProperties().getProtocol().equalsIgnoreCase("udp")) {
            protocol = " ";
        } else {
            protocol = " TCP/";
        }
        if (Constant.getSysProperties().getProtocol().equalsIgnoreCase("tcpActive")) {
            setup = "active";
        }
        callId = UUID.randomUUID().toString();
        String inviteRawData = "INVITE sip:34020000001320000001@" + Constant.getSysProperties().getClientIp() + ":" + Constant.getSysProperties().getClientPort() + " SIP/2.0\r\n" +
                "Via: SIP/2.0/UDP " + Constant.getSysProperties().getServerIp() + ":" + Constant.getSysProperties().getServerPort() + ";rport;branch=z9hG4bK99877481\r\n" +
                "From: <sip:34020000002000000001@3402000000>;tag=909877481\r\n" +
                "To: <sip:34020000001320000001@" + Constant.getSysProperties().getClientIp() + ":" + Constant.getSysProperties().getClientPort() + ">\r\n" +
                "Call-ID: "+ callId +  "\r\n" +
                "CSeq: 1610 INVITE\r\n" +
                "Content-Type: APPLICATION/SDP\r\n" +
                "Contact: <sip:34020000002000000001@" + Constant.getSysProperties().getServerIp() + ":" + Constant.getSysProperties().getServerPort() + ">\r\n" +
                "Max-Forwards: 70\r\n" +
                "User-Agent: LiveGBS v211108\r\n" +
                "Subject: 34020000001320000001:0200000001,34020000002000000001:0\r\n" +
                "Route: <sip:34020000001320000001@" + Constant.getSysProperties().getClientIp() + ":" + Constant.getSysProperties().getClientPort() + ">\r\n" +
                "Content-Length: 220\r\n" +
                "\r\n" +
                "v=0\r\n" +
                "o=34020000001320000001 0 0 IN IP4 " + Constant.getSysProperties().getServerIp() + "\r\n" +
                "s=Play\r\n" +
                "c=IN IP4 " + Constant.getSysProperties().getServerIp() + "\r\n" +
                "t=0 0\r\n" +
                "m=video " + streamPort + protocol + "RTP/AVP 96 97 98\r\n" +
                "a=setup:" + setup + "\r\n" +
                "a=recvonly\r\n" +
                "a=rtpmap:96 PS/90000\r\n" +
                "a=rtpmap:97 MPEG4/90000\r\n" +
                "a=rtpmap:98 H264/90000\r\n" +
                "y=0200000001\r\n\r\n";
        return inviteRawData;
    }

    /**
     * 发送200 OK给服务端
     * 捎上流端口给服务端发流
     *
     * @param streamPort
     * @throws IOException
     */
    public static void sendSipResponseMsg(Integer streamPort,String callId) throws IOException {
        String protocol;
        String setup = "active";
        if (Constant.getSysProperties().getProtocol().equalsIgnoreCase("udp")) {
            protocol = " ";
        } else {
            protocol = " TCP/";
        }
        if (Constant.getSysProperties().getProtocol().equalsIgnoreCase("tcpActive")) {
            setup = "passive";
        }

        String rawResponseStr = "SIP/2.0 200 OK\r\n" +
                "Via: SIP/2.0/UDP " + serverIpPort + ";rport=15060;received=" + Constant.getSysProperties().getServerIp() + ";branch=z9hG4bK249269433\r\n" +
                "From: <sip:34020000002000003246@3402000000>;tag=270269433\r\n" +
                "To: <sip:34020000001320000001@" + clientIpPort + ">;tag=903871041\r\n" +
                "CSeq: 152 INVITE\r\n" +
                "Call-ID: "+ callId +  "\r\n" +
                "User-Agent: LiveGBS v220127\r\n" +
                "Contact: <sip:34020000002000005274@" + clientIpPort + ">\r\n" +
                "Content-Type: APPLICATION/SDP\r\n" +
                "Content-Length: 203\r\n" +
                "\r\n" +
                "v=0\r\n" +
                "o=34020000001110000001 0 0 IN IP4 " + Constant.getSysProperties().getClientIp() + "\r\n" +
                "s=Play\r\n" +
                "c=IN IP4 " + Constant.getSysProperties().getClientIp() + "\r\n" +
                "t=0 0\r\n" +
                "m=video " + streamPort + protocol +"RTP/AVP 96\r\n" +
                "a=sendonly\r\n" +
                "a=rtpmap:96 PS/90000\r\n" +
                "a=setup:" + setup + "\r\n" +
                "a=connection:new\r\n" +
                "y=0200000001\r\n";
        if (!Constant.getSysProperties().getProtocol().equalsIgnoreCase("tcpActive")) {
            //todo 告诉C程序把流发送给streamPort
            sendStream(Constant.getSysProperties().getServerIp() + ":" + streamPort, Constant.getSysProperties().getProtocol(), String.valueOf(streamPort),callId);
        } else {
            openMonitorPort(streamPort,"tcp",callId);
        }


        byte[] dataBytes = rawResponseStr.getBytes();
        DatagramPacket datagramPacket = new DatagramPacket(dataBytes,
                dataBytes.length,
                InetAddress.getByName(Constant.getSysProperties().getServerIp()), Constant.getSysProperties().getServerPort());
        datagramSocket.send(datagramPacket);
    }

    /**
     * 发送ACK给服务端
     *
     * @throws IOException
     */
    public static void sendAckToServer(String callId) throws IOException {
        String rawAckData = "ACK sip:34020000001320000001@" + clientIpPort + " SIP/2.0\r\n" +
                "Via: SIP/2.0/UDP " + serverIpPort + ";rport;branch=z9hG4bK980272289\r\n" +
                "From: <sip:34020000002000003246@3402000000>;tag=270269433\r\n" +
                "To: <sip:34020000001320000001@" + clientIpPort + ">;tag=903871041\r\n" +
                "Call-ID: " + callId + "\r\n" +
                "CSeq: 152 ACK\r\n" +
                "Contact: <sip:34020000002000003246@" + serverIpPort + ">\r\n" +
                "Max-Forwards: 70\r\n" +
                "User-Agent: LiveGBS v220127\r\n" +
                "Content-Length: 0\r\n";

        byte[] dataBytes = rawAckData.getBytes();
        DatagramPacket datagramPacket = new DatagramPacket(dataBytes,
                dataBytes.length,
                InetAddress.getByName(Constant.getSysProperties().getServerIp()), Constant.getSysProperties().getServerPort());
        datagramSocket.send(datagramPacket);
    }

    /**
     * 开启iperf收流
     *
     * @param monitorPort
     */
    public void exeIperfServer(int monitorPort) throws Exception {
        String cmd = "iperf -s -u " + "-p" + monitorPort + "-i 1";
        SshExecuter.execSh(cmd);
    }

    @Override
    public void run() {
        try {
            //发送invite请求
            sendInviteMessage();
            //如果是巡检的，那么过几秒钟要发BYE
            if (schedule) {
                Thread.sleep(Constant.getSysProperties().getScheduleCheckInterval() * 1000);
                sendByeMessage();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 接收端
     * @param port
     * @param protocol
     */
    public static void openMonitorPort(int port,String protocol,String callId) {
        String url = "http://localhost:"+httpPort+"/api/v1/create";
        if (protocol.equalsIgnoreCase("tcp")) {
            url = "http://localhost:"+httpPort+"/api/v1/create/passive";
        } else if (protocol.equalsIgnoreCase("tcpActive")) {
            url = "http://localhost:"+httpPort+"/api/v1/create/active";
        }
        Map<String, String> requestHeader = new HashMap<>();
        requestHeader.put("Content-Type", "application/json");
        requestHeader.put("Accept", "*/*");
        JSONObject param = new JSONObject();
        param.put("call_id", callId);
        param.put("local", ":" + port);
        param.put("remote",null);
        StringEntity stringEntity = new StringEntity(param.toString(), ContentType.APPLICATION_JSON);
        System.out.println("接收端："+param );
        String postRequest = doPostRequest(url, requestHeader, null, stringEntity);
        System.out.println(postRequest);
    }

    public static void closeMonitorPort(String callId) {
        String url = "http://localhost:"+httpPort+"/api/v1/remove";
        Map<String, String> requestHeader = new HashMap<>();
        requestHeader.put("Content-Type", "application/json");
        requestHeader.put("Accept", "*/*");
        JSONObject param = new JSONObject();
        param.put("call_id", callId);
        StringEntity stringEntity = new StringEntity(param.toString(), ContentType.APPLICATION_JSON);
        String postRequest = doPostRequest(url, requestHeader, null, stringEntity);
        System.out.println(postRequest);
    }

    /**
     * 发送端
     * @param dst
     * @param protocol
     */
    public static void sendStream(String dst ,String protocol,String port,String callId) {
        String url = "http://localhost:"+httpPort+"/api/v1/create";
        if (protocol.equalsIgnoreCase("tcp")) {
            url = "http://localhost:"+httpPort+"/api/v1/create/passive";
        } else if (protocol.equalsIgnoreCase("tcpActive")) {
            url = "http://localhost:"+httpPort+"/api/v1/create/active";
        }
        Map<String, String> requestHeader = new HashMap<>();
        requestHeader.put("Content-Type", "application/json");
        requestHeader.put("Accept", "*/*");
        JSONObject param = new JSONObject();
        param.put("remote", dst);
        param.put("local",":"+port);
        param.put("call_id",callId);
        StringEntity stringEntity = new StringEntity(param.toString(), ContentType.APPLICATION_JSON);
        System.out.println("发送端："+param );
        String postRequest = doPostRequest(url, requestHeader, null, stringEntity);
        System.out.println(postRequest);
    }


}
