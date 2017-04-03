package me.zhehua.firerooster;

import org.opencv.core.Mat;

import me.zhehua.firerooster.pipeline.Message;
import me.zhehua.firerooster.pipeline.OutTask;


/**
 * Created by zhehua on 03/04/2017.
 */

public class DisplayThread extends OutTask {

    @Override
    public Message process(Message inputMessage) {
        CameraBridgeViewBase displayView = (CameraBridgeViewBase) sharedObj;
        Mat[] bundledFrames = (Mat[]) inputMessage.obj;
        for (Mat frame: bundledFrames) {
            displayView.deliverAndDrawFrame(frame);
            try {
                Thread.sleep(34);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
