package com.example.cuiweicong.myapplication;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

public class FiveStarView extends View {
    private float rating;
    private int layerDrawableRes;
    private int margin;
    private int size;
    private Rect srcRect;
    private RectF destRect;
    private Paint paint;

    private static final int DEFAULT_SIZE = 60;
    private static final int DEFAULT_MARGIN = 20;

    public FiveStarView(Context context) {
        this(context, null);
    }

    public FiveStarView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FiveStarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs){
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.FiveStarView);
        layerDrawableRes = array.getResourceId(R.styleable.FiveStarView_five_star_drawable, R.drawable.rating);
        size = array.getDimensionPixelSize(R.styleable.FiveStarView_five_star_size, DEFAULT_SIZE);
        margin = array.getDimensionPixelSize(R.styleable.FiveStarView_fice_star_margin, DEFAULT_MARGIN);
        array.recycle();
        srcRect = new Rect();
        destRect = new RectF();
        paint = new Paint();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (widthMode == MeasureSpec.AT_MOST) {
            widthMeasureSpec = MeasureSpec.makeMeasureSpec((size + margin) * 5, widthMode);
        }
        if (heightMode == MeasureSpec.AT_MOST) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.AT_MOST);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Drawable drawable = getResources().getDrawable(layerDrawableRes);

        if (drawable instanceof LayerDrawable) {
            LayerDrawable layerDrawable = (LayerDrawable) drawable;
            if (layerDrawable.getNumberOfLayers() > 0) {
                layerDrawable.getDrawable(layerDrawable.getNumberOfLayers() - 1);
                BitmapDrawable emptyDrawable = (BitmapDrawable) layerDrawable.findDrawableByLayerId(R.id.empty);
                BitmapDrawable fullDrawable = (BitmapDrawable) layerDrawable.findDrawableByLayerId(R.id.full);
                if (emptyDrawable == null || fullDrawable == null) {
                    return;
                }
                drawFullStar(canvas, fullDrawable);
                drawEmptyStar(canvas, emptyDrawable);
            }
        }
    }

    private void drawEmptyStar(Canvas canvas, BitmapDrawable emptyDrawable) {
        float singleWidth = size + margin;
        Bitmap emptyBitmap = emptyDrawable.getBitmap();
        if (rating < 5) {
            for (float i = 5;i > rating;i --) {
                int left;
                int right = (int) (i * singleWidth - margin / 2);
                if ((i - rating) >= 1) {
                    srcRect.set(0, 0, emptyBitmap.getWidth(), emptyBitmap.getHeight());
                    left = (int) ((i - 1) * singleWidth + margin / 2);
                } else  {
                    srcRect.set((int) (((1 - (i - rating)) * emptyBitmap.getWidth())), 0, emptyBitmap.getWidth(), emptyBitmap.getHeight());
                    left = (int) ((i - 1) * singleWidth + margin / 2 + (rating - (i - 1)) * size);
                }
                destRect.set(left, 0, right, size);
                canvas.drawBitmap(emptyBitmap, srcRect, destRect, paint);
            }
        }
    }

    private void drawFullStar(Canvas canvas, BitmapDrawable fullDrawable) {
        Bitmap fullBitmap = fullDrawable.getBitmap();
        float singleWidth = size + margin;
        if (rating > 0) {
            for (int i = 0;i < rating;i ++) {
                int left = (int) (i * singleWidth + margin / 2);
                int right;
                if ((rating - i) >= 1) {
                    srcRect.set(0, 0, fullBitmap.getWidth(), fullBitmap.getHeight());
                    right = (int) (i * singleWidth + margin / 2 + size);
                } else {
                    srcRect.set(0, 0, (int) ((rating - i)* fullBitmap.getWidth()), fullBitmap.getHeight());
                    right = (int) (i * singleWidth + margin / 2 + size * (rating - i));
                }
                destRect.set(left, 0, right, size);
                canvas.drawBitmap(fullBitmap, srcRect, destRect, paint);
            }
        }
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
        invalidate();
    }
}
