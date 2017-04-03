package me.zhehua.firerooster.pipeline;

/**
 * Created by zhehua on 25/03/2017.
 */
public abstract class ProcessTask extends Task {
    @Override
    public void run() {
        Thread.currentThread().setName(getTaskName());
        while (!isStop) {
            Message msg = incomingQueue.poll();
            if (msg == null) {
                try {
                    Thread.yield();
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            } else if (msg.type() == Message.STOP_TASK) {
                processEndTaskMessage(msg);
            } else {
                msg = process(msg);
                if (msg != null)
                putMessageToOutgoingQueue(msg);
            }
        }
    }
}
