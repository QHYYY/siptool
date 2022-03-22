package com.sailing.siptool;

import com.sailing.siptool.common.Constant;
import com.sailing.siptool.service.InitSipStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.URL;

@SpringBootApplication
public class SipToolApplication {
    private static final Logger log = LoggerFactory.getLogger(SipToolApplication.class);

    public static void main(String[] args) {
        if (args.length == 0) {
            URL config = SipToolApplication.class.getClassLoader().getResource("application.properties");
            if (config != null) {
                Constant.initConfig(config.getPath());
            } else {
                // 初始化系统配置(默认配置)
                // 需要跟配置文件路径
                System.err.println("Start Filed");
                System.err.println("Please start with config file");
                log.error("Start Filed");
                log.error("Please start with config file");
                System.exit(0);
            }
        } else {
            String configPath = args[0];
            Constant.initConfig(configPath);
        }

        initSipStack();
        SpringApplication.run(SipToolApplication.class, args);

    }

    private static void initSipStack() {
        InitSipStack initSipStack = new InitSipStack();
        initSipStack.init();
    }
}
