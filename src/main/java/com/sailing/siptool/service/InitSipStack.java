package com.sailing.siptool.service;

import com.sailing.siptool.common.Constant;
import com.sailing.siptool.util.Util;
import gov.nist.javax.sip.parser.StringMsgParser;

import java.io.File;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InitSipStack {

    private static StringMsgParser myParser;
    private final ExecutorService fixedThreadPool = Executors.newFixedThreadPool(2000);

    public void init() {
        try {
            File log = new File(SipProcess.LOG_PATH);
            if (!log.exists()) {
                log.mkdir();
            }
            new Thread(new SipReceive()).start();
            new Thread(new SipProcess()).start();
            //如果是server，发送invite信令
            if (Constant.getSysProperties().getMode().equalsIgnoreCase("server")) {
                String streamPortRange = Constant.getSysProperties().getStreamPortRange();
                String[] split = streamPortRange.split("-");
                //获取流媒体范围内的端口（接收流用）
                Set<Integer> random = Util.getRandom(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Constant.getSysProperties().getRouteNum());
                for (int streamPort : random) {
                    fixedThreadPool.execute(new SipSend(streamPort));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static StringMsgParser getMyParser() {
        if (myParser == null) {
            myParser = new StringMsgParser();
            myParser.setParseExceptionListener((e, sipMessage, clazz, s, s1) -> {
            });
        }
        return myParser;
    }
}
