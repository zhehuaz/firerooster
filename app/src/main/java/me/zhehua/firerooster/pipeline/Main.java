package me.zhehua.firerooster.pipeline;

public class Main {

    public static void main(String[] args) {
        Pipeline pipeline = new Pipeline.Builder()
                .addTask(ReadTask.class)
                .addTaskGroup(KLTTask.class, 3)
                .addTask(CompensationTask.class)
                .addTask(WriteTask.class)
                .setMessageQueueCapacity(5)
                .addSharedObj(0, new Counter(0))
                .build();
        pipeline.start();
    }
}
