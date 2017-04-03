package me.zhehua.firerooster.pipeline;

/**
 * Created by Zhehua on 2017/3/21.
 */
public class WriteTask extends OutTask {

    @Override
    public Message process(Message inputMessage) {
        long startTime = System.nanoTime();
        while (System.nanoTime() - startTime < 200 * 1000 * 1000) { }
        System.out.println("Receive msg " + inputMessage.number());
        return inputMessage;
    }
}
