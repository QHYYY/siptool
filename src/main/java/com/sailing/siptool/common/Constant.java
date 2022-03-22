package com.sailing.siptool.common;

import java.net.InetAddress;

/**
 * @author ：qhy
 * @date ：Created in 2021/8/20 14:11
 * @description：
 */
public class Constant {
    public static int serverPort = 5061;
    public static String clientIp;
    public static int clientPort;
    public static String mode;//server/client
    public static String protocol;//协议udp tcp
    public static int routeNum;//流量数量
    public static String streamPortRange;//传输流量端口范围
    public static String flowSize;//流量带宽大小
    public static Integer transTime;//流量传输时间 单位秒

    private static SysProperties sysProperties;

    public static void initConfig(String configPath) {
        sysProperties = new SysProperties(configPath);
    }

    public static SysProperties getSysProperties() {
        return sysProperties;
    }

    public static String makeKey(InetAddress var0, int var1) {
        // 为了适配网闸的情况,这里的key选用了只选用ip,不适用端口
//        return var0.getHostAddress() + ":" + var1;
        return var0.getHostAddress() ;
    }
}
