package me.zhehua.firerooster.pipeline;

/**
 * Created by zhehua on 25/03/2017.
 */
public abstract class OutTask extends Task {
    @Override
    public void run() {
        Thread.currentThread().setName("Write Thread");
        while (!isStop) {
            Message msg = incomingQueue.poll();
            if (msg == null) {
                try {
                    Thread.yield();
                    Thread.sleep(0, 100000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            } else if (msg.type() == Message.STOP_TASK) {
                processEndTaskMessage(msg);
            } else {
                process(msg);
            }
        }
    }
}
