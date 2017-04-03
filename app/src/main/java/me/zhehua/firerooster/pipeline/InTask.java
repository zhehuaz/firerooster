package me.zhehua.firerooster.pipeline;

/**
 * Created by zhehua on 25/03/2017.
 */
public abstract class InTask extends Task {

    @Override
    public void run() {
        Thread.currentThread().setName("Read Thread");
        while(!isStop) {
            Message newMsg = process(null);
            // if read over, process should return null
            if (newMsg != null) {
                putMessageToOutgoingQueue(newMsg);
            }
        }
    }
}

