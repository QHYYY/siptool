package com.sailing.siptool.service;

import com.sailing.siptool.common.Constant;
import com.sailing.siptool.util.SshExecuter;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.parser.StringMsgParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.ParseException;

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

    public SipSend(int streamPort) {
        this.streamPort = streamPort;
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
        String sipMessage = getInviteSipMessage(streamPort);
        byte[] dataBytes = sipMessage.getBytes("gb2312");
        DatagramPacket datagramPacket = new DatagramPacket(dataBytes,
                dataBytes.length,
                InetAddress.getByName(clientIp),
                clientPort);
        datagramSocket.send(datagramPacket);
        log.info("发送" + sipMessage);
        exeIperfServer(streamPort);
    }


    /**
     * 生成INVITE信令
     *
     * @throws Exception
     */
    private String getInviteSipMessage(int streamPort) {
        String inviteRawData = "INVITE sip:34020000001320000001@" + Constant.getSysProperties().getClientIp() + ":" + Constant.getSysProperties().getClientPort() + " SIP/2.0\r\n" +
                "Via: SIP/2.0/UDP " + Constant.getSysProperties().getServerIp() + ":" + Constant.getSysProperties().getServerPort() + ";rport;branch=z9hG4bK99877481\r\n" +
                "From: <sip:34020000002000000001@3402000000>;tag=909877481\r\n" +
                "To: <sip:34020000001320000001@" + Constant.getSysProperties().getClientIp() + ":" + Constant.getSysProperties().getClientPort() + ">\r\n" +
                "Call-ID: 971877365\r\n" +
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
                "m=video " + streamPort + " RTP/AVP 96 97 98\r\n" +
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
    public static void sendSipResponseMsg(Integer streamPort) throws IOException {

        String rawResponseStr = "SIP/2.0 200 OK\r\n" +
                "Via: SIP/2.0/UDP " + serverIpPort + ";rport=15060;received=" + Constant.getSysProperties().getServerIp() + ";branch=z9hG4bK249269433\r\n" +
                "From: <sip:34020000002000003246@3402000000>;tag=270269433\r\n" +
                "To: <sip:34020000001320000001@" + clientIpPort + ">;tag=903871041\r\n" +
                "CSeq: 152 INVITE\r\n" +
                "Call-ID: 655269416\r\n" +
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
                "m=video " + streamPort + " RTP/AVP 96\r\n" +
                "a=sendonly\r\n" +
                "a=rtpmap:96 PS/90000\r\n" +
                "a=setup:positive\r\n" +
                "a=connection:new\r\n" +
                "y=0200000001\r\n";


        byte[] dataBytes = rawResponseStr.getBytes();
        DatagramPacket datagramPacket = new DatagramPacket(dataBytes,
                dataBytes.length,
                InetAddress.getByName(Constant.getSysProperties().getClientIp()), Constant.getSysProperties().getClientPort());
        datagramSocket.send(datagramPacket);
    }

    /**
     * 发送ACK给服务端
     *
     * @throws IOException
     */
    public static void sendAckToServer() throws IOException {
        String rawAckData = "ACK sip:34020000001320000001@" + clientIpPort + " SIP/2.0\r\n" +
                "Via: SIP/2.0/UDP " + serverIpPort + ";rport;branch=z9hG4bK980272289\r\n" +
                "From: <sip:34020000002000003246@3402000000>;tag=270269433\r\n" +
                "To: <sip:34020000001320000001@" + clientIpPort + ">;tag=903871041\r\n" +
                "Call-ID: 655269416\r\n" +
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
            //开启iperf收流


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
