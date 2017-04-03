package me.zhehua.firerooster.pipeline;

import java.lang.reflect.InvocationTargetException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by Zhehua on 2017/3/20.
 */
public class Pipeline implements Runnable {
    protected ExecutorService exeService;
    protected List<Task> taskList;
    protected BlockingQueue<Message> messageQueue;

    Pipeline(int threadNum, int msgQueueCap) {
        this(msgQueueCap);
        exeService = Executors.newFixedThreadPool(threadNum);
    }

    Pipeline(int msgQueueCap) {
        messageQueue = new LinkedBlockingDeque<>(msgQueueCap);
        taskList = new LinkedList<>();
    }

    void setThreadNum(int num) {
        exeService = Executors.newFixedThreadPool(num);
    }

    BlockingQueue<Message>[] queueBetweenTasks;

    @Override
    public void run() {
        while (!taskList.isEmpty()) {
            try {
                Message msg = messageQueue.take();
                switch (msg.type()) {
                    case Message.EXCEPTION_TASK:
                        break;
                    case Message.STOP_TASK:
                        Task src = msg.getSourceTask();
                        src.isStop = true;
                        taskList.remove(src);
                        break;
                    case Message.PROCESS:
                        break;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Prepare to shutdown");
        exeService.shutdown();
    }

    /**
     * Make sure thread pool is set before start().
     * Call {@link #Pipeline(int, int)} or {@link #setThreadNum(int)} to initialize thread pool.
     * @return The thread pipeline runs on.
     */
    public Thread start() {
        Thread masterThread = new Thread(this);
        masterThread.start();
        if (exeService != null) {
            for (Task task : taskList) {
                exeService.submit(task);
            }
            //taskList.forEach((task) -> exeService.submit(task));
        } else {
            throw new IllegalStateException("No thread pool set, call setThreadNum() first.");
        }
        return masterThread;
    }

    public void stop() {

    }

    public static class Builder {
        Pipeline pipeline;
        TreeMap<Integer, Map.Entry<Class<? extends Task>, Integer>> taskMap;
        TreeMap<Integer, Object> sharedObjs;
        Map<Integer, Integer> incomingQueueCapacities;
        int blockingQueueCapacity = 3;
        int messageQueueCapacity = 3;

        public Builder() {
            taskMap = new TreeMap<>();
            sharedObjs = new TreeMap<>();
            incomingQueueCapacities = new HashMap<>();
        }

        public Builder addTaskGroup(Class<? extends Task> taskClass, int count, int groupSeq) {
            if (taskMap.containsKey(groupSeq)) {
                Map.Entry<Class<? extends Task>, Integer> entry = taskMap.get(groupSeq);
                entry.setValue(entry.getValue() + count);
            } else {
                taskMap.put(groupSeq, new AbstractMap.SimpleEntry<Class<? extends Task>, Integer>(taskClass, count));
            }
            return this;
        }

        public Builder addTaskGroup(Class<? extends Task> taskClass, int count) {
            return addTaskGroup(taskClass, count, taskMap.lastEntry() == null ? 0 : taskMap.lastKey() + 1);
        }

        public Builder addTask(Class<? extends Task> taskClass) {
            return addTaskGroup(taskClass, 1);
        }

        public Builder addTask(Class<? extends Task> taskClass, int groupSeq) {
            return addTaskGroup(taskClass, 1, groupSeq);
        }

        public Builder setBlockingQueueCapacity(int capacity) {
            this.blockingQueueCapacity = capacity;
            return this;
        }

        public Builder setMessageQueueCapacity(int capacity) {
            this.messageQueueCapacity = capacity;
            return this;
        }

        /**
         * Incoming queue of task group in {@param seq}.
         * {@link InTask} has no incoming queue.
         * @param seq Group sequence of task group to set.
         * @param capacity Capacity set to incoming queue.
         * @return Builder
         */
        public Builder setIncomingQueueCapacity(int seq, int capacity) {
            this.incomingQueueCapacities.put(seq, capacity);
            return this;
        }

        public Builder addSharedObj(int seq, Object object) {
            sharedObjs.put(seq, object);
            return this;
        }

        public void initTask(int seq,
                             Class<? extends Task> taskClass,
                             int count,
                             BlockingQueue<Message> incomingQueue,
                             BlockingQueue<Message> outgoingQueue) {
            CountDownLatch counter = new CountDownLatch(count);
            try {
                for (int i = 0; i < count; i++) {
                    Task task = taskClass.getConstructor().newInstance();
                    task.setTaskName(taskClass.getSimpleName());
                    task.setCounter(counter);
                    task.setIncomingQueue(incomingQueue);
                    task.setOutgoingQueue(outgoingQueue);
                    task.setSharedObj(sharedObjs.get(seq));
                    task.setMessageQueue(pipeline.messageQueue);
                    pipeline.taskList.add(task);
                }
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        public Pipeline build() {
            if (taskMap.size() > 0) {
                pipeline = new Pipeline(messageQueueCapacity);
                int groupOrder = -1;
                pipeline.queueBetweenTasks = new BlockingQueue[taskMap.size() - 1];
                for (Map.Entry<Integer, Map.Entry<Class<? extends Task>, Integer>> entry : taskMap.entrySet()) {
                    int groupSeq = entry.getKey();
                    initTask(groupSeq,
                            entry.getValue().getKey(),
                            entry.getValue().getValue(),
                            // take last task's outgoing as incoming
                            groupOrder == -1 ? null :
                                    pipeline.queueBetweenTasks[groupOrder],
                            // a new queue as outgoing
                            // if capacity not set, use default
                            ++groupOrder == taskMap.size() - 1 ? null :
                                    (pipeline.queueBetweenTasks[groupOrder]
                                            = new LinkedBlockingDeque<>(incomingQueueCapacities.get(groupSeq) == null
                                            ? blockingQueueCapacity : incomingQueueCapacities.get(groupSeq))));

                }
                pipeline.setThreadNum(pipeline.taskList.size());
            }
            taskMap.clear();
            sharedObjs.clear();
            return pipeline;
        }
    }
}
