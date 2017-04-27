package me.zhehua.firerooster;

import android.graphics.Matrix;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import me.zhehua.firerooster.pipeline.Message;
import me.zhehua.firerooster.pipeline.ProcessTask;

/**
 * Created by zhehua on 13/04/2017.
 */

public class MotionCompensationTask extends ProcessTask {
    private static final String TAG = "MontionCompensationTask";
    boolean isShakedDetect = true;
    public static final double TRANSLATEMPLITUTE = 0.4d;
    public static final double ROTATEEAMPITUTE = 0.02;

    boolean computeAffine(List<List<Point>> trj, List<Point> avgFeatPos, List<Mat> affineMat) {
        Log.i(TAG, "trj size: " + trj.size());
        if (trj.size() < 3)
            return false;

        int trjNum = trj.size();
        int trjLength = trj.get(0).size();
        for (int i = 0; i < trjLength; i ++) {
            Point avgPoint = new Point(0, 0);
            for (List<Point> points : trj) {
                avgPoint.x = points.get(i).x;
                avgPoint.y = points.get(i).y;
            }
            avgPoint.x /= trjNum;
            avgPoint.y /= trjNum;
            avgFeatPos.add(avgPoint);
        }

        List<MatOfPoint> normalTrj = new ArrayList<>();
        for (int i = 0; i < trjLength; i ++) {
            Point[] tmp = new Point[trj.size()];
            int j = 0;
            for (List<Point> points: trj) {
                points.get(i).x -= avgFeatPos.get(i).x;
                points.get(i).y -= avgFeatPos.get(i).y;
                tmp[j ++] = points.get(i);
            }
            normalTrj.add(new MatOfPoint(tmp));
        }

        Mat affine;
        for (int i = 0; i < avgFeatPos.size() - 1; i ++) {
            affine = Video.estimateRigidTransform(normalTrj.get(i), normalTrj.get(0), false);
            affineMat.add(affine);
        }

        affine = Video.estimateRigidTransform(normalTrj.get(0), normalTrj.get(avgFeatPos.size() - 1), false);
        affineMat.add(affine);
        return true;
    }

    private Point minusPoint(Point a, Point b) {
        return new Point(a.x - b.x, a.y - b.y);
    }

    @Override
    public Message process(Message inputMessage) {
        Log.i(TAG, "input message");
        List<Point> avgFeatsPos = new ArrayList<>();
        List<Mat> affineMatrix  = new ArrayList<>();

        Mat[] frameBundle = (Mat[]) inputMessage.obj;

        if (inputMessage.extra == null
                || !computeAffine((List<List<Point>>) inputMessage.extra, avgFeatsPos, affineMatrix)) {
            return inputMessage;
        }

        List<Double> thetaVec = new ArrayList<>();
        for (int i = 0; i < affineMatrix.size(); i ++) {
            if (!affineMatrix.isEmpty()) {
                Mat affine, W = new Mat(), U = new Mat(), VT = new Mat();
                double theta = 0;
                if (!affineMatrix.get(i).empty()) {
                    affine = affineMatrix.get(i).colRange(0, 2);
                    Core.SVDecomp(affine, W, U, VT, Core.SVD_FULL_UV);
                    affine = U.mul(VT);
                    theta = Math.asin(affine.get(0, 1)[0]);
                }
                thetaVec.add(theta);
            }
        }

        boolean isStable = false;
        if (isShakedDetect) {
            if (avgFeatsPos.size() > 0) {
                double consinLim = 0.996;
                List<Double> shiftSd = new LinkedList<>();
                for (int i = 1; i < avgFeatsPos.size() - 1; i ++) {
                    Point d1, d2;
                    d1 = minusPoint(avgFeatsPos.get(i), avgFeatsPos.get(i - 1));
                    d2 = minusPoint(avgFeatsPos.get(i + 1), avgFeatsPos.get(i));
                    double d1Mu, d2Mu;
                    d1Mu = Math.sqrt(d1.x * d1.x + d1.y * d1.y);
                    d2Mu = Math.sqrt(d2.x * d2.x + d2.y * d2.y);

                    double yuxian = (d1.x * d1.x + d1.y * d1.y) / (d1Mu * d2Mu);
                    if (yuxian > consinLim) {
                        shiftSd.add(0d);
                    } else {
                        Point d = minusPoint(d2, d1);
                        d.x /= 2.0;
                        d.y /= 2.0;
                        double dMu;
                        dMu = Math.sqrt(d.x * d.x + d.y * d.y);
                        shiftSd.add(dMu);
                    }
                }
                double avgShiftSd = 0;
                for (int sd = 0; sd < shiftSd. size(); sd ++) {
                    avgShiftSd += shiftSd.get(sd);
                }
                avgShiftSd /= shiftSd.size();
                double avgRotateSd = 0;
                if (thetaVec.size() == CameraPreviewGrabber.SEGSIZE - 1) {
                    List<Double> tmpRotate = new ArrayList<>();
                    for (int i = 0; i < thetaVec.size(); i ++) {
                        if (i == 0) {
                            tmpRotate.add(-thetaVec.get(i));
                        } else {
                            if (i == thetaVec.size() - 1) {
                                tmpRotate.add(thetaVec.get(i) + thetaVec.get(i - 1));
                            } else {
                                tmpRotate.add(-thetaVec.get(i) + thetaVec.get(i - 1));
                            }
                        }

                    }
                    List<Double> rotateSd = new ArrayList<>();
                    for (int i = 0; i < tmpRotate.size() - 1; i ++) {
                        if ((tmpRotate.get(i) > 0 && tmpRotate.get(i + 1) > 0) ||
                                (tmpRotate.get(i) < 0 && tmpRotate.get(i + 1) < 0)) {
                            rotateSd.add(0d);
                        } else {
                            rotateSd.add(Math.abs(tmpRotate.get(i) - tmpRotate.get(i + 1)));
                        }
                    }
                    avgRotateSd = 0;
                    for (int sd = 0; sd < rotateSd.size(); sd ++) {
                        avgRotateSd += rotateSd.get(sd);
                    }
                    avgRotateSd /= rotateSd.size();
                }
                if (avgShiftSd < TRANSLATEMPLITUTE) {
                    isStable = true;
                }
                if (thetaVec.size() == CameraPreviewGrabber.SEGSIZE - 1) {
                    if (avgRotateSd > ROTATEEAMPITUTE) {
                        isStable = false;
                    }
                }
            }
        }

        //Mat tmp = Mat.zeros(2, 3, CvType.CV_64FC1);
        //tmp.put(0, 0, 1);
        //tmp.put(1, 1, 1);
        Matrix firstMatrix = new Matrix();
        //firstMatrix.setValues(new float[] {0, 0, 1, 1, 1, 1, 0, 0, 1});
        List<Matrix> stableTransformVec = new ArrayList<>();
        stableTransformVec.add(firstMatrix);

        double s2eTheta = 0;
        if (affineMatrix.size() != 0) {
            if (!affineMatrix.get(affineMatrix.size() - 1).empty()) {
                s2eTheta = thetaVec.get(thetaVec.size() - 1);
            }
        }

        for (int m = 0; m < frameBundle.length - 2; m ++) {
            Point shift;
            Mat affine;
            if (affineMatrix.size() == 0) {
                shift = new Point(0, 0);
                affine = Mat.zeros(2, 3, CvType.CV_64FC1);
                affine.put(0, 0, 1);
                affine.put(1, 1, 1);
            } else {
                Point tmpPoint;
                tmpPoint = minusPoint(avgFeatsPos.get(avgFeatsPos.size() - 1), avgFeatsPos.get(0));
                // TODO is this supposed to be m + 1 here?
                tmpPoint.x = (m + 1) * tmpPoint.x / (avgFeatsPos.size() - 1);
                tmpPoint.y = (m + 1) * tmpPoint.y / (avgFeatsPos.size() - 1);

                shift = minusPoint(avgFeatsPos.get(0), avgFeatsPos.get(m + 1));
                shift.x += tmpPoint.x;
                shift.y += tmpPoint.y;

                double degree = 0;
                if (!affineMatrix.get(affineMatrix.size() - 1).empty() && !affineMatrix.get(m).empty()) {
                    double deltaTheta;
                    deltaTheta = (m + 1) * s2eTheta / (avgFeatsPos.size() - 1);
                    double theta;
                    theta = thetaVec.get(m);
                    degree = theta + deltaTheta;
                }

                if (isShakedDetect && isStable) {
                    shift.x = 0;
                    shift.y = 0;
                    degree = 0;
                }

                // TODO crop control
                degree = degree * 180 / Math.PI;
                shift.x += avgFeatsPos.get(m + 1).x;
                shift.y += avgFeatsPos.get(m + 1).y;
                affine = Imgproc.getRotationMatrix2D(shift, degree, 1);
            }

            Mat shiftAffine = Mat.zeros(3, 3, CvType.CV_64FC1);
            shiftAffine.put(0, 0, 1);
            shiftAffine.put(1, 1, 1);
            shiftAffine.put(0, 2, (float)shift.x);
            shiftAffine.put(1, 2, (float)shift.y);
            shiftAffine.put(2, 2, 1);

            Mat tmpAff = Mat.zeros(3, 3, CvType.CV_64FC1);
            double[] affineValues = new double[6];
            affine.get(0, 0, affineValues);
            tmpAff.put(0, 0, affineValues);
            tmpAff.put(2, 2, 1);

            double[] values = new double[9];
            Matrix resultAffine = new Matrix();
            tmpAff.mul(shiftAffine).get(0, 0, values);
            float[] valuesf = new float[9];
            for (int i = 0; i < values.length; i ++) {
                valuesf[i] = (float) values[i];
            }
            resultAffine.setValues(valuesf);
            stableTransformVec.add(resultAffine);
        }
        inputMessage.extra = stableTransformVec;
        return inputMessage;
    }
}
