package me.zhehua.firerooster;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import me.zhehua.firerooster.pipeline.Message;
import me.zhehua.firerooster.pipeline.ProcessTask;

/**
 * Created by zhehua on 07/04/2017.
 */
public class KltTask extends ProcessTask{
    private static final String TAG = "KltTask";
    private  boolean outOfImg(double[] point, Size size) {
        return (point[0] <= 0 || point[1] <= 0 || point[0] > size.width || point[1] > size.height);
    }

    @Override
    public Message process(Message inputMessage) {
        List<List<Point>> featureTrajectory = new LinkedList<>();
        Mat preGray = new Mat(), curGray = new Mat();
        MatOfPoint preFeats = new MatOfPoint();
        MatOfPoint2f curFeats = new MatOfPoint2f();
        MatOfByte status = new MatOfByte();
        MatOfFloat err = new MatOfFloat();
        Mat keyFrame = (Mat) inputMessage.id;
        if (keyFrame == null) {
            return inputMessage;
        }
        Imgproc.cvtColor(keyFrame, preGray, Imgproc.COLOR_RGB2GRAY);

        for (int i = 0; i < preFeats.size().height; i ++) {
            double[] point = preFeats.get(i, 0);
            List<Point> tmp = new LinkedList<>();
            tmp.add(new Point(point[0], point[1]));
            featureTrajectory.add(tmp);
        }
        Imgproc.goodFeaturesToTrack(preGray, preFeats, 60, 0.1, 20);

        for (Mat curFrame : (Mat[])inputMessage.obj) {
            Imgproc.cvtColor(curFrame, curGray, Imgproc.COLOR_RGB2GRAY);
            if (preFeats.size().height == 0) // vector is empty
                break;
            Video.calcOpticalFlowPyrLK(preGray, curGray, new MatOfPoint2f(preFeats.toArray()), curFeats, status, err);

            // omit error match of features
            int ptCount = (int) status.size().height;

            Point[] pointList = new Point[ptCount];
            for (int i = 0; i < ptCount; i ++) {
                pointList[i] = new Point(preFeats.get(i, 0)[0], preFeats.get(i, 0)[1]);
            }
            MatOfPoint2f p1 = new MatOfPoint2f(pointList);
            for (int i = 0; i < ptCount; i ++) {
                pointList[i] = new Point(curFeats.get(i, 0)[0], curFeats.get(i, 0)[1]);
            }
            MatOfPoint2f p2 = new MatOfPoint2f(pointList);

            Mat ransacStatus = new Mat();
            Calib3d.findFundamentalMat(p1, p2, Calib3d.FM_RANSAC, 3.d, 0.99d, ransacStatus);

            for (int i = 0; i < ptCount; i ++) {
                if (ransacStatus.get(i, 0)[0] == 0) {
                    status.put(i, 0, 0);
                }
            }
            int j = 0;
            preFeats.release();
            preFeats = new MatOfPoint();
            List<Point> preFeatsArray = new ArrayList<>();
            for (int i = 0; i < featureTrajectory.size(); j ++) {
                if (status.get(i, 0)[0] == 0 || err.get(i, 0)[0] > 20 || outOfImg(curFeats.get(i, 0), curFrame.size())) {
                    featureTrajectory.remove(i);
                } else {
                    preFeatsArray.add(new Point(curFeats.get(j, 0)));
                    featureTrajectory.get(i).add(new Point(curFeats.get(j, 0)));
                    i ++;
                }
            }
            preFeats.fromList(preFeatsArray);

            if (featureTrajectory.size() == 0) {
                break;
            }
            preGray.release();
            preGray = curGray.clone();
        }
        inputMessage.extra = featureTrajectory;
        return inputMessage;
    }
}
