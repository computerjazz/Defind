package com.danielmerrill.defind.Core.ExtraViews;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.danielmerrill.defind.R;

/**
 * Created by Fadi on 5/11/2014.
 */
public class FocusBoxView extends View {

    private static final int MIN_FOCUS_BOX_WIDTH = 50;
    private static final int MIN_FOCUS_BOX_HEIGHT = 20;

    private final Paint paint;
    private final int maskColor;
    private final int textRectColor;
    private final String TAG = getClass().getSimpleName();
    private RoundRectShape roundRect;
    private RoundRectShape textBoxRounded;
    private Rect textBox;
    private Rect frame;
    private int previewWidth;
    private int previewHeight;
    private float canvasToPreviewRatioW;
    private float canvasToPreviewRatioH;
    private boolean ratioSet;
    private boolean previewSizeSet;

    public FocusBoxView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Resources resources = getResources();
        maskColor = resources.getColor(R.color.focus_box_mask);
        textRectColor = resources.getColor(R.color.textRect);

        // set up window raduis
        float windowOuterRadii[] = {0, 0, 0, 0, 0, 0, 0, 0};
        float windowInnerRadii[] = {8, 8, 8, 8, 8, 8, 8, 8};
        roundRect = new RoundRectShape(windowOuterRadii, new RectF(5,5,5,5), windowInnerRadii);

        //set up text box
        float textBoxOuterRadii[] = {10, 10, 10, 10, 10, 10, 10, 10};
        float textBoxInnerRadii[] = {5, 5, 5, 5, 5, 5, 5, 5};
        textBoxRounded = new RoundRectShape(textBoxOuterRadii, null, textBoxInnerRadii);

        frame = getBoxRect();
        ratioSet = false;
        previewSizeSet = false;

    }

    private Rect box;

    private static Point ScrRes;

    private  Rect getBoxRect() {

        if (box == null) {

            ScrRes = FocusBoxUtils.getScreenResolution(getContext());

            // Box takes up 6/7 of screen width
            int width = ScrRes.x * 6 / 7;
            // Box takes up 1/9 of screen height
            int height = ScrRes.y / 9;

            width = width == 0
                    ? MIN_FOCUS_BOX_WIDTH
                    : width < MIN_FOCUS_BOX_WIDTH ? MIN_FOCUS_BOX_WIDTH : width;

            height = height == 0
                    ? MIN_FOCUS_BOX_HEIGHT
                    : height < MIN_FOCUS_BOX_HEIGHT ? MIN_FOCUS_BOX_HEIGHT : height;

            // Set box equidistant on both sides
            int left = (ScrRes.x - width) / 2;

            // Use the same margin as width for the top
            int top = left;

            box = new Rect(left, top, left + width, top + height);
        }

        return box;
    }

    public Rect getBox() {
        return box;
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (!ratioSet && previewSizeSet) {

            int canvasWidth = (canvas.getWidth() < canvas.getHeight()) ? canvas.getWidth() : canvas.getHeight();
            int canvasHeight = (canvas.getWidth() < canvas.getHeight()) ? canvas.getHeight() : canvas.getWidth();
            canvasToPreviewRatioW = (float)canvasWidth / (float)previewWidth;
            canvasToPreviewRatioH = (float)canvasHeight / (float)previewHeight;

            Log.i(TAG, "Preview dimensions = " + previewWidth + " x " + previewHeight);
            Log.i(TAG, "Canvas dimensions = " + canvasWidth + " x " + canvasHeight);
            Log.i(TAG, "canvasToPreviewRatioW = " + canvasToPreviewRatioW);
            Log.i(TAG, "canvasToPreviewRatioH = " + canvasToPreviewRatioH);
            ratioSet = true;
        } else if (!previewSizeSet) {
            canvasToPreviewRatioH = canvasToPreviewRatioW = 1.0f;
        }

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Draw the bounding boxes around the window
        paint.setColor(maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);

        roundRect.resize(frame.width() + 1, frame.height() + 1);
        canvas.save();
        canvas.translate(frame.left, frame.top);
        roundRect.draw(canvas, paint);
        canvas.restore();

        // Draw box around text if one exists
        paint.setColor(textRectColor);
        if (textBox != null) {
            textBox.left = (int)((float)textBox.left * canvasToPreviewRatioW);
            textBox.top = (int)((float)textBox.top * canvasToPreviewRatioH);
            textBox.right = (int)((float)textBox.right * canvasToPreviewRatioW);
            textBox.bottom = (int)((float)textBox.bottom * canvasToPreviewRatioH);

         //   Log.i(TAG, "Drawing a textBox at (" + textBox.left +", " + textBox.top + ") from frame edge");
            textBoxRounded.resize(textBox.width(),textBox.height());
            canvas.save();
            canvas.translate(frame.left + textBox.left, frame.top + textBox.top);
            textBoxRounded.draw(canvas, paint);
            canvas.restore();
        }
    }

    public void setTextBox(Rect rect) {
        textBox = rect;
    }

    public void setPreviewSize(int w, int h) {
        previewWidth = w;
        previewHeight = h;
        previewSizeSet = true;
    }

}
