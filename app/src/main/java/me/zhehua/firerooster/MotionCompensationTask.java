package me.zhehua.firerooster;

import android.graphics.Matrix;
import android.util.Log;
import android.util.MutableDouble;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import me.zhehua.firerooster.pipeline.Message;
import me.zhehua.firerooster.pipeline.ProcessTask;

import static java.lang.Math.abs;

/**
 * Created by zhehua on 13/04/2017.
 */

public class MotionCompensationTask extends ProcessTask {
    private static final String TAG = "MotionCompensationTask";
    boolean isShakedDetect = true;
    boolean cropControlFlag = false;
    float cropRotation = 0.8f;
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
                avgPoint.x += points.get(i).x;
                avgPoint.y += points.get(i).y;
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
        for (int i = 1; i < avgFeatPos.size() - 1; i ++) {
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

    private Point addPoint(Point a, Point b) {
        return new Point(a.x + b.x, a.y + b.y);
    }

    boolean cropControl(float cropRotation, Point center, Point shift, MutableDouble degree, Size videoSize) {
        List<Point> pt = new ArrayList<>();
        pt.add(new Point(0, 0));
        pt.add(new Point(0, videoSize.height - 1));
        pt.add(new Point(videoSize.width - 1, videoSize.height - 1));
        pt.add(new Point(videoSize.width - 1, 0));
        List<Point> ptCrop = new ArrayList<>();
        int xTip = (int) (videoSize.width * (1 - cropRotation) / 2);
        int yTip = (int) (videoSize.height * (1 - cropRotation) / 2);
        ptCrop.add(new Point(xTip, yTip));
        ptCrop.add(new Point(xTip, videoSize.height - yTip));
        ptCrop.add(new Point(videoSize.width - xTip, videoSize.height - yTip));
        ptCrop.add(new Point(videoSize.width - xTip, yTip));

        Mat shiftMat = Mat.eye(3, 3, CvType.CV_64FC1);
        shiftMat.put(0, 2, shift.x);
        shiftMat.put(1, 2, shift.y);
        double angle = degree.value * 180 / 3.1415926;
        Mat tmpRotateMat = Imgproc.getRotationMatrix2D( addPoint(center, shift), angle, 1);
        Mat rotateMat = Mat.zeros(3,3,CvType.CV_64FC1);
        rotateMat.put(0, 0, tmpRotateMat.get(0, 0)); // TODO copy mat
        rotateMat.put(2, 2, 1);
        Mat affine = new Mat(3, 3, CvType.CV_64FC1);
        Core.gemm(rotateMat, shiftMat, 1, new Mat(), 0, affine);
        affine = affine.rowRange(0,2);

        if( isInsideAfterTrans( affine , ptCrop , pt ) )//经过平移旋转后依然包括裁剪窗口
        {
            return true;
        }
        else
        {
            if( xTip < abs(shift.x) || yTip < abs(shift.y) )//经过平移后不包括裁剪窗口
            {
                double ratio1 = xTip / abs(shift.x);
                double ratio2 = yTip / abs(shift.y);
                if( ratio1 < ratio2 )
                {
                    if( shift.x > 0 )
                    {
                        shift.x = xTip;
                    }
                    else
                    {
                        shift.x = -xTip;
                    }
                    shift.y = ratio1 * shift.y;
                }
                else
                {
                    shift.x = ratio2 * shift.x;
                    if( shift.y > 0 )
                    {
                        shift.y = yTip;
                    }
                    else
                    {
                        shift.y = -yTip;
                    }
                }
                degree.value = 0d;
            }
            else//经过平移后包括裁剪窗口
            {
				/*计算最大旋转角*/
                List<Point> new_crop_pt = new ArrayList<>();
                for( int i = 0 ; i < 4 ; i++ )
                {
                    new_crop_pt.add(minusPoint(ptCrop.get(i), shift));
                }

                double[] maxDegree = new double[4];
                List<Point> img_line = new ArrayList<>();
                List<Point> crop_line = new ArrayList<>();
                img_line.add( new Point(0,0) );
                img_line.add( new Point(videoSize.width-1,0) );
                crop_line.add( new_crop_pt.get(0) );
                crop_line.add( new_crop_pt.get(3) );
                maxDegree[0] = computeMaxDegree( img_line , crop_line , degree.value , center );

                img_line.set(1, new Point(videoSize.height-1,0));
                crop_line.set(0, new Point(videoSize.height-1-new_crop_pt.get(1).y , new_crop_pt.get(1).x));
                crop_line.set(1, new Point(videoSize.height-1-new_crop_pt.get(0).y , new_crop_pt.get(0).x));
                Point newCenter = new Point();
                newCenter.x = videoSize.height-1-center.y;
                newCenter.y = center.x;
                maxDegree[1] = computeMaxDegree( img_line , crop_line , degree.value , newCenter );

                img_line.set(1, new Point(videoSize.width-1,0));
                crop_line.set(0, new Point(videoSize.width-1-new_crop_pt.get(2).x , videoSize.height-1-new_crop_pt.get(2).y));
                crop_line.set(1, new Point(videoSize.width-1-new_crop_pt.get(1).x , videoSize.height-1-new_crop_pt.get(1).y));
                newCenter.x = videoSize.width-1-center.x;
                newCenter.y = videoSize.height-1-center.y;
                maxDegree[2] = computeMaxDegree( img_line , crop_line , degree.value , newCenter );

                img_line.set(1, new Point(videoSize.height-1,0));
                crop_line.set(0, new Point(new_crop_pt.get(3).y , videoSize.width-1-new_crop_pt.get(3).x));
                crop_line.set(1, new Point(new_crop_pt.get(2).y , videoSize.width-1-new_crop_pt.get(2).x));
                newCenter.x = center.y;
                newCenter.y = videoSize.width-1-center.x;
                maxDegree[3] = computeMaxDegree( img_line , crop_line , degree.value , newCenter );

                if( degree.value > 0 )
                {
                    double min = degree.value;
                    for( int i = 0 ; i < 4 ; i++ )
                    {
                        if( min > maxDegree[i] )
                        {
                            min = maxDegree[i];
                        }
                    }
                    degree.value = min;
                }
                else
                {
                    double max = degree.value;
                    for( int i = 0 ; i < 4 ; i++ )
                    {
                        if( max < maxDegree[i] )
                        {
                            max = maxDegree[i];
                        }
                    }
                    degree.value = max;
                }
				/**/
            }

            return false;
        }

    }

    boolean isInsideAfterTrans(Mat affine, List<Point> ptCrop, List<Point> pt) {
        List<Point> ptTransform = new ArrayList<>();
        for (int i = 0; i < 4; i ++) {
            Mat tmp = Mat.ones(3, 1, CvType.CV_64FC1);
            tmp.put(0, 0, pt.get(i).x);
            tmp.put(1, 0, pt.get(i).y);
            Mat pos = new Mat(3, 1, CvType.CV_64FC1);
            Core.gemm(affine, tmp, 1, new Mat(), 0, pos);
            ptTransform.add(new Point(pos.get(0, 0)[0], pos.get(1, 0)[0]));
        }
        boolean allInside = true;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                Point vec1, vec2;
                vec1 = minusPoint(ptTransform.get(j), ptCrop.get(i));
                vec2 = minusPoint(ptTransform.get((j + 1) % 4), ptTransform.get(j));
                double cross = vec1.x * vec2.y - vec2.x * vec1.y;
                if (cross > 0) {
                    allInside = false;
                    break;
                }
            }
            if (!allInside) {
                break;
            }
        }
        return allInside;
    }

    double computeMaxDegree( List<Point> img_line , List<Point> crop_line , double degree , Point center )
    {
        if( degree > 0 )
        {
            if( center.x <= crop_line.get(0).x )
            {
                return Math.PI;
            }
            else
            {
                double dis = Math.sqrt( Math.pow(crop_line.get(0).x - center.x, 2) + Math.pow(crop_line.get(0).y - center.y,2));
                if( dis <= center.y )
                {
                    return Math.PI;
                }
                else
                {
					/*计算切点*/
                    double a1 , a2 , a3;
                    a1 = center.x - crop_line.get(0).x;
                    a2 = center.y - crop_line.get(0).y;
                    a3 = center.x*crop_line.get(0).x - center.x * center.x + center.y*crop_line.get(0).y;
                    double k , n;
                    k = -a2 / a1;
                    n = -a3 / a1;
                    double a , b , c;
                    a = k*k + 1;
                    b = 2*k*n - 2*center.x*k - 2*center.y;
                    c = n*n - 2*center.x*n + center.x*center.x;
                    Point pointofContact = new Point();
                    double y1 , y2;
                    y1 = (-b + Math.sqrt(b*b - 4*a*c)) / (2*a);
                    y2 = (-b - Math.sqrt(b*b - 4*a*c)) / (2*a);
                    pointofContact.y = (y1<y2)?y1:y2;
                    pointofContact.x = k*pointofContact.y + n;
					/**/

                    Point vec1 , vec2;
                    vec1 = minusPoint(pointofContact,crop_line.get(0));
                    vec2 = minusPoint(crop_line.get(0), crop_line.get(0));
                    double cos_alpha = (vec1.x*vec2.x + vec1.y*vec2.y) / (Math.sqrt(vec1.x*vec1.x+vec1.y*vec1.y) * Math.sqrt(vec2.x*vec2.x+vec2.y*vec2.y));
                    double alpha = Math.acos(cos_alpha);

                    return alpha;
                }
            }
        }
        else
        {
            if( center.x >= crop_line.get(1).x )
            {
                return -3.1415926;
            }
            else
            {
                double dis = Math.sqrt( Math.pow(crop_line.get(1).x - center.x,2) + Math.pow(crop_line.get(1).y - center.y,2) );
                if( dis <= center.y )
                {
                    return -3.1415926;
                }
                else
                {
					/*计算切点*/
                    double a1 , a2 , a3;
                    a1 = center.x - crop_line.get(1).x;
                    a2 = center.y - crop_line.get(1).y;
                    a3 = center.x*crop_line.get(1).x - center.x*center.x + center.y*crop_line.get(1).y;
                    double k , n;
                    k = -a2 / a1;
                    n = -a3 / a1;
                    double a , b , c;
                    a = k*k + 1;
                    b = 2*k*n - 2*center.x*k - 2*center.y;
                    c = n*n - 2*center.x*n + center.x*center.x;
                    Point pointofContact = new Point();
                    double y1 , y2;
                    y1 = (-b + Math.sqrt(b*b - 4*a*c)) / (2*a);
                    y2 = (-b - Math.sqrt(b*b - 4*a*c)) / (2*a);
                    pointofContact.y = (y1<y2)?y1:y2;
                    pointofContact.x = k*pointofContact.y + n;
					/**/

                    Point vec1 , vec2;
                    vec1 = minusPoint(pointofContact, crop_line.get(1));
                    vec2 = minusPoint(crop_line.get(0), crop_line.get(1));
                    double cos_alpha = (vec1.x*vec2.x + vec1.y*vec2.y) / (Math.sqrt(vec1.x*vec1.x+vec1.y*vec1.y) * Math.sqrt(vec2.x*vec2.x+vec2.y*vec2.y));
                    double alpha = Math.acos(cos_alpha);

                    return -alpha;
                }
            }
        }
    }
    
    @Override
    public Message process(Message inputMessage) {
        Log.i(TAG, "input message");
        List<Point> avgFeatsPos = new ArrayList<>();
        List<Mat> affineMatrix  = new ArrayList<>();

        Mat[] frameBundle = (Mat[]) inputMessage.obj;

        if (inputMessage.extra == null
                || !computeAffine((List<List<Point>>) inputMessage.extra, avgFeatsPos, affineMatrix)) {
            inputMessage.extra = null;
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
                    Core.gemm(U, VT, 1, new Mat(), 0, affine);
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
                            rotateSd.add(abs(tmpRotate.get(i) - tmpRotate.get(i + 1)));
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

        List<Matrix> stableTransformVec = new ArrayList<>();
        //stableTransformVec.add(Mat.zeros(5, 1, CvType.CV_64FC1));
        stableTransformVec.add(new Matrix());

        double s2eTheta = 0;
        if (affineMatrix.size() != 0) {
            if (!affineMatrix.get(affineMatrix.size() - 1).empty()) {
                s2eTheta = thetaVec.get(thetaVec.size() - 1);
            }
        }

        for (int m = 0; m < frameBundle.length - 1; m ++) {
            Point shift;
            Mat affine;
            double degree = 0;
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

                if (!affineMatrix.get(affineMatrix.size() - 1).empty() && !affineMatrix.get(m + 1).empty()) {
                    double deltaTheta;
                    deltaTheta = (m + 1) * s2eTheta / (avgFeatsPos.size() - 1);
                    double theta;
                    theta = thetaVec.get(m);
                    degree = theta + deltaTheta;
                }

//                if (isShakedDetect && isStable) {
//                    shift.x = 0;
//                    shift.y = 0;
//                    degree = 0;
//                }

                MutableDouble degreeWrap = new MutableDouble(degree);
                if (cropControlFlag) {
                    cropControl(cropRotation, avgFeatsPos.get(m + 1), shift, degreeWrap, frameBundle[0].size());
                }
                degree = degreeWrap.value * 180 / Math.PI;
                affine = Imgproc.getRotationMatrix2D(addPoint(shift, avgFeatsPos.get(m + 1)), degree, 1);
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
            Mat mulRes = new Mat(3, 3, CvType.CV_64FC1);
            Core.gemm(tmpAff, shiftAffine, 1, new Mat(), 0, mulRes);
            mulRes.get(0, 0, values);
            float[] valuesf = new float[9];
            for (int i = 0; i < values.length; i ++) {
                valuesf[i] = (float) values[i];
            }
            resultAffine.setValues(valuesf);

//            Mat transMat = Mat.zeros(5, 1, CvType.CV_64FC1);
//            transMat.put(0, 0, degree);
//            transMat.put(1, 0, shift.x);
//            transMat.put(2, 0, shift.y);
//            transMat.put(3, 0, shift.x + avgFeatsPos.get(m + 1).x);
//            transMat.put(4, 0, shift.y + avgFeatsPos.get(m + 1).y);

            stableTransformVec.add(resultAffine);
        }
        //stableTransformVec.add(Mat.zeros(5, 1, CvType.CV_64FC1));
        //stableTransformVec.add(new Matrix());
        inputMessage.extra = stableTransformVec;
        return inputMessage;
    }
}
