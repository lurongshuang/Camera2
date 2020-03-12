package com.lrs.camera2.camera;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.FaceDetector;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.lrs.camera2.MessageEvent;
import com.lrs.camera2.R;
import com.lrs.camera2.RoundTextureView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class CameraActivity extends AppCompatActivity implements ViewTreeObserver.OnGlobalLayoutListener, CameraListener {
    private static final String TAG = "CameraActivity";
    //摄像头 整合
    private CameraHelper cameraHelper;
    //摄像头
    private RoundTextureView textureView;
    //圆  显示是被状态
    private RoundBorderView roundBorderView;
    //默认打开的CAMERA         CAMERA_FACING_FRONT 前      CAMERA_FACING_BACK 后
    private static final int CAMERA_ID = Camera.CameraInfo.CAMERA_FACING_FRONT;

    //获取人脸bitmap
    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private Type.Builder yuvType, rgbaType;
    private Allocation in, out;

    //展示识别成功后的 人脸
    private Bitmap bitmapSend;
    private ImageView ivimage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //         无title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //         全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main1);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        findViewById(R.id.ivclose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CameraActivity.this.finish();
            }
        });
        ivimage = findViewById(R.id.ivimage);
        EventBus.getDefault().register(this);
        //实例化摄像头
        initView();
        //实例化人脸识别
        rs = RenderScript.create(this);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
        initFace();
    }

    @Subscribe
    public void eventsbusMessage(MessageEvent event) {
    }

    private void initView() {
        textureView = findViewById(R.id.texture_preview);
        textureView.getViewTreeObserver().addOnGlobalLayoutListener(this);
        textureView.setRadius(250);
    }


    void initCamera() {
        cameraHelper = new CameraHelper.Builder()
                .cameraListener(this)
                .specificCameraId(CAMERA_ID)
                .previewOn(textureView)
                .previewViewSize(new Point(textureView.getLayoutParams().width, textureView.getLayoutParams().height))
                .rotation(getWindowManager().getDefaultDisplay().getRotation())
                .build();
        cameraHelper.start();
    }


    @Override
    public void onGlobalLayout() {
        textureView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        ViewGroup.LayoutParams layoutParams = textureView.getLayoutParams();
        int sideLength = Math.min(textureView.getWidth(), textureView.getHeight()) * 3 / 4;
        layoutParams.width = sideLength;
        layoutParams.height = sideLength;
        textureView.setLayoutParams(layoutParams);
        textureView.turnRound();
        initCamera();
    }

    @Override
    protected void onPause() {
        if (cameraHelper != null) {
            cameraHelper.stop();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cameraHelper != null) {
            cameraHelper.start();
        }
    }

    private Camera.Size previewSize;

    @Override
    public void onCameraOpened(Camera camera, int cameraId, final int displayOrientation, boolean isMirror) {
        previewSize = camera.getParameters().getPreviewSize();
        Log.i(TAG, "onCameraOpened:  previewSize = " + previewSize.width + "x" + previewSize.height);
        //在相机打开时，添加右上角的view用于显示原始数据和预览数据
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //将预览控件和预览尺寸比例保持一致，避免拉伸
                {
                    ViewGroup.LayoutParams layoutParams = textureView.getLayoutParams();
                    //横屏
                    if (displayOrientation % 180 == 0) {
                        layoutParams.height = layoutParams.width * previewSize.height / previewSize.width;
                    }
                    //竖屏
                    else {
                        layoutParams.height = layoutParams.width * previewSize.width / previewSize.height;
                    }
                    textureView.setLayoutParams(layoutParams);
                }
                roundBorderView = new RoundBorderView(CameraActivity.this);
                roundBorderView.setRadius(textureView.getRadius());
                ((RelativeLayout) textureView.getParent()).addView(roundBorderView, textureView.getLayoutParams());
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            }
        });
    }

    private Handler mFaceHandle;
    private HandlerThread mFaceHandleThread;
    private long time;
    private Handler mHandler;
    private int mOrienta = 90, index;

    /**
     * 初始化 识别人脸
     */
    private void initFace() {
        //
        mFaceHandleThread = new HandlerThread("face");
        mFaceHandleThread.start();
        mFaceHandle = new Handler(mFaceHandleThread.getLooper());
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Bundle bun = msg.getData();
                switch (msg.what) {
                    case 1:
                        int type = bun.getInt("type");
                        if (type == 1) {
                            Log.e("mHandler", "识别到人脸");
                            roundBorderView.setColors(getResources().getColor(R.color.color_green));
                            roundBorderView.turnRound();
                            if (bitmapSend != null) {
                                ivimage.setImageBitmap(bitmapSend);
                            }
                            EventBus.getDefault().post(new MessageEvent(200, ""));
                            TimerTask task = new TimerTask() {
                                @Override
                                public void run() {
                                    CameraActivity.this.finish();
                                }
                            };
                            Timer timer = new Timer();
                            timer.schedule(task, 2000);
                        } else {
                            Log.e("mHandler", "没有识别到人脸");
                            roundBorderView.setColors(getResources().getColor(R.color.color_red));
                            roundBorderView.invalidate();
                        }
                        break;
                }
            }
        };
    }


    @Override
    public void onPreview(byte[] data, Camera camera) {
        //开始人脸识别
        if (data != null && data.length > 0 && System.currentTimeMillis() - time > 200) {
            time = System.currentTimeMillis();
            mFaceHandle.post(new FaceThread(data, camera, (++index)));
        }
    }

    @Override
    public void onCameraClosed() {
        Log.i(TAG, "onCameraClosed: ");
    }

    @Override
    public void onCameraError(Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onCameraConfigurationChanged(int cameraID, int displayOrientation) {

    }

    @Override
    protected void onDestroy() {
        if (cameraHelper != null) {
            cameraHelper.release();
        }
        if (mFaceHandleThread != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mFaceHandleThread.quitSafely();
            }
            try {
                mFaceHandleThread.join();
                mFaceHandleThread = null;
                mFaceHandle = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }

    /**
     * 旋转角度
     */
//    public void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {
//        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
//        android.hardware.Camera.getCameraInfo(cameraId, info);
//        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
//        int degrees = 0;
//        switch (rotation) {
//            case Surface.ROTATION_0:
//                degrees = 0;
//                break;
//            case Surface.ROTATION_90:
//                degrees = 90;
//                break;
//            case Surface.ROTATION_180:
//                degrees = 180;
//                break;
//            case Surface.ROTATION_270:
//                degrees = 270;
//                break;
//        }
//        int result;
//        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//            result = (info.orientation + degrees) % 360;
//            result = (360 - result) % 360;   // compensate the mirror
//        } else {
//            // back-facing
//            result = (info.orientation - degrees + 360) % 360;
//        }
//        mOrienta = result;
//        camera.setDisplayOrientation(result);
//    }

    /**
     * 识别人脸
     */
    private class FaceThread implements Runnable {
        private byte[] mData;
        private ByteArrayOutputStream mBitmapOutput;//mUploadOutp1ut
        private Matrix mMatrix;
        private Message mMessage;
        private Camera mtCamera;
        private int index;
        int degrees = 0;

        /**
         * 构造函数  得到 摄像头 和图像
         * @param data
         * @param camera
         * @param index
         */
        public FaceThread(byte[] data, Camera camera, int index) {
            mData = data;
            mBitmapOutput = new ByteArrayOutputStream();
            mMessage = mHandler.obtainMessage();
            mMatrix = new Matrix();
            switch (mOrienta) {
                case 90:
                    mMatrix.postRotate(270);
                    break;
                case 270:
                    mMatrix.postRotate(90);
                    break;
                default:
                    mMatrix.postRotate(mOrienta);
                    break;
            }
            // mMatrix.postScale(-1, 1);//水平
            mtCamera = camera;
            this.index = index;
        }

        @Override
        public void run() {
            Bitmap bitmap = null;
            Bitmap mFaceBitmap = null;
            int type = -1;
            try {
                Camera.Size size = mtCamera.getParameters().getPreviewSize();
                YuvImage yuvImage = new YuvImage(mData, ImageFormat.NV21, size.width, size.height, null);
                mData = null;
                yuvImage.compressToJpeg(new Rect(0, 0, size.width, size.height), 100, mBitmapOutput);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;//必须设置为565，否则无法检测
                // 转换成图片
                bitmap = BitmapFactory.decodeByteArray(mBitmapOutput.toByteArray(), 0, mBitmapOutput.toByteArray().length, options);
                bitmapSend = BitmapFactory.decodeByteArray(mBitmapOutput.toByteArray(), 0, mBitmapOutput.toByteArray().length, options);
                if (bitmap != null) {
                    mBitmapOutput.reset();
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mMatrix, false);
                    final Bitmap mBitmap = bitmap;
                    FaceDetector.Face[] faces = cameraHelper.findFaces(bitmap);

                    FaceDetector.Face facePostion = null;
                    int index = 0;
                    if (faces != null) {
                        for (FaceDetector.Face face : faces) {
                            if (face == null) {
                                bitmap.recycle();
                                bitmap = null;
                                mBitmapOutput.close();
                                mBitmapOutput = null;
//                                Log.e("face", "无人脸");
                                type = 0;
                                break;
                            } else {
//                                Log.e("face", "有人脸");
                                facePostion = face;
                                type = 1;
                                break;
                            }
                        }
                    }
                    Message message = mHandler.obtainMessage(1);
                    Bundle bun = new Bundle();
                    bun.putInt("type", type);
                    message.setData(bun);
                    mHandler.sendMessage(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (bitmap != null) {
                    bitmap.recycle();
                    bitmap = null;
                }
                if (mFaceBitmap != null) {
                    mFaceBitmap.recycle();
                    mFaceBitmap = null;
                }
                if (mBitmapOutput != null) {
                    try {
                        mBitmapOutput.close();
                        mBitmapOutput = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}