package me.zhehua.firerooster;

import android.graphics.Matrix;
import android.util.Log;

import org.opencv.core.Mat;

import java.util.List;

import me.zhehua.firerooster.pipeline.Message;
import me.zhehua.firerooster.pipeline.OutTask;


/**
 * Created by zhehua on 03/04/2017.
 */

public class DisplayThread extends OutTask {
    private final static String TAG = "DisplayThread";

    @Override
    public Message process(Message inputMessage) {
        Log.i(TAG, "input message");
        CameraBridgeViewBase displayView = (CameraBridgeViewBase) sharedObj;
        Mat[] bundledFrames = (Mat[]) inputMessage.obj;
        List<Matrix> transformMats = (List<Matrix>) inputMessage.extra;
        int i = 0;
        for (Mat frame: bundledFrames) {
            //displayView.deliverAndDrawFrame(frame, matrix);
            if (transformMats != null) {
                displayView.deliverAndDrawFrame(frame, transformMats.get(i++));
            } else {
                displayView.deliverAndDrawFrame(frame, null);
            }
            try {
                //frame.release();
                Thread.sleep(34);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
