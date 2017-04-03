package me.zhehua.firerooster.pipeline;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

/**
 * Created by Zhehua on 2017/3/20.
 */
public abstract class Task implements Runnable {
    private String taskName;
    protected boolean isStop = false;
    BlockingQueue<Message> incomingQueue;
    BlockingQueue<Message> outgoingQueue;
    BlockingQueue<Message> messageQueue;

    volatile protected Object sharedObj;

    CountDownLatch counter = new CountDownLatch(1);
    Thread masterThread; // null

    public Task(String taskName) {
        setTaskName(taskName);
    }

    public Task() {
    }

    public Object getSharedObj() {
        return sharedObj;
    }

    public void setSharedObj(Object sharedObj) {
        this.sharedObj = sharedObj;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    //public void setMasterThread(Thread masterThread) {
    //    this.masterThread = masterThread;
    //}

    public void setMessageQueue(BlockingQueue<Message> messageQueue) {
        this.messageQueue = messageQueue;
    }

    public void setCounter(CountDownLatch counter) {
        this.counter = counter;
    }

    public abstract Message process(Message inputMessage);

    void putMessageToOutgoingQueue(Message msg) {
        msg.setSourceTask(this);
        while(!outgoingQueue.offer(msg)) { }
    }

    void processEndTaskMessage(Message msg) {
        Message masterMessage = new Message(this);
        masterMessage.type(Message.STOP_TASK);
        try {
            messageQueue.put(masterMessage);
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.err.println("end task message failed to send");
        }

        synchronized (counter) {
            counter.countDown();
            if (counter.getCount() > 0) {
                // this message shouldn't be received by who sent it
                while (!incomingQueue.offer(msg)) {

                }
                while (!isStop) {
                    // if not yield here, loop may occupy CPU permanently
                    Thread.yield();
                }
            } else {
                putMessageToOutgoingQueue(msg);
            }
        }
    }

    public void putMessageToMessageQueue(Message msg, boolean isBlock) {
        // TODO
    }

    public BlockingQueue<Message> getIncomingQueue() {
        return incomingQueue;
    }

    public void setIncomingQueue(BlockingQueue<Message> incomingQueue) {
        this.incomingQueue = incomingQueue;
    }

    public BlockingQueue<Message> getOutgoingQueue() {
        return outgoingQueue;
    }

    public void setOutgoingQueue(BlockingQueue<Message> outgoingQueue) {
        this.outgoingQueue = outgoingQueue;
    }
}
