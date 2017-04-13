package me.zhehua.firerooster;

import me.zhehua.firerooster.pipeline.Message;
import me.zhehua.firerooster.pipeline.ProcessTask;

/**
 * Created by zhehua on 13/04/2017.
 */

public class MotionCompensationTask extends ProcessTask {
    @Override
    public Message process(Message inputMessage) {
        long startTime = System.nanoTime();
        while (System.nanoTime() - startTime < 300 * 1000 * 1000) {

        }
        return inputMessage;
    }
}
