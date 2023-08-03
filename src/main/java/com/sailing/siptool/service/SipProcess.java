package com.sailing.siptool.service;

import com.sailing.siptool.common.Constant;
import com.sailing.siptool.common.SysProperties;
import com.sailing.siptool.util.SshExecuter;
import com.sailing.siptool.util.Util;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.net.DatagramPacket;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author ：qhy
 * @date ：Created in 2021/8/20 14:36
 * @description：
 */
public class SipProcess implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(SipProcess.class);
    public static final String LOG_PATH = "/opt/siptool/log/";


    @Override
    public void run() {
        Thread.currentThread().setName("SipProcess-" + UUID.randomUUID().toString().substring(0, 4));
        while (true) {
            try {
                DatagramPacket packet = SipReceive.receiveData.take();
                processIncomingDataPacket(packet);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("接收到的packet取出并处理报错，msg:{}", e.getStackTrace());
            }
        }
    }

    /**
     * 处理udp包
     *
     * @param datagramPacket
     * @throws Exception
     */
    private void processIncomingDataPacket(DatagramPacket datagramPacket) throws Exception {
        int length = datagramPacket.getLength();
        byte[] bytes = datagramPacket.getData();

        byte[] bytes1 = new byte[length];
        System.arraycopy(bytes, 0, bytes1, 0, length);

        SIPMessage sipMessage;
        try {
            sipMessage = InitSipStack.getMyParser().parseSIPMessage(bytes1);
        } catch (Exception parseException) {
            parseException.printStackTrace();
            return;
        }
        String rawData = new String(bytes1, "gb2312");
        //处理invite请求
        if (sipMessage instanceof SIPRequest && ((SIPRequest) sipMessage).getMethod().equalsIgnoreCase("INVITE")) {
            String sipBody = Util.getContent(sipMessage);
            Integer monitorPort = Util.getSDPListenPort(sipBody);
            String callId = Util.getCallId(rawData);
            //回个200OK
            SipSend.sendSipResponseMsg(monitorPort,callId);
        }
        //处理200OK
        else if (rawData.startsWith("SIP/2.0") && rawData.contains("Play")) {
            if (Constant.getSysProperties().getProtocol().equalsIgnoreCase("tcpActive")) {
                String sipBody = Util.getContent(sipMessage);
                Integer dstPort = Util.getSDPListenPort(sipBody);
                String callId = Util.getCallId(rawData);
                //todo 发送消息给C向200OK的端口发流
                SipSend.sendStream(Constant.getSysProperties().getClientIp() + ":" + dstPort, "tcp", dstPort.toString(),callId);
            }
            String callId = Util.getCallId(rawData);
            //回复ACK给客户端
            SipSend.sendAckToServer(callId);
        }
        //处理BYE
        else if (rawData.startsWith("BYE")) {
            String callId = Util.getCallId(rawData);
            SipSend.closeMonitorPort(callId);
            SipSend.sendRespToBye(callId);
        }
    }
}
