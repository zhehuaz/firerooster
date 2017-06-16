package me.zhehua.firerooster;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;

import org.opencv.BuildConfig;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import static android.content.ContentValues.TAG;

/**
 * Created by zhehua on 12/05/2017.
 */

public class PreviewSurfaceView extends SurfaceView {
    protected float mScale = 1f;
    Bitmap mCacheBitmap;
    public PreviewSurfaceView(Context context) {
        super(context);
    }

    public PreviewSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PreviewSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected void deliverAndDrawFrame(Mat modified) {
        this.deliverAndDrawFrame(modified, null);
    }

    /**
     * This method shall be called by the subclasses when they have valid
     * object and want it to be delivered to external client (via callback) and
     * then displayed on the screen.
     * @param modified - the current frame to be delivered
     */
    protected void deliverAndDrawFrame(Mat modified, Matrix transformMat) {

//        boolean bmpValid = true;
//        if (modified != null) {
//            try {
//                Utils.matToBitmap(modified, mCacheBitmap);
//            } catch(Exception e) {
//                Log.e(TAG, "Mat type: " + modified);
//                Log.e(TAG, "Bitmap type: " + mCacheBitmap.getWidth() + "*" + mCacheBitmap.getHeight());
//                Log.e(TAG, "Utils.matToBitmap() throws an exception: " + e.getMessage());
//                bmpValid = false;
//            }
//        }

        if (mCacheBitmap != null) {
            Canvas canvas = getHolder().lockCanvas();
            if (canvas != null) {
                canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "mStretch value: " + mScale);

                canvas.save();
                if (transformMat == null) {
                    transformMat = new Matrix();
                }
//                    Matrix matrix = new Matrix();
//                    float rotateDegree = (float) transformMat.get(0, 0)[0];
//                    float shiftX = (float) transformMat.get(1, 0)[0];
//                    float shiftY = (float) transformMat.get(2, 0)[0];
//                    float rotateCenterX = (float) transformMat.get(3, 0)[0];
//                    float rotateCenterY = (float) transformMat.get(4, 0)[0];
//                    matrix.setRotate(rotateDegree, rotateCenterX + (canvas.getWidth() - mCacheBitmap.getWidth()) / 2,
//                        rotateCenterY + (canvas.getHeight() - mCacheBitmap.getHeight()) / 2);
//                    matrix.setTranslate(shiftX, shiftY);

                transformMat.preRotate(90, mCacheBitmap.getWidth() / 2, mCacheBitmap.getHeight() / 2);
                canvas.setMatrix(transformMat);

                Log.d(TAG, "transformMat value: ");
                Log.d(TAG, transformMat.toShortString());
                //transformMat.postTranslate(10, 0);
                //transformMat.preTranslate(-(canvas.getWidth() - mScale*mCacheBitmap.getWidth()) / 2,
                //      -(canvas.getHeight() - mScale*mCacheBitmap.getHeight()) / 2);
                // TODO transform coordinate
                Log.d(TAG, transformMat.toShortString());

                Paint paint = new Paint();
                paint.setColor(Color.GREEN);
                paint.setStrokeWidth(10);
                paint.setAntiAlias(true);
//                if (mScale != 0) {
//                    canvas.drawBitmap(mCacheBitmap, new Rect(0,0,mCacheBitmap.getWidth(), mCacheBitmap.getHeight()),
//                         new Rect((int)((canvas.getWidth() - mScale*mCacheBitmap.getWidth()) / 2),
//                         (int)((canvas.getHeight() - mScale*mCacheBitmap.getHeight()) / 2),
//                         (int)((canvas.getWidth() - mScale*mCacheBitmap.getWidth()) / 2 + mScale*mCacheBitmap.getWidth()),
//                         (int)((canvas.getHeight() - mScale*mCacheBitmap.getHeight()) / 2 + mScale*mCacheBitmap.getHeight())), null);
//                } else {
//                     canvas.drawBitmap(mCacheBitmap, new Rect(0,0,mCacheBitmap.getWidth(), mCacheBitmap.getHeight()),
//                         new Rect((canvas.getWidth() - mCacheBitmap.getWidth()) / 2,
//                         (canvas.getHeight() - mCacheBitmap.getHeight()) / 2,
//                         (canvas.getWidth() - mCacheBitmap.getWidth()) / 2 + mCacheBitmap.getWidth(),
//                         (canvas.getHeight() - mCacheBitmap.getHeight()) / 2 + mCacheBitmap.getHeight()), null);
//                }
                if (mScale != 0) {
                    canvas.drawBitmap(mCacheBitmap, new Rect(0,0,mCacheBitmap.getWidth(), mCacheBitmap.getHeight()),
                            new Rect((int)((canvas.getWidth() - mScale*mCacheBitmap.getWidth()) / 2),
                                    (int)((canvas.getHeight() - mScale*mCacheBitmap.getHeight()) / 2),
                                    (int)((canvas.getWidth() - mScale*mCacheBitmap.getWidth()) / 2 + mScale*mCacheBitmap.getWidth()),
                                    (int)((canvas.getHeight() - mScale*mCacheBitmap.getHeight()) / 2 + mScale*mCacheBitmap.getHeight())), null);
                } else {
                    canvas.drawBitmap(mCacheBitmap, new Rect(0,0,mCacheBitmap.getWidth(), mCacheBitmap.getHeight()),
                            new Rect(0,
                                    0,
                                    mCacheBitmap.getWidth(),
                                    mCacheBitmap.getHeight()), null);
                }
//                canvas.drawLine((canvas.getWidth() - mCacheBitmap.getWidth()) / 2,
//                        (canvas.getHeight() - mCacheBitmap.getHeight()) / 2,
//                        (canvas.getWidth() - mCacheBitmap.getWidth()) / 2 + mCacheBitmap.getWidth(),
//                        (canvas.getHeight() - mCacheBitmap.getHeight()) / 2, paint);
//                canvas.drawLine((canvas.getWidth() - mCacheBitmap.getWidth()) / 2,
//                        (canvas.getHeight() - mCacheBitmap.getHeight()) / 2,
//                        (canvas.getWidth() - mCacheBitmap.getWidth()) / 2,
//                        (canvas.getHeight() - mCacheBitmap.getHeight()) / 2 + mCacheBitmap.getHeight(), paint);

                //canvas.drawOval(new RectF(-7, -7, 7, 7), paint);
//                if (mFpsMeter != null) {
//                    mFpsMeter.measure();
//                    mFpsMeter.draw(canvas, 20, 30);
//                }
                canvas.restore();
                getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }
}
