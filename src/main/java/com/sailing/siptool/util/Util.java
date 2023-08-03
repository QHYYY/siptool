package com.sailing.siptool.util;

import com.sailing.siptool.service.InitSipStack;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.parser.StringMsgParser;

import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author ：qhy
 * @date ：Created in 2022/3/17 10:58
 * @description：
 */
public class Util {
    /**
     * 解析iperf输出 获取结果
     *
     * @param logPath
     * @return
     * @throws Exception
     */
    public static String parseLog(String logPath) throws Exception {
        File file = new File(logPath);
        if (file.isFile() && file.exists()) {
            List<String> list = readLocalFile(logPath);
            //存在%说明文件已经写完
            for (String str : list) {
                if (str.contains("%")) {
                    return str;
                }
            }
        }
        return null;
    }


    /**
     * 获取sdp监听端口
     * @param sipBody
     * @return
     */
    public static Integer getSDPListenPort(String sipBody) {
        for (String line : sipBody.split(System.lineSeparator())) {
            if (line.toLowerCase().contains("m=")){
                String s = line.split(" ")[1];
                if (s.matches("^[1-9]\\d*$")){
                    return Integer.parseInt(s);
                }
            }
        }
        throw  new RuntimeException("SDP not contain port > [line.toLowerCase().contains(\"m=video\").split(\" \")[i].matches(\"^[1-9]\\\\d*$\") ]");
    }

    public static String getCallId(String data) {
        return data.split("Call-ID: ")[1].split("\r\n")[0];
    }

    public static String getSetUp(String data) {
        if (data.contains("setup:passive")) {
            return "active";
        } else {
            return "passive";
        }
    }

    public static List<String> readLocalFile(String filepath) throws IOException {
        List<String> list = new ArrayList<>();
        BufferedReader br = null;
        InputStream inputStream = null;
        try {
            File file = new File(filepath);
            inputStream = new FileInputStream(file);
            br = new BufferedReader(new InputStreamReader(inputStream,"utf-8"));
            String eachLine = null;
            while ((eachLine = br.readLine()) != null){
                list.add(eachLine);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                br.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return list;
    }

    /**
     * 获取区间内的一个随机数
     *
     * @return
     */
    public static Set<Integer> getRandom(int x, int y, int sum) {
        Set<Integer> ports = new HashSet<>();
        for (int i = 0; i < sum * 10; i++) {
            int max = Math.max(x, y);
            int min = Math.min(x, y);
            int mid = max - min;//求差
            //产生随机数
            int num = (int) (Math.random() * (mid + 1)) + min;
            ports.add(num);
            if (ports.size() == sum)
                return ports;
        }
        return ports;
    }

    public static String getContent(SIPMessage sipMsg) {
        Object content = sipMsg.getContent();
        if (content != null ){
            String sipBody;
            if (content instanceof byte[]) {
                sipBody = new String((byte[]) content);
            }else {
                sipBody = (String) content;
            }
            return sipBody;
        }
        return null;
    }

    public static void main(String[] args) throws ParseException {
        String rawData = "INVITE sip:34020000001320000001@172.20.54.61:7777 SIP/2.0\r\n" +
                "Via: SIP/2.0/UDP 172.20.52.146:9999;rport;branch=z9hG4bK99877481\r\n" +
                "From: <sip:34020000002000000001@3402000000>;tag=909877481\r\n" +
                "To: <sip:34020000001320000001@172.20.54.61:7777>\r\n" +
                "Call-ID: 971877365\r\n" +
                "CSeq: 1610 INVITE\r\n" +
                "Content-Type: APPLICATION/SDP\r\n" +
                "Contact: <sip:34020000002000000001@172.20.52.146:9999>\r\n" +
                "Max-Forwards: 70\r\n" +
                "User-Agent: LiveGBS v211108\r\n" +
                "Subject: 34020000001320000001:0200000001,34020000002000000001:0\r\n" +
                "Route: <sip:34020000001320000001@172.20.54.61:7777>\r\n" +
                "Content-Length: 220\r\n" +
                "\r\n" +
                "v=0\r\n" +
                "o=34020000001320000001 0 0 IN IP4 172.20.52.146\r\n" +
                "s=Play\r\n" +
                "c=IN IP4 172.20.52.146\r\n" +
                "t=0 0\r\n" +
                "m=video 8091 RTP/AVP 96 97 98\r\n" +
                "a=recvonly\r\n" +
                "a=rtpmap:96 PS/90000\r\n" +
                "a=rtpmap:97 MPEG4/90000\r\n" +
                "a=rtpmap:98 H264/90000\r\n" +
                "y=0200000001\r\n";
        StringMsgParser myParser = InitSipStack.getMyParser();
        SIPMessage message = myParser.parseSIPMessage(rawData);
        System.out.println("");
    }
}
