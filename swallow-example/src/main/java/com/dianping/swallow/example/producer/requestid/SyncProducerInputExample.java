package com.dianping.swallow.example.producer.requestid;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.dianping.phoenix.environment.PhoenixContext;
import com.dianping.swallow.common.message.Destination;
import com.dianping.swallow.producer.Producer;
import com.dianping.swallow.producer.ProducerConfig;
import com.dianping.swallow.producer.ProducerMode;
import com.dianping.swallow.producer.impl.ProducerFactoryImpl;

/**
 */
public class SyncProducerInputExample {

    public static void main(String[] args) throws Exception {
        ProducerConfig config = new ProducerConfig();
        config.setMode(ProducerMode.SYNC_MODE);
        Producer p = ProducerFactoryImpl.getInstance().createProducer(Destination.topic("example"), config);
        String CurLine = ""; // Line read from standard in

        System.out.println("输入要发送的消息 (type 'quit' to exit): ");
        InputStreamReader converter = new InputStreamReader(System.in);

        BufferedReader in = new BufferedReader(converter);

        while (!(CurLine.equals("quit"))) {
            CurLine = in.readLine();

            if (!(CurLine.equals("quit"))) {
                PhoenixContext.getInstance().setRequestId("test-request-id:" + CurLine);
                PhoenixContext.getInstance().setReferRequestId("test-refer-request-id:" + CurLine);
                PhoenixContext.getInstance().setGuid("test-guid:" + CurLine);

                System.out.println("您发送的是: " + CurLine);
                try {
                    p.sendMessage(CurLine);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                System.exit(0);
            }
        }
    }

}
