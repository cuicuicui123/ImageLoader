package com.example.cuiweicong.myapplication;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * 自定义RatingBar
 * 宽高都按照设置的每个star的宽度来，都会改成wrap_content
 */
public class CustomRatingBar extends LinearLayout {
    private int starNum;
    private int starDrawableRes;
    private int starWidth;
    private int starMargin;
    private ImageView[] imageViews;
    private float downX;
    private int currentRating = 0;
    private LayerDrawable layerDrawable;
    boolean moved = false;

    private static final int DEFAULT_WIDTH = 180;
    private static final int DEFAULT_NUM = 5;
    private static final int DEFAULT_MARGIN = 10;
    private static final int DEFAULT_DRAWABLE = R.drawable.rating;

    public CustomRatingBar(Context context) {
        this(context, null);
    }

    public CustomRatingBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomRatingBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthSpecMode == MeasureSpec.AT_MOST) {
            int widthSize = starWidth * starNum + starMargin * 6;
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(widthSize, widthSpecMode);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void init(Context context, AttributeSet attrs){
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.CustomRatingBar);
        starNum = array.getInteger(R.styleable.CustomRatingBar_star_num, DEFAULT_NUM);
        starDrawableRes = array.getResourceId(R.styleable.CustomRatingBar_star_drawable, DEFAULT_DRAWABLE);
        starWidth = array.getDimensionPixelSize(R.styleable.CustomRatingBar_star_width, DEFAULT_WIDTH);
        starMargin = array.getDimensionPixelSize(R.styleable.CustomRatingBar_star_margin, DEFAULT_MARGIN);
        array.recycle();
        setOrientation(HORIZONTAL);
        initStars(context);
    }

    private void initStars(Context context){
        imageViews = new ImageView[starNum];
        Drawable drawable = context.getResources().getDrawable(starDrawableRes);
        if (drawable instanceof LayerDrawable) {
            layerDrawable = (LayerDrawable) drawable;
        } else {
            layerDrawable = (LayerDrawable) context.getResources().getDrawable(DEFAULT_DRAWABLE);
        }
        Drawable emptyDrawable = layerDrawable.getDrawable(layerDrawable.getNumberOfLayers() - 1);
        int starHeight = emptyDrawable.getIntrinsicHeight() * starWidth / emptyDrawable.getIntrinsicWidth();
        for (int i = 0;i < starNum;i ++) {
            ImageView imageView = new ImageView(context);
            imageViews[i] = imageView;
            imageView.setImageDrawable(emptyDrawable);
            LinearLayout.LayoutParams lp = new LayoutParams(starWidth, starHeight);
            lp.leftMargin = starMargin / 2;
            lp.rightMargin = starMargin / 2;
            addView(imageView, lp);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                if (isClickable()) {
                    performClick();
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                float distance = event.getX() - downX;
                if (Math.abs(distance) > 60) {
                    moved = true;
                    handleMoveEvent(distance);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (!moved) {
                    handleUpEvent(event.getX());
                } else {
                    moved = false;
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    private void handleMoveEvent(float distance){
        if (layerDrawable.getNumberOfLayers() == 0) {
            return;
        }
        if (distance < 0 && currentRating == -1) {
            return;
        }
        if (distance > 0 && currentRating == starNum) {
            return;
        }
        currentRating = distance > 0 ? currentRating + 1 : currentRating - 1;
        handleImageView();
        downX += distance;
    }

    private void handleImageView() {
        for (int i = 0;i < imageViews.length;i ++) {
            ImageView imageView = imageViews[i];
            if (i <= currentRating) {
                imageView.setImageDrawable(layerDrawable.getDrawable(0));
            } else {
                imageView.setImageDrawable(layerDrawable.getDrawable(layerDrawable.getNumberOfLayers() - 1));
            }
        }
    }

    private void handleUpEvent(float upX){
        currentRating = (int) (upX / (starWidth + starMargin));
        handleImageView();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }
}
