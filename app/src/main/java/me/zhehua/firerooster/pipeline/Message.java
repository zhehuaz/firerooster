package me.zhehua.firerooster.pipeline;

/**
 * Created by Zhehua on 2017/3/20.
 */
public class Message {
    public static final int STOP_TASK = 0;
    public static final int EXCEPTION_TASK = 1;
    public static final int PROCESS = 2;

    private int messageNum = -1;
    private int messageType;

    public Object obj;
    public Object id;
    public Object extra;
    public int arg1;
    public int arg2;
    public Task sourceTask;

    public Task getSourceTask() {
        return sourceTask;
    }

    public void setSourceTask(Task sourceTask) {
        this.sourceTask = sourceTask;
    }

    public Message(Task sourceTask) {
        this.sourceTask = sourceTask;
    }

    public Message(int msgType, int msgNum) {
        this.messageType = msgType;
        this.messageNum = msgNum;
    }

    public Message(int msgType) {
        this.messageType = msgType;
    }

    public int number() {
        return messageNum;
    }

    public int type() {
        return messageType;
    }

    public void type(int t) {
        this.messageType = t;
    }
}
