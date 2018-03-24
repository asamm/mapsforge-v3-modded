package org.mapsforgeV3.core.graphics;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

/**
 * Created by menion on 19/06/15.
 * Asamm Software, s. r. o.
 */
public class DrawTools {

    public static boolean isTransparent(Paint paint) {
        return paint.getShader() == null && paint.getAlpha() == 0;
    }

    private static final Rect mRect = new Rect();

    public static synchronized int getTextHeight(Paint paint, String text) {
        paint.getTextBounds(text, 0, text.length(), mRect);
        return mRect.height();
    }

    public static void drawTextRotated(Canvas canvas, String text, int x1, int y1, int x2, int y2, Paint paint) {
        if (isTransparent(paint)) {
            return;
        }

        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        canvas.drawTextOnPath(text, path, 0, 3, paint);
    }
}
