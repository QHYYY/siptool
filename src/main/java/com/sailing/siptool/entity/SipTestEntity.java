package com.sailing.siptool.entity;

/**
 * @author ：qhy
 * @date ：Created in 2021/8/23 15:57
 * @description：
 */
public class SipTestEntity {
    private String protocol;//udp or tcp
    private int routeNum;//发送数量

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public int getRouteNum() {
        return routeNum;
    }

    public void setRouteNum(int routeNum) {
        this.routeNum = routeNum;
    }
}
