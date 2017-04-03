package me.zhehua.firerooster.pipeline;

/**
 * Created by Zhehua on 2017/3/21.
 */
public class ReadTask extends InTask {

    @Override
    public Message process(Message inputMessage) {
        if (((Counter) sharedObj).num >= 300) {
            Message endMsg = new Message(Message.STOP_TASK);
            endMsg.setSourceTask(this);
            processEndTaskMessage(endMsg);
            return null;
        }
        long startTime = System.nanoTime();
        while (System.nanoTime() - startTime < 200 * 1000 * 1000) {

        }

        Message msg;
        synchronized(sharedObj) {
            msg = new Message(Message.PROCESS, ((Counter) sharedObj).num);
            ((Counter) sharedObj).num += 1;
        }
        return msg;
    }
}
