package me.zhehua.firerooster.pipeline;

/**
 * Created by Zhehua on 2017/3/21.
 */
public class KLTTask extends ProcessTask {
    @Override
    public Message process(Message inputMessage) {
        long startTime = System.nanoTime();
        while (System.nanoTime() - startTime < 500 * 1000 * 1000) {

        }
        return inputMessage;
    }
}
