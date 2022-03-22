package com.util;

import com.jcraft.jsch.*;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * create by wsw
 * ssh连接的工具类
 *
 * 使用说明
 *  newInstance()创建session实例
 *  exec***()执行命令
 *  closeSession()
 *
 */
public class SshExecuter implements Closeable{
    static long interval = 1000L;
    static int timeout = 3000;
    private JSch jsch = null;
    private Session session = null;
    private  ChannelExec channelExec;

    public SshExecuter(){ }

    /**
     * 使用Rsa私钥验证
     * 返回一个SshExecuter实例对象
     * 默认
     *      端口22
     *      私钥地址：/root/.ssh/id_rsa
     *      登录用户 root
     * @return
     * @throws Exception
     */
    public void newInstance(String host) throws Exception{

        //String filePath = "C:\\root\\3\\.ssh\\id_rsa";
        String filePath = "/root/.ssh/id_rsa";
        String userName = "root";
        JSch jsch = new JSch();
        jsch.addIdentity(filePath,"");
        //jsch.setKnownHosts("C:\\Users\\3\\.ssh\\known_hosts");
        session=jsch.getSession(userName, host, 22);
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect();
    }
    /**
     * 使用密码验证
     * 返回一个SshExecuter实例对象
     * @return
     * @throws Exception
     */
    public void newInstance(String host,Integer port,String user,String password) throws JSchException {
        ////log.info("主机名称>>>{},端口号>>>{},用户名>>>{},密码>>>{}",host,port,user,password);
        jsch = new JSch();
        session = jsch.getSession(  user,host,port);
        session.setPassword(password);
        session.setConfig("userauth.gssapi-with-mic", "no");
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
    }
    /**
     * 使用java给Linux发送命令，接受返回的消息
     * @param cmd 要运行的命令
     * @return
     */
    public String execToString(String cmd) {
        ////log.info("需执行的命令>>>{}",cmd);
        BufferedReader reader = null;
        ChannelExec channelExec = null;
        try {

            channelExec = (ChannelExec) session.openChannel("exec");
            channelExec.setCommand(cmd);
            channelExec.setInputStream(null);
            channelExec.setErrStream(System.err);
            channelExec.connect();
            InputStream in = channelExec.getInputStream();
            reader = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));
            String buf = null;
            StringBuffer sb = new StringBuffer();
            while ((buf = reader.readLine()) != null) {
                sb.append(buf);
                sb.append(";");
            }
            ////log.info("控制台打印结果共计>{}行,\n内容为>\n{}",sb.toString().split(";").length,sb.toString());
            return sb.toString();
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
    /**
     * 使用java连接linux的shell
     * @param userName 用户名
     * @param password 密码
     * @param host 服务器地址
     * @param port 端口号
     * @return 类shell的长连接
     */
    public void shellConn(String userName,String password,String host,int port) throws Exception {
        // //log.info("用户名>>>{},密码>>>{},主机名>>>{},端口号>>>{}",userName,password,host,port);
        BufferedReader reader = null;
        ChannelShell channelShell = null;
        Session session = null;
        JSch jsch = null;
        jsch = new JSch(); // 创建JSch对象
        session = jsch.getSession(userName, host, port); // 根据用户名，主机ip，端口获取一个Session对象
        session.setPassword(password); // 设置密码
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config); // 为Session对象设置properties
        int timeout = 60000000;
        session.setTimeout(timeout); // 设置timeout时间
        session.connect(); // 通过Session建立链接

        channelShell = (ChannelShell) session.openChannel("shell");
        channelShell.setInputStream(System.in);
        channelShell.setOutputStream(System.out);
        channelShell.connect();
    }

    /**
     * 执行一条命令，输出一个文件
     * @param cmd
     * @param outputFileName
     * @return
     * @throws Exception
     */
    public void execToFile( String cmd, String outputFileName ) throws Exception{
        ////log.info("执行的命令>>>{},输出的文件{}",cmd,outputFileName);
        ChannelShell channelShell = (ChannelShell)session.openChannel( "shell" );
        PipedInputStream pipeIn = new PipedInputStream();
        PipedOutputStream pipeOut = new PipedOutputStream( pipeIn );
        FileOutputStream fileOut = new FileOutputStream( outputFileName );
        channelShell.setInputStream( pipeIn );
        channelShell.setOutputStream( fileOut );
        channelShell.connect( timeout );
        pipeOut.write( cmd.getBytes() );
        Thread.sleep( interval );
        pipeOut.close();
        pipeIn.close();
        fileOut.close();
        channelShell.disconnect();
    }

    /**
     * 执行一条后台命令(无返回值)
     * @param cmd
     * @throws Exception
     */
    public void exec_nohup( String cmd ) throws JSchException,IOException{
        // 后台执行
        cmd = "nohup "+ cmd +" >/dev/null 2>&1 &";
        ChannelExec channelExec = (ChannelExec)session.openChannel( "exec" );
        channelExec.setCommand( cmd );
        channelExec.setInputStream( null );
        channelExec.setErrStream( System.err );
        InputStream in = channelExec.getInputStream();
        channelExec.connect();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuffer sb = new StringBuffer();
        String buf = null;
        while ((buf = reader.readLine()) != null) {
            sb.append(buf);
        }
        //.info(format("Exit//log:%s",sb.toString()));
        channelExec.disconnect();
    }

    /**
     * 执行一条命令
     * @param cmd
     * @return
     * @throws Exception
     */
    public Boolean execToBoolean( String cmd ) throws JSchException,IOException{
        ////log.info("执行的命令>>>{}",cmd);
        ChannelExec channelExec = (ChannelExec)session.openChannel( "exec" );
        channelExec.setCommand( cmd );
        channelExec.setInputStream( null );
        channelExec.setErrStream( System.err );
        InputStream in = channelExec.getInputStream();
        channelExec.connect();
        Boolean res = false;

        StringBuffer buf = new StringBuffer( 1024 );
        byte[] tmp = new byte[ 1024 ];
        String resTemp = null;

        while ( true ) {
            while ( in.available() > 0 ) {
                int i = in.read( tmp, 0, 1024 );
                if ( i < 0 ) break;
                resTemp = new String( tmp, 0, i );
                buf.append(resTemp);
            }
            if ( channelExec.isClosed() ) {
                int status = channelExec.getExitStatus();
                res = status == 0?true:false;
                //in.info( format( "Exit-status: %s", res ) );
                break;
            }
        }
        //in.info(format("Exit-//log:%s",buf.toString()));
        channelExec.disconnect();
        //in.info("返回的执行状态>>>{}",res);
        return res;
    }

    public byte[] execToByte( String cmd ) throws Exception{
        //log.info("执行的命令>>>{}",cmd);
        channelExec = (ChannelExec)session.openChannel( "exec" );
        channelExec.setCommand( cmd );
        channelExec.setInputStream( null );
        channelExec.setErrStream( System.err );
        InputStream in = channelExec.getInputStream();
        channelExec.connect();
        byte[] b= readFileByBytes(in);
        channelExec.disconnect();
        //log.info("返回的byte数组>>>{}",b);
        return b;
    }

    /**
     * 执行linux命令并接收返回值
     * @param cmd
     * @return
     * @throws Exception
     */
    public String execs(String host, String cmd) throws Exception{
        //log.info("主机名称>>>{},执行的命令>>>{}",host,cmd);
        this.newInstance(host);
        ChannelExec channelExec = (ChannelExec)session.openChannel( "exec" );
        channelExec.setCommand( cmd );
        channelExec.setInputStream( null );
        channelExec.setErrStream( System.err );
        InputStream in = channelExec.getInputStream();
        channelExec.connect();
        int res = -1;
        StringBuffer buf = new StringBuffer( 1024 );
        byte[] tmp = new byte[ 1024 ];
        String resTemp = null;
        while ( true ) {
            while ( in.available() > 0 ) {
                int i = in.read( tmp, 0, 1024 );
                if ( i < 0 ) break;
                resTemp = new String( tmp, 0, i );
                buf.append(resTemp);
            }
            if ( channelExec.isClosed() ) {
                res = channelExec.getExitStatus();
                //log.info( format( "Exit-status: %d", res ) );
                break;
            }
        }
        //log.info("执行命令{}后返回的结果集为>\n{}",cmd , buf.toString() );
        channelExec.disconnect();
        return buf.toString();
    }

    /**
     * 以字节为单位读取文件，常用于读二进制文件，如图片、声音、影像等文件。
     */
    public static byte[] readFileByBytes(String fileName) {
        //log.info("文件名称>>>{}",fileName);
        InputStream in = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            in = new FileInputStream(fileName);
            return readFileByBytes(in);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return null;
    }
    public static byte[] readFileByBytes(InputStream in) {
        //log.info("传入参数>>>{}",in);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            byte[] buf = new byte[1024];
            int length = 0;
            while ((length = in.read(buf)) != -1) {
                out.write(buf, 0, length);
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e1) {
                }
            }
        }
        //log.info("返回的数组的大小>>>{}",out.size());
        //log.info("返回的数组的具体内容>>>{}",out);
        return out.toByteArray();
    }


    /**
     * 执行一个sh命令
     *
     * @param shell
     * @return
     */
    public static String execSh(String shell) throws Exception{
        try {
            //log.info("执行execShL:"+shell);
            Process process = Runtime.getRuntime().exec(new String[] {"/bin/sh","-c",shell},null,null);
            InputStreamReader ir = new InputStreamReader(process.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
            String line;
            process.waitFor();
            StringBuffer sb = new StringBuffer();
            while ((line = input.readLine()) != null) {
                sb.append(line);
                sb.append(";");
            }
            return  sb.toString();
        } catch (Exception e) {
            //log.error(e.getMessage(), e);
            throw e;
        }
    }
    /**
     * 获得session
     * @return
     */
    public Session getSession(){
        return session;
    }

    /**
     * 关闭连接
     * @throws IOException
     */
    public void close()  throws IOException{
        getSession().disconnect();
    }
    /**
     * 执行一个sh命令
     *
     * @param shell
     * @return
     */
    public static List<String> execShReturnList(String shell) {
        //log.info("执行execShReturnList:"+shell);
        InputStreamReader isr =null;
        LineNumberReader lnr=null;
        List<String> list=new ArrayList<>();
        try {
            Process process = Runtime.getRuntime().exec(new String[] {"/bin/sh","-c",shell},null,null);
            isr = new InputStreamReader(process.getInputStream());
            lnr = new LineNumberReader(isr);
            String line;
            process.waitFor();
            while ((line = lnr.readLine()) != null) {
                list.add(line);
            }
            return  list;
        } catch (Exception e) {
            //log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }finally {
            try {
                if(isr!=null)isr.close();
            }catch (Exception e) {
                //log.error(e.getMessage(), e);
            }
            try {
                if(lnr!=null)lnr.close();
            }catch (Exception e) {
                //log.error(e.getMessage(), e);
            }
        }
    }
}
