package com.sailing.siptool.entity;


import com.sailing.siptool.common.Constant;
import gov.nist.javax.sip.message.SIPMessage;
import lombok.Data;
import lombok.ToString;

/**
 * 上下级平台的IP
 * @author wsw
 */
@Data
@ToString
@SuppressWarnings("AlibabaClassNamingShouldBeCamel")
public class HostPortEntity {

    public static final String FROM_UP = "fromUp";
    public static final String FROM_DOWN = "fromDown";

    private String protocol;
    private String upHost ;
    private int upPort ;
    private String downHost ;
    private int downPort ;
    private String myHost ;
    private int myPort ;
    private String peerHost ;
    private int peerPort ;
    /** 区别发送时候的源和目标 */
    private String sourcePeerHost ;
    /** 区别发送时候的源和目标 */
    private int sourcePeerPort ;
    private String requestDirect;
    private boolean requestFromUp;


    private SIPMessage sipMessage;


    public boolean isRequestFromUp() {
        return requestFromUp && requestDirect !=null;
    }


    public static boolean checkIpEquals(String host1, String host2){
        if (host1.equalsIgnoreCase(host2)){
            return true;
        }
        return false;
    }

    public String getUpHost() {
        return upHost;
    }

    public void setUpHost(String upHost) {
        this.upHost = upHost;
    }

    public int getUpPort() {
        return upPort;
    }

    public void setUpPort(int upPort) {
        this.upPort = upPort;
    }

    public String getDownHost() {
        return downHost;
    }

    public void setDownHost(String downHost) {
        this.downHost = downHost;
    }

    public int getDownPort() {
        return downPort;
    }

    public void setDownPort(int downPort) {
        this.downPort = downPort;
    }

    /**
     * 返回一个用于数据发送的包装对象
     */
    public static HostPortEntity instance(SIPMessage sipMessage,String myHost,int myPort,
                                          String peerHost, int peerPort,String sourcePeerHost,
                                          int sourcePeerPort, String protocol) {
        HostPortEntity entity = new HostPortEntity();
        entity.setSipMessage(sipMessage);
        entity.setMyHost(myHost);
        entity.setMyPort(myPort);
        entity.setPeerHost(peerHost);
        entity.setPeerPort(peerPort);
        entity.setSourcePeerHost(sourcePeerHost);
        entity.setSourcePeerPort(sourcePeerPort);
        entity.setProtocol(protocol);
        return entity;
    }

    /**
     * @Description: 返回一个包装内部request以及ipPort的对象
     * @Param:
     * @return: [req, upHost, upPort, downHost, downPort]
     * @Author: wangsw
     * @date:
     */
    public static HostPortEntity instance(SIPMessage req, String upHost, int upPort, String downHost, int downPort){
        HostPortEntity entity = new HostPortEntity();
        entity.setSipMessage(req);
        entity.setDownHost(downHost);
        entity.setDownPort(downPort);
        entity.setUpHost(upHost);
        entity.setUpPort(upPort);
        return entity;
    }
    /**
     * @Description: 返回一个审计数据包装对象
     * @Param:
     * @return: [req, upHost, upPort, downHost, downPort, peerHost, peerPort]
     * @Author: wangsw
     * @date:
     */
    public static HostPortEntity instance(SIPMessage req, String upHost, int upPort, String downHost, int downPort,String peerHost,int peerPort) {
        HostPortEntity entity = new HostPortEntity();
        entity.setSipMessage(req);
        entity.setDownHost(downHost);
        entity.setDownPort(downPort);
        entity.setUpHost(upHost);
        entity.setUpPort(upPort);
        entity.setPeerHost(peerHost);
        entity.setPeerPort(peerPort);
        return entity;
    }
    /**
     * @Description: 返回一个上下级网闸的编组包装对象
     * @Param:
     * @return: [req, upHost, upPort, downHost, downPort, peerHost, peerPort]
     * @Author: wangsw
     * @date:
     */
    public static HostPortEntity instance(String upHost,String downHost) {
        HostPortEntity entity = new HostPortEntity();
        entity.setDownHost(downHost);
        entity.setUpHost(upHost);
        return entity;
    }
    public static HostPortEntity instance(String upHost,int upPort,String downHost,int downPort) {
        HostPortEntity entity = new HostPortEntity();
        entity.setDownHost(downHost);
        entity.setDownPort(downPort);
        entity.setUpHost(upHost);
        entity.setUpPort(upPort);
        return entity;
    }
    public String getPeerHost() {
        return peerHost;
    }

    public void setPeerHost(String peerHost) {
        this.peerHost = peerHost;
    }

    public int getPeerPort() {
        return peerPort;
    }

    public void setPeerPort(int peerPort) {
        this.peerPort = peerPort;
    }

    public SIPMessage getSipMessage() {
        return sipMessage;
    }

    public void setSipMessage(SIPMessage sipMessage) {
        this.sipMessage = sipMessage;
    }
}
