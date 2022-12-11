package com.heima.kafka.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class HelloListener {

    @KafkaListener(topics = "test-topic")
    public void onMessage(String msg) {
        if (!StringUtils.isEmpty(msg)) {
            System.out.println("msg = " + msg);
        }
    }
}
