/*
* -------------------------Reference: https://stackoverflow.com/questions/8974088/how-to-create-a-resizable-rectangle-with-user-touch-events-on-android
* */

package com.guillermonegrete.tts.CustomViews;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.guillermonegrete.tts.R;


public class DrawView extends View {

    Point[] points;

    /**
     * point1 and point 3 are of same group and same as point 2 and point4
     */
    int groupId = -1;
    private ArrayList<ColorBall> colorballs;
    // array that holds the balls
    private int balID = 0;
    // variable to know what ball is being dragged
    Paint paint;
    Canvas canvas;

    private int INITIAL_SIZE = 700;
    private String TAG = this.getClass().getSimpleName();
    private int wParent;
    private int hParent;
    private int left, top, right, bottom;
    private int[] bitmaps;

    public DrawView(Context context) {
        super(context);
        paint = new Paint();
        setFocusable(true); // necessary for getting the touch events
        canvas = new Canvas();
        setBitmaps(context);
    }

    public DrawView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setBitmaps(context);
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        setFocusable(true); // necessary for getting the touch events
        canvas = new Canvas();

        setBitmaps(context);
    }

    private void setBitmaps(Context context){
        wParent = context.getResources().getDisplayMetrics().widthPixels;
        hParent = context.getResources().getDisplayMetrics().heightPixels;
        Log.i(TAG, "H: "+wParent+" W: "+hParent);

        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) result = getResources().getDimensionPixelSize(resourceId);

        bitmaps = new int[4];
        points = new Point[4];
        bitmaps[0] = R.drawable.corner_topleft;
        bitmaps[1] = R.drawable.corner_bottomleft;
        bitmaps[2] = R.drawable.corner_bottomright;
        bitmaps[3] = R.drawable.corner_topright;
        points[0] = new Point();
        points[0].x = 24;
        points[0].y = result + 24;

        points[1] = new Point();
        points[1].x = 24;
        points[1].y = hParent-24;

        points[2] = new Point();
        points[2].x = wParent-24;
        points[2].y = hParent-24;

        points[3] = new Point();
        points[3].x = wParent-24;
        points[3].y = result+24;
        int j = 0;
        colorballs = new ArrayList<>();
        for (Point pt : points) {
            colorballs.add(new ColorBall(getContext(), bitmaps[j], j, pt));
            j++;
        }
        //invalidate();
    }

    // the method that draws the balls
    @Override
    protected void onDraw(Canvas canvas) {
        if(points[3]==null) //point4 null when user did not touch and move on screen.
            return;
        int nleft, ntop, nright, nbottom;
        int wBall = colorballs.get(0).getWidthOfBall();
        int hBall = colorballs.get(0).getHeightOfBall();
        left = points[0].x - wBall / 2;
        top = points[0].y - hBall / 2;
        right = points[0].x + wBall / 2;
        bottom = points[0].y + hBall / 2;
        for (int i = 1; i < points.length; i++) {
            nleft = points[i].x - wBall / 2; ntop = points[i].y - hBall / 2;
            nright = points[i].x + wBall / 2; nbottom = points[i].y + hBall / 2;
            left = left > nleft ? nleft : left;
            top = top > ntop ? ntop : top;
            right = right < nright ? nright : right;
            bottom = bottom < nbottom ? nbottom : bottom;
        }
        //Log.i(TAG, "L: "+left+" T: "+top+" R: "+right+" B: "+bottom);
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(5);

        //draw stroke
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.parseColor("#AA000000"));
        paint.setStrokeWidth(2);
        canvas.drawRect(left, top , right, bottom , paint);
        //fill the rectangle
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#55DB1255"));
        paint.setStrokeWidth(0);
        canvas.drawRect(left, top , right, bottom , paint);

        //draw the corners
        // draw the balls on the canvas
        paint.setColor(Color.BLUE);
        paint.setTextSize(18);
        paint.setStrokeWidth(0);
        for (int i =0; i < colorballs.size(); i ++) {
            ColorBall ball = colorballs.get(i);
            canvas.drawBitmap(ball.getBitmap(), points[i].x - wBall / 2, points[i].y - hBall / 2,
                    paint);

            //canvas.drawText("" + (i+1), points[i].x, points[i].y, paint);
        }
    }

    // events when touching the screen
    public boolean onTouchEvent(MotionEvent event) {
        int eventaction = event.getAction();

        int X = (int) event.getX();
        int Y = (int) event.getY();

        switch (eventaction) {

            case MotionEvent.ACTION_DOWN: // touch down so check if the finger is on
                // a ball
                if (points[0] == null) {

                    //initialize rectangle.
                    points[0] = new Point();
                    points[0].x = X;
                    points[0].y = Y;

                    points[1] = new Point();
                    points[1].x = X;
                    points[1].y = Y + INITIAL_SIZE;

                    points[2] = new Point();
                    points[2].x = X + INITIAL_SIZE;
                    points[2].y = Y + INITIAL_SIZE;

                    points[3] = new Point();
                    points[3].x = X +INITIAL_SIZE;
                    points[3].y = Y;

                    balID = 2;
                    groupId = 1;
                    // declare each ball with the ColorBall class
                    int j = 0;
                    for (Point pt : points) {
                        colorballs.add(new ColorBall(getContext(), bitmaps[j], j, pt));
                        j++;
                    }
                } else {
                    //resize rectangle
                    balID = -1;
                    groupId = -1;
                    for (int i = colorballs.size()-1; i>=0; i--) {
                        ColorBall ball = colorballs.get(i);
                        // check if inside the bounds of the ball (circle)
                        // get the center for the ball
                        int centerX = ball.getX();
                        int centerY = ball.getY();
                        paint.setColor(Color.CYAN);
                        // calculate the radius from the touch to the center of the
                        // ball
                        double radCircle = Math
                                .sqrt((double) (((centerX - X) * (centerX - X)) + (centerY - Y)
                                        * (centerY - Y)));

                        if (radCircle < ball.getWidthOfBall()) {

                            balID = ball.getID();
                            if (balID == 1 || balID == 3) {
                                groupId = 2;
                            } else {
                                groupId = 1;
                            }
                            invalidate();
                            break;
                        }
                        invalidate();
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE: // touch drag with the ball


                if (balID > -1) {
                    // move the balls the same as the finger
                    X = X < colorballs.get(balID).getWidthOfBall() / 2 ? colorballs.get(balID).getWidthOfBall() / 2 : X;
                    X = X > wParent - colorballs.get(balID).getWidthOfBall() / 2 ? wParent - colorballs.get(balID).getWidthOfBall() / 2 : X;
                    Y = Y < colorballs.get(balID).getWidthOfBall() / 2 ? colorballs.get(balID).getWidthOfBall() / 2 : Y;
                    Y = Y > hParent - colorballs.get(balID).getHeightOfBall() / 2 ? hParent - colorballs.get(balID).getHeightOfBall() / 2 : Y;
                    colorballs.get(balID).setX(X);
                    colorballs.get(balID).setY(Y);

                    paint.setColor(Color.CYAN);
                    if (groupId == 1) {
                        colorballs.get(1).setX(colorballs.get(0).getX());
                        colorballs.get(1).setY(colorballs.get(2).getY());
                        colorballs.get(3).setX(colorballs.get(2).getX());
                        colorballs.get(3).setY(colorballs.get(0).getY());
                    } else {
                        colorballs.get(0).setX(colorballs.get(1).getX());
                        colorballs.get(0).setY(colorballs.get(3).getY());
                        colorballs.get(2).setX(colorballs.get(3).getX());
                        colorballs.get(2).setY(colorballs.get(1).getY());
                    }

                    invalidate();
                }

                break;

            case MotionEvent.ACTION_UP:
                // touch drop - just do things here after dropping

                break;
        }
        // redraw the canvas
        invalidate();
        return true;

    }

    public int getPosx(){
        return left;
    }

    public int getPosy(){
        return top;
    }

    public int getRectHeight(){
        return bottom-top;
    }

    public int getRectWidth(){
        return right-left;
    }


    public static class ColorBall {

        Bitmap bitmap;
        Context mContext;
        Point point;
        int id;

        ColorBall(Context context, int resourceId,
                  int ID, Point point) {
            this.id = ID;
            bitmap = BitmapFactory.decodeResource(context.getResources(),
                    resourceId);
            mContext = context;
            this.point = point;
        }

        int getWidthOfBall() {
            return bitmap.getWidth();
        }

        int getHeightOfBall() {
            return bitmap.getHeight();
        }

        Bitmap getBitmap() {
            return bitmap;
        }

        int getX() {
            return point.x;
        }

        int getY() {
            return point.y;
        }

        int getID() {
            return id;
        }

        void setX(int x) {
            point.x = x;
        }

        void setY(int y) {
            point.y = y;
        }
    }
}