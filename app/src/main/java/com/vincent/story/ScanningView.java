package com.vincent.story;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

import java.util.ArrayList;

/**
 * Depiction:
 * Author:Vincent
 * Date:2023/10/14
 */
public class ScanningView extends View {

    //默认的主题颜色
    private int DEFAULT_COLOR = Color.parseColor("#91D7F4");

    // 圆圈和交叉线的颜色
    private int mCircleColor = DEFAULT_COLOR;
    //圆圈的数量 不能小于1
    private int mCircleNum = 3;
    //扫描的颜色 ScanningView会对这个颜色做渐变透明处理
    private int mSweepColor = DEFAULT_COLOR;
    //是否显示交叉线
    private boolean isShowCross = true;
    //扫描的转速，表示几秒转一圈
    private float mSpeed = 3.0f;
    private Paint mCirclePaint;// 圆的画笔
    private Paint mTextPaint;// 文字的画笔
    private Paint mPercentSignPaint;// 百分号的画笔
    private Paint mSweepPaint; //扫描效果的画笔
    private Paint mRaindropPaint;// 水滴的画笔

    private float mDegrees; //扫描时的扫描旋转角度。
    private boolean isScanning = false;//是否扫描

    private int progress = 0;

    //保存水滴数据
    private ArrayList<Raindrop> mRaindrops = new ArrayList<>();

    public ScanningView(Context context) {
        super(context);
        init();
    }

    public ScanningView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        getAttrs(context, attrs);
        init();
    }

    public ScanningView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getAttrs(context, attrs);
        init();
    }

    /**
     * 获取自定义属性值
     *
     * @param context
     * @param attrs
     */
    private void getAttrs(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.ScanningView);
            mCircleColor = mTypedArray.getColor(R.styleable.ScanningView_circleColor, DEFAULT_COLOR);
            mCircleNum = mTypedArray.getInt(R.styleable.ScanningView_circleNum, mCircleNum);
            if (mCircleNum < 1) {
                mCircleNum = 3;
            }
            mSweepColor = mTypedArray.getColor(R.styleable.ScanningView_sweepColor, DEFAULT_COLOR);
            isShowCross = mTypedArray.getBoolean(R.styleable.ScanningView_showCross, true);
            mSpeed = mTypedArray.getFloat(R.styleable.ScanningView_speed, mSpeed);
            if (mSpeed <= 0) {
                mSpeed = 3;
            }
            mTypedArray.recycle();
        }
    }

    /**
     * 初始化
     */
    private void init() {
        // 初始化画笔
        mCirclePaint = new Paint();
        mCirclePaint.setColor(mCircleColor);
        mCirclePaint.setStrokeWidth(1);
        mCirclePaint.setStyle(Paint.Style.STROKE);
        mCirclePaint.setAntiAlias(true);

        mTextPaint = new Paint();
        mTextPaint.setColor(Color.parseColor("#ffffff"));
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setAntiAlias(true);

        mPercentSignPaint = new Paint();
        mPercentSignPaint.setTextSize(30);
        mPercentSignPaint.setColor(mCircleColor);
        mPercentSignPaint.setStyle(Paint.Style.FILL);
        mPercentSignPaint.setAntiAlias(true);

        mRaindropPaint = new Paint();
        mRaindropPaint.setStyle(Paint.Style.FILL);
        mRaindropPaint.setAntiAlias(true);

        mSweepPaint = new Paint();
        mSweepPaint.setAntiAlias(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //设置宽高,默认200dp
        int defaultSize = dp2px(getContext(), 200);
        setMeasuredDimension(measureWidth(widthMeasureSpec, defaultSize),
                measureHeight(heightMeasureSpec, defaultSize));
    }

    /**
     * 测量宽
     *
     * @param measureSpec
     * @param defaultSize
     * @return
     */
    private int measureWidth(int measureSpec, int defaultSize) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            result = defaultSize + getPaddingLeft() + getPaddingRight();
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        result = Math.max(result, getSuggestedMinimumWidth());
        return result;
    }

    /**
     * 测量高
     *
     * @param measureSpec
     * @param defaultSize
     * @return
     */
    private int measureHeight(int measureSpec, int defaultSize) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            result = defaultSize + getPaddingTop() + getPaddingBottom();
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        result = Math.max(result, getSuggestedMinimumHeight());
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        //计算圆的半径
        int width = getWidth() - getPaddingLeft() - getPaddingRight();
        int height = getHeight() - getPaddingTop() - getPaddingBottom();
        int radius = Math.min(width, height) / 2;

        //计算圆的圆心
        int cx = getPaddingLeft() + (getWidth() - getPaddingLeft() - getPaddingRight()) / 2;
        int cy = getPaddingTop() + (getHeight() - getPaddingTop() - getPaddingBottom()) / 2;

        drawCircle(canvas, cx, cy, radius);

        if (isShowCross) {
            drawCross(canvas, cx, cy, radius);
        }


        //正在扫描
        if (isScanning) {

            //画百分比进度
            drawPercentSign(canvas, cx, cy, radius);
            drawPercent(canvas, cx, cy, radius);
            drawSweep(canvas, cx, cy, radius);

            //计算雷达扫描的旋转角度
            mDegrees = (mDegrees + (360 / mSpeed / 60)) % 360;

            //触发View重新绘制，通过不断的绘制View的扫描动画效果
            invalidate();
        }
    }

    public void startAminator() {
        ValueAnimator animator = ValueAnimator.ofInt(0, 99);

        animator.setDuration(10000);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            progress = (int) animation.getAnimatedValue();
            Log.i("progress==", progress + "");
        });

        animator.start();
    }

    private void drawPercentSign(Canvas canvas, int cx, int cy, int radius) {
        int textSize = radius * 4 / 5;
        canvas.drawText("%", cx + textSize * 3 / 5, cy - textSize * 1 / 5, mPercentSignPaint);
    }

    private void drawPercent(Canvas canvas, int cx, int cy, int radius) {
        int textSize = radius * 4 / 5;
        mTextPaint.setTextSize(textSize);
        float x = 0;
        if (progress < 10) {
            x = cx - textSize * 1 / 3;
        } else {
            x = cx - textSize * 3 / 5;
        }
        canvas.drawText(progress + "", x, cy + textSize * 2 / 5, mTextPaint);
    }

    /**
     * 画圆
     */
    private void drawCircle(Canvas canvas, int cx, int cy, int radius) {
        int strokeWidth = 1;
        //画mCircleNum个半径不等的圆圈。
        for (int i = 0; i < mCircleNum; i++) {
            //根据圆的大小设置画笔的粗细
            strokeWidth = 5 / (i + 1);
            mCirclePaint.setStrokeWidth(strokeWidth);

            canvas.drawCircle(cx, cy, radius - (radius / mCircleNum * i) - strokeWidth / 2, mCirclePaint);
        }
    }

    /**
     * 画交叉线
     */
    private void drawCross(Canvas canvas, int cx, int cy, int radius) {
        //水平线
        canvas.drawLine(cx - radius, cy, cx + radius, cy, mCirclePaint);

        //垂直线
        canvas.drawLine(cx, cy - radius, cx, cy + radius, mCirclePaint);
    }


    /**
     * 画扫描效果
     */
    private void drawSweep(Canvas canvas, int cx, int cy, int radius) {
        //扇形的透明的渐变效果
        SweepGradient sweepGradient = new SweepGradient(cx, cy,
                new int[]{Color.TRANSPARENT, changeAlpha(mSweepColor, 0), changeAlpha(mSweepColor, 100),
                        changeAlpha(mSweepColor, 100), changeAlpha(mSweepColor, 0)
                }, new float[]{0.0f, 0.6f, 0.99f, 0.998f, 1f});
        mSweepPaint.setShader(sweepGradient);
        //先旋转画布，再绘制扫描的颜色渲染，实现扫描时的旋转效果。
        canvas.rotate(-90 + mDegrees, cx, cy);
        canvas.drawCircle(cx, cy, radius, mSweepPaint);
    }

    /**
     * 开始扫描
     */
    public void start() {
        if (!isScanning) {
            isScanning = true;
            startAminator();
            invalidate();
        }

    }

    private boolean isInterrupted() {
        if (progress > 100) {
            return true;
        }
        return false;
    }

    /**
     * 停止扫描
     */
    public void stop() {
        if (isScanning) {
            isScanning = false;
            mRaindrops.clear();
            mDegrees = 0.0f;
        }
    }

    /**
     * 水滴数据类
     */
    private static class Raindrop {

        int x;
        int y;
        float radius;
        int color;
        float alpha = 255;

        public Raindrop(int x, int y, float radius, int color) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.color = color;
        }

        /**
         * 获取改变透明度后的颜色值
         *
         * @return
         */
        public int changeAlpha() {
            return ScanningView.changeAlpha(color, (int) alpha);
        }

    }

    /**
     * dp转px
     */
    private static int dp2px(Context context, float dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dpVal, context.getResources().getDisplayMetrics());
    }

    /**
     * a
     * 改变颜色的透明度
     *
     * @param color
     * @param alpha
     * @return
     */
    private static int changeAlpha(int color, int alpha) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }
}
