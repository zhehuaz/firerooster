package me.zhehua.firerooster;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{
    static {
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV init error!");
        }
    }

    CameraBridgeViewBase cameraView;
    CameraPreviewGrabber cameraPreviewGrabber;
    private final static String TAG = "MainActivity";

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    cameraView.enableView();
                break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        cameraView.enableView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraView != null) {
            cameraView.disableView();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cameraPreviewGrabber = new CameraPreviewGrabber(-1);
        cameraView = (CameraBridgeViewBase) findViewById(R.id.cbv_pre);
        cameraView.setVisibility(View.VISIBLE);
        cameraView.setmPreviewGrabber(cameraPreviewGrabber);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //Utils.matToBitmap(inputFrame.rgba(), bitmap);
        //runOnUiThread(new Runnable() {
        //    @Override
        //    public void run() {
        //        imageView.setImageBitmap(bitmap);
        //    }
        //});

        return inputFrame.rgba();
    }
}

