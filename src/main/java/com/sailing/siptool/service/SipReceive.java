package com.sailing.siptool.service;

import com.sailing.siptool.common.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
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
