package com.sailing.siptool.service;

import com.sailing.siptool.common.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * @author ：qhy
 * @date ：Created in 2021/8/20 13:55
 * @description：
 */
public class SipReceive implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(SipReceive.class);

    public static ArrayBlockingQueue<DatagramPacket> receiveData = new ArrayBlockingQueue<>(2000);
    public static DatagramSocket sock;

    public SipReceive() {
        if (sock == null) {
            initPortMonitor();
        }
    }

    public static void main(String[] args) throws IOException {
        DatagramSocket socket = new DatagramSocket(12345);
        String rawData = "REGISTER sip:15020000002000000002@172.16.100.20:9991 SIP/2.0\r\n" +
                "Via: SIP/2.0/UDP 172.27.22.1:5061;branch=z9hG4bK6672557761725577ee725577f\r\n" +
                "Call-ID: 0e3bffa6093bffa6863bffa6953bffa68b3b@172.27.22.1\r\n" +
                "From: <sip:50011200012000000001@172.27.22.1:5061>;tag=1680a8ec1180a8ec9e80a8ec8d80a8ec\r\n" +
                "To: <sip:50011200012000000001@172.27.22.1>\r\n" +
                "CSeq: 29389938 REGISTER\r\n" +
                "Contact: <sip:50011200012000000001@172.27.22.1:5061>\r\n" +
                "OutUserInfo: DomainId=5001120001;UserName=15020000002000000002u;UserPri=10\r\n" +
                "Max-Forwards: 70\r\n" +
                "Expires: 3600\r\n" +
                "User-Agent: IMOS/V3\r\n" +
                "RegMode: PLAT;Describe=H3C-VM9500;Register;DevVer=Plat1.0\r\n" +
                "Content-Length: 0\r\n\r\n";
        byte[] dataBytes = rawData.getBytes
                ("gb2312");
        DatagramPacket datagramPacket = new DatagramPacket(dataBytes,
                dataBytes.length,
                InetAddress.getByName("172.20.54.61"),
                5060);
        for (int i = 0;i< 8000;i++) {
            socket.send(datagramPacket);
            System.out.println(i);
        }
    }


    private static void initPortMonitor() {
        try {
            if ("server".equalsIgnoreCase(Constant.getSysProperties().getMode())) {
                sock = new DatagramSocket(Constant.getSysProperties().getServerPort());
            } else {
                sock = new DatagramSocket(Constant.getSysProperties().getClientPort());
            }
        } catch (Exception e) {
            if (e instanceof BindException) {
                log.error("端口被占用，程序退出。msg:{}", e.getMessage());
                System.exit(12);
            } else {
                log.error("开放udp端口错误，msg:{}", e.getMessage());
            }
        }
    }

    @Override
    public void run() {
        while (true) {
            byte[] buf = new byte[20 * 1024];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                getSock().receive(packet);
            } catch (Exception e) {
                log.error("udp接收数据错误，msg:{}", e.getMessage());
            }
            try {
                receiveData.put(packet);
                if (receiveData.size() > 20) {
                    log.warn("数据队列容量报警===[receiveData]={}", receiveData.size());
                }
            } catch (Exception e) {
                log.error("udp接收完缓存错误，msg:{}", e.getMessage());
            }
        }
    }

    public static DatagramSocket getSock() {
        return sock;
    }
}
