package me.zhehua.firerooster;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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
    ImageView marginLeft, marginTop, marginRight, marginBottom;
    float cropRotation = 0.8f;
    //PreviewSurfaceView previewSurfaceView;
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
        //previewSurfaceView = (PreviewSurfaceView) findViewById(R.id.psv_pre);
        cameraView = (CameraBridgeViewBase) findViewById(R.id.cbv_pre);
        cameraView.setVisibility(View.VISIBLE);
        cameraView.setPreviewGrabber(cameraPreviewGrabber);
        cameraView.setCvCameraViewListener(this);
        //cameraView.setPreviewSurfaceView(previewSurfaceView);

        marginLeft = (ImageView) findViewById(R.id.iv_left);
        marginTop= (ImageView) findViewById(R.id.iv_top);
        marginRight = (ImageView) findViewById(R.id.iv_right);
        marginBottom = (ImageView) findViewById(R.id.iv_bottom);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        int viewHeight = cameraView.getHeight();
        int viewWidth = cameraView.getWidth();

        float rotio = (1 - cropRotation) / 2;
        ViewGroup.LayoutParams params = marginLeft.getLayoutParams();
        params.width = (int) (viewWidth * rotio);
        marginLeft.setLayoutParams(params);

        params = marginRight.getLayoutParams();
        params.width = (int) (viewWidth * rotio);
        marginRight.setLayoutParams(params);

        params = marginTop.getLayoutParams();
        params.height = (int) (viewHeight * rotio);
        marginTop.setLayoutParams(params);

        params = marginBottom.getLayoutParams();
        params.height = (int) (viewHeight * rotio);
        marginBottom.setLayoutParams(params);
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

