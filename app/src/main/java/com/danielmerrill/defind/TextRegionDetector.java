package com.danielmerrill.defind;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by danielmerrill on 7/24/15.
 */
public class TextRegionDetector{
    private Rect centerWordRect;
    private android.graphics.Rect rectToReturn;
    private int kernelSize;
    private int dilateIndex;
    private String TAG = "TextRegionDetector";
    private Boolean OPEN = false;
    private Boolean CLOSE = true;
    private int canvasH;
    private int canvasW;
    private Point canvasCenter;
    private Mat word;
    private int padding; // Set rectangle padding
    private Point rectOpen;
    private Point rectClose;
    private Rect thisContour;
    private int x;
    private int y;
    private int w;
    private int h;
    private Point wordCenter = new Point(0,0);
    private int thisDistance;
    private int bestDistance;
    private MainActivity mainActivity;
    private boolean paused;
    private int prevDistance;



    public TextRegionDetector(MainActivity mainActivity) {
        kernelSize = 3;
        dilateIndex = 5;
        padding = 5;
        rectToReturn = new android.graphics.Rect();
        paused = false;
        this.mainActivity = mainActivity;
    }



    public void setDilate(int dilate) {
        dilateIndex = dilate;
    }

    public void setKernalSize(int kernel) {
        kernelSize = kernel;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public Bitmap process(Bitmap input) {
        Log.i(TAG, "process()");
        Mat rgbaImage = new Mat();
        Utils.bitmapToMat(input, rgbaImage);

        if (canvasCenter == null) {
            canvasW = rgbaImage.width();
            canvasH = rgbaImage.height();
            canvasCenter = new Point(canvasW/2, canvasH/2);
        }

        //Mat processed = new Mat(rgbaImage, new Rect(roiLMargin, roiTopMargin, roiRMargin, roiBottomMargin)); // set ROI
        Mat processed = new Mat();
        Mat kernel;
        Mat heirarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();



        Imgproc.cvtColor(rgbaImage, rgbaImage, Imgproc.COLOR_BGR2GRAY); // greyscale the entire image
        Imgproc.GaussianBlur(rgbaImage, processed, new Size(5, 5), 0);// blur width more than height b/c we want to blend letters horizontally but not across line breaks
        Imgproc.adaptiveThreshold(processed, processed, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 11, 2); // threshold



        kernel = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(kernelSize,kernelSize));
        Imgproc.dilate(processed, processed, kernel, new Point(), dilateIndex);
        Imgproc.findContours(processed, contours, heirarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE); // See above



        for (MatOfPoint contour : contours) {
            thisContour = Imgproc.boundingRect(contour);
            x = thisContour.x;
            y = thisContour.y;
            w = thisContour.width;
            h = thisContour.height;

            // Discard areas too large
            if (h > canvasH - 5 && w > canvasW - 10) continue;

            // Discard areas too small
            if (h < 40 || w < 40) continue;

            // Calculate this word's center and find its distance from canvas center
            wordCenter.x = x + (w / 2);
            wordCenter.y = y + (h / 2);
            thisDistance = getDistance(wordCenter, canvasCenter);

            // Load a default center word if first time through
            if (centerWordRect == null){
                setCenterWord(thisContour, rgbaImage);
                bestDistance = thisDistance;
            }

            // Test to see if this word is closest to the center of the canvas
            if (thisDistance < bestDistance) {
                bestDistance = thisDistance;
                setCenterWord(thisContour, rgbaImage);
            }

            // Draw grey bounding box with padding around all words
            // rectOpen = pad(new Point(x,y), rgbaImage, padding, OPEN);
            // rectClose = pad(new Point(x + w, y + h), rgbaImage, padding, CLOSE);
            // Imgproc.rectangle(rgbaImage, rectOpen, rectClose, new Scalar(200, 200, 200), 2);

        }
        if (centerWordRect != null && !paused) {
            // Cut out word
            rectOpen = pad(new Point(centerWordRect.x, centerWordRect.y), rgbaImage, padding, OPEN);
            rectClose = pad(new Point(centerWordRect.x + centerWordRect.width, centerWordRect.y + centerWordRect.height), rgbaImage, padding, CLOSE);

            Rect currentRect = new Rect(rectOpen, rectClose);


            // don't process the image if the values are outside of the distance tolerated (should play around with this number)

            float differenceTolerated = .1f;
            int currentDistance = bestDistance;
            float movementAmount = (float)Math.abs(currentDistance - prevDistance) / (float)canvasW;

            //Log.i(TAG, "movementAmt =" + movementAmount);

            centerWordRect = null;
            prevDistance = bestDistance;

            if (movementAmount < differenceTolerated){
                // process image and return it to main activity
                word = new Mat(rgbaImage, currentRect);
                //Imgproc.adaptiveThreshold(word, word, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);

                // Create a bitmap of just the word
                Bitmap bmp = Bitmap.createBitmap(word.cols(), word.rows(), Bitmap.Config.ARGB_8888);

                Utils.matToBitmap(word, bmp);

                mainActivity.updateTextRect(rectToReturn);
                return bmp;
            } else {
                mainActivity.updateTextRect(null);
                return null;
            }


        } else {
            mainActivity.updateTextRect(null);
            return null;
        }
    }

    private int getDistance(Point p1, Point p2) {
        return (int) Math.sqrt((p1.x-p2.x)*(p1.x-p2.x) + (p1.y-p2.y)*(p1.y-p2.y));
    }

    private Point pad (Point coord, Mat img, int padding, boolean closed) {
        Point padded = new Point();
        if (!closed) {
            padded.x = (coord.x - padding > 0) ? coord.x - padding : 0;
            padded.y = (coord.y - padding > 0) ? coord.y - padding : 0;
        } else {
            padded.x = (coord.x + padding < img.width()) ? coord.x + padding : img.width();
            padded.y = (coord.y + padding < img.height()) ? coord.y + padding : img.height();
        }

        return padded;
    }

    private android.graphics.Rect cvRectToPaddedAndroidRect(Rect rectToConvert, int padding, Mat img) {
        android.graphics.Rect newRect = new android.graphics.Rect();
        int l = rectToConvert.x;
        int t = rectToConvert.y;
        int r = rectToConvert.x + rectToConvert.width;
        int b = rectToConvert.y + rectToConvert.height;

        newRect.left = x;
        newRect.top = y;
        newRect.right = (r + padding < img.width()) ? r + padding : img.width();
        newRect.bottom = (b + padding < img.height()) ? b + padding : img.height();

        return newRect;
    }

    private void setCenterWord(Rect newCenterWord, Mat img) {
        centerWordRect = newCenterWord;
        rectToReturn = cvRectToPaddedAndroidRect(centerWordRect, padding, img);
        Log.i(TAG, "Center Word at " + centerWordRect.x + ", " + centerWordRect.y);
    }

}
