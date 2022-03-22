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
    private static final SysProperties sysProperties = Constant.getSysProperties();
    public static final String LOG_PATH = "/opt/siptool/log/";
    private final ExecutorService rtpPool = Executors.newCachedThreadPool();


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
            //回个200OK
            SipSend.sendSipResponseMsg(monitorPort);

            //开启打流(异步）
            //iperf无法指定发送端口 iperf3可以
            Thread iperfThread = new Thread(() -> {
                try {
                    String cmd = "iperf -c " + sysProperties.getServerIp() + " -p " + monitorPort + " -b "+sysProperties.getFlowSize()+ "-i 1 "+
                            "-t " + sysProperties.getTransTime();
                    if (sysProperties.getProtocol().equalsIgnoreCase("udp")) {
                        cmd = cmd +" -u";
                    }
                    String filePath = LOG_PATH + monitorPort + ".txt";
                    //将打流结果输出到文件
                    cmd = cmd + " > " + filePath + " &";
                    SshExecuter.execSh(cmd);
                    log.debug(Thread.currentThread() + "客户端开始打流，命令为：" + cmd);
                    while (true) {
                        String result = Util.parseLog(filePath);
                        if (!StringUtils.isEmpty(result)) {
                            log.info("\r\n流端口" + monitorPort + "出结果啦：" + result + "\r\n丢包率为："+result.split("\\(")[1].split("\\)")[0] +"哦");
                            break;
                        }
                        Thread.sleep(1000);
                    }
                }catch (Exception e) {
                    log.error("打流报错:"+e);
                }

            });
            rtpPool.execute(iperfThread);
        }
        //处理200OK
        else if (rawData.startsWith("SIP/2.0")) {
            //回复ACK给客户端
            SipSend.sendAckToServer();
        }
    }


    /**
     * 处理udp包
     *
     * @param datagramPacket
     * @throws Exception
     */
//    private void processIncomingDataPacket(DatagramPacket datagramPacket) throws Exception {
//        int length = datagramPacket.getLength();
//        byte[] bytes = datagramPacket.getData();
//
//        byte[] bytes1 = new byte[length];
//        System.arraycopy(bytes, 0, bytes1, 0, length);
//
//        SIPMessage sipMessage;
//        try {
//            sipMessage = InitSipStack.getMyParser().parseSIPMessage(bytes1);
//        } catch (Exception parseException) {
//            parseException.printStackTrace();
//            return;
//        }
//        String rtpAddress = sipMessage.getMessageContent();
//        String[] ipPort = rtpAddress.split(":");
//        if (sipMessage instanceof SIPRequest) {
//            SipUri uri = (SipUri) sipMessage.getFrom().getAddress().getURI();
//            String clientHost = uri.getHost();
//            int clientPort = uri.getPort();
//            //发送200OK给客户端
//            log.info("clientHost:" + clientHost + " clientPort:" + clientPort);
//            SipSend.sendSipResponseMsg(clientHost, clientPort, ipPort[1]);
//            //开始接收媒体流
//            String cmd = "iperf -s -p" + ipPort[1] + " -i1";
//            if (sysProperties.getProtocol().equalsIgnoreCase("udp")) {
//                cmd = cmd + " -u";
//            }
//            log.info(cmd);
//
//            String finalCmd = cmd;
//            fixedThreadPool.submit(() -> {
//                try {
//                    String result = SshExecuter.execSh(finalCmd);
//                    log.info("server执行iperf完成:" + result);
//                } catch (Exception e) {
//                    log.error(e.getMessage());
//                }
//            });
//
//        } else {
//            SIPResponse sipResponse = (SIPResponse) sipMessage;
//            if (sipResponse.getStatusCode() == 200) {
//                // 回复ACK给服务端
//                SipSend.sendAckToServer();
//                // 开始发送媒体流
//                String cmd = "iperf -c " + sysProperties.getServerIp()
//                        + " -p" + ipPort[1] + " -i1 -b " + sysProperties.getFlowSize();
//                if (sysProperties.getProtocol().equalsIgnoreCase("udp")) {
//                    cmd = cmd + " -u";
//                }
//                if (!StringUtils.isEmpty(sysProperties.getTransTime())) {
//                    cmd = cmd + " -t " + sysProperties.getTransTime();
//                }
//                log.info(cmd);
//                String finalCmd = cmd;
//                fixedThreadPool.submit(() -> {
//                    try {
//                        String result = SshExecuter.execSh(finalCmd);
//                        log.info("client执行iperf完成:" + result);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                });
//            }
//        }
//    }
}
