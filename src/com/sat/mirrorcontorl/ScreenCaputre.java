package com.sat.mirrorcontorl;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static android.media.MediaCodec.CONFIGURE_FLAG_ENCODE;
import static android.media.MediaFormat.KEY_BIT_RATE;
import static android.media.MediaFormat.KEY_FRAME_RATE;
import static android.media.MediaFormat.KEY_I_FRAME_INTERVAL;

public class ScreenCaputre {

    private static final String TAG = ScreenCaputre.class.getSimpleName();

    public static final int NAL_SLICE = 1;
    public static final int NAL_SLICE_DPA = 2;
    public static final int NAL_SLICE_DPB = 3;
    public static final int NAL_SLICE_DPC = 4;
    public static final int NAL_SLICE_IDR = 5;
    public static final int NAL_SEI = 6;
    public static final int NAL_SPS = 7;
    public static final int NAL_PPS = 8;
    public static final int NAL_AUD = 9;
    public static final int NAL_FILLER = 12;
    
//    private final static int FRAME_RATE = 25;
	private final static int FRAME_INTERVAL = 1;
//	private final static int FRAME_BIT_RATE = 614400;
	
	private final static int FRAME_RATE = 25;
	private final static int FRAME_BIT_RATE = 1000000;
//	private final static int FRAME_BIT_RATE = 921600;

	private static final String MIMETYPE_VIDEO_AVC = "video/avc";

    private MediaCodec.BufferInfo vBufferInfo = new MediaCodec.BufferInfo();
    private MediaCodec vEncoder;
    private Thread videoEncoderThread;
    private boolean videoEncoderLoop;
//    private long presentationTimeUs;


    public interface ScreenCaputreListener {
        void onImageData(byte[] buf);
    }

    private int width;
    private int height;
    private DisplayManager mDisplayManager;
    private VirtualDisplay mVirtualDisplay;

    private ScreenCaputreListener screenCaputreListener;

    public ScreenCaputre(int width, int height, DisplayManager mDisplayManager) {
        this.width = 1024;
        this.height = 600;

//        this.width = 720;
//        this.height = 1280;

//        this.width = 360;
//        this.height = 640;

//        this.width = width;
//        this.height = height;
//        int W = 640, H = W/9*16;
//        this.width = W;
//        this.height = H;
        this.mDisplayManager = mDisplayManager;
    }

    public void setScreenCaputreListener(ScreenCaputreListener screenCaputreListener) {
        this.screenCaputreListener = screenCaputreListener;
    }

    public void start() {
        try {
            prepareVideoEncoder();
            startVideoEncode();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        videoEncoderLoop = false;

        if (null != vEncoder) {
            vEncoder.stop();
        }
        if(mVirtualDisplay != null){
        	mVirtualDisplay.release();
        } 
       // if(mMediaProjection!= null) mMediaProjection.stop();
    }

    public void prepareVideoEncoder() throws IOException {
        MediaFormat format = MediaFormat.createVideoFormat(MIMETYPE_VIDEO_AVC, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(KEY_BIT_RATE, FRAME_BIT_RATE);
        format.setInteger(KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(KEY_I_FRAME_INTERVAL, FRAME_INTERVAL);
        MediaCodec vencoder = MediaCodec.createEncoderByType(MIMETYPE_VIDEO_AVC);
        vencoder.configure(format, null, null, CONFIGURE_FLAG_ENCODE);
        Surface surface = vencoder.createInputSurface();
        mVirtualDisplay = mDisplayManager.createVirtualDisplay("-display", width, height, 1,
                surface, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC);
        vEncoder = vencoder;
    }

    public void startVideoEncode() {
        if (vEncoder == null) {
            throw new RuntimeException("请初始化视频编码器");
        }
        if (videoEncoderLoop) {
            throw new RuntimeException("必须先停止");
        }
        videoEncoderThread = new Thread() {
            @Override
            public void run() {
//                presentationTimeUs = System.currentTimeMillis() * 1000;
                vEncoder.start();
                while (videoEncoderLoop && !Thread.interrupted()) {
                    try {
                        ByteBuffer[] outputBuffers = vEncoder.getOutputBuffers();
                        int outputBufferId = vEncoder.dequeueOutputBuffer(vBufferInfo, 0);
                        if (outputBufferId >= 0) {
                            ByteBuffer bb = outputBuffers[outputBufferId];
                            onEncodedAvcFrame(bb, vBufferInfo);
                            vEncoder.releaseOutputBuffer(outputBufferId, false);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        };
        videoEncoderLoop = true;
        videoEncoderThread.start();
    }

    private void onEncodedAvcFrame(ByteBuffer bb, final MediaCodec.BufferInfo vBufferInfo) {
    	
    //	Log.i(TAG, "hdb----b0:"+bb.get(0)+"b1:"+bb.get(1)+"b2:"+bb.get(2)+"b3:"+bb.get(3)+"b4:"+bb.get(4));
        int offset = 4;
        //判断帧的类型
        if (bb.get(2) == 0x01) {
            offset = 3;
        }
        int type = bb.get(offset) & 0x1f;
        if (type == NAL_SPS) {
            //[0, 0, 0, 1, 103, 66, -64, 13, -38, 5, -126, 90, 1, -31, 16, -115, 64, 0, 0, 0, 1, 104, -50, 6, -30]
            //打印发现这里将 SPS帧和 PPS帧合在了一起发送
            // SPS为 [4，len-8]
            // PPS为后4个字节
        	final byte[] bytes1 = new byte[vBufferInfo.size];
            bb.get(bytes1);
            Log.d(TAG, "hdb---sps:" + Arrays.toString(bytes1));
          /*  byte[] pps = new byte[8];
            byte[] sps = new byte[14];
         //   bb.getInt();// 抛弃 0,0,0,1
            bb.get(sps, 0, sps.length);
         //   bb.getInt();
            bb.get(pps, 0, pps.length);
            Log.d(TAG, "hdb---sps:" + Arrays.toString(sps) + ",PPS=" + Arrays.toString(pps));
            
            //hdb ---
            pps = null;
            sps = null;
            if (vBufferInfo.size > 22) {
            	final byte[] bytes = new byte[vBufferInfo.size - 22];
                bb.get(bytes,0 ,bytes.length);
                Log.i(TAG, "hdb---bytes:"+Arrays.toString(bytes));
                if (null != screenCaputreListener) {
                    screenCaputreListener.onImageData(bytes);
                }
			}*/
            
            //hdb ---

        } else if (type == NAL_SLICE || type == NAL_SLICE_IDR) {
            final byte[] bytes = new byte[vBufferInfo.size];
            bb.get(bytes);
            if (null != screenCaputreListener) {
                screenCaputreListener.onImageData(bytes);
            }
        }
    }
}
