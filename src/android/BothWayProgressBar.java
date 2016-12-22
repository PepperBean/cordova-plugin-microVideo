package com.pepperbean.plugin;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class BothWayProgressBar extends View {
    private static final String TAG = "BothWayProgressBar";
    //取消状态为红色bar, 反之为绿色bar
    private boolean isCancel = false;
    private Context mContext;
    //正在录制的画笔
    private Paint mRecordPaint;
    //上滑取消时的画笔
    private Paint mCancelPaint;
    //是否显示
    private int mVisibility;
    // 当前进度 [0~1]
    private float progress;
    //进度条结束的监听
    private OnProgressEndListener mOnProgressEndListener;

    public BothWayProgressBar(Context context) {
        super(context, null);
    }
    public BothWayProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }
    private void init() {
        mVisibility = INVISIBLE;
        mRecordPaint = new Paint();
        mRecordPaint.setColor(Color.GREEN);
        mCancelPaint = new Paint();
        mCancelPaint.setColor(Color.RED);
        progress = 0;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mVisibility == View.VISIBLE) {
            int height = getHeight();
            int width = getWidth();
            int mid = width / 2;

            int progLength = (int) (progress * mid);

            //画出进度条

            canvas.drawRect(progLength, 0, width - progLength, height, isCancel ? mCancelPaint : mRecordPaint);

            if (mOnProgressEndListener != null && progress >= 1) {
                mOnProgressEndListener.onProgressEndListener();
            } else if (mOnProgressEndListener != null && isCancel) {
                mOnProgressEndListener.onProgressEndListener();
            }
        } else {
            canvas.drawColor(Color.argb(0, 0, 0, 0));
        }
    }




    /**
     * 设置进度
     * @param progress
     */
    public void setProgress(float progress) {
        if (progress >= 1) {
            this.progress = 1;
        } else {
            this.progress = progress;
        }
        invalidate();
    }

    /**
     * 设置录制状态 是否为取消状态
     * @param isCancel
     */
    public void setCancel(boolean isCancel) {
        this.isCancel = isCancel;
        this.progress = 0;
        invalidate();
    }
    /**
     * 重写是否可见方法
     * @param visibility
     */
    @Override
    public void setVisibility(int visibility) {
        mVisibility = visibility;
        //重新绘制
        invalidate();
    }

    /**
     * 当进度条结束后的 监听
     * @param onProgressEndListener
     */
    public void setOnProgressEndListener(OnProgressEndListener onProgressEndListener) {
        mOnProgressEndListener = onProgressEndListener;
    }


    public interface OnProgressEndListener{
        void onProgressEndListener();
    }

}
