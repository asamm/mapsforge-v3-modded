package org.mapsforgeV3.android.maps.rendertheme.tools;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.RectF;
import android.graphics.Paint.Style;

import org.mapsforgeV3.map.layer.renderer.PaintContainerPointText;
import org.mapsforgeV3.android.maps.rendertheme.renderinstruction.RenderInstruction;

import java.util.HashMap;

import static org.mapsforgeV3.android.maps.rendertheme.renderinstruction.RenderInstruction.*;

public class BgRectangle {

    /**
     * Create background rectangle object from list of attributes.
     * @param attrs attributes
     * @return background rectangle object
     */
    public static BgRectangle create(HashMap<String, String> attrs) {
        // BASIC
        int bgRectFill = 0;
        int bgRectStroke = 0;
        float bgRectOver = 0.0f;
        float bgRectStrokeWidth = 0.0f;
        float bgRectRound = 0.0f;

        // BASIC
        if (attrs.containsKey(KEY_BG_RECT_FILL)) {
            bgRectFill = Color.parseColor(attrs.remove(KEY_BG_RECT_FILL));
        }
        if (attrs.containsKey(KEY_BG_RECT_STROKE)) {
            bgRectStroke = Color.parseColor(attrs.remove(KEY_BG_RECT_STROKE));
        }
        if (attrs.containsKey(KEY_BG_RECT_OVER)) {
            bgRectOver = RenderInstruction.parseLengthUnits(attrs.remove(KEY_BG_RECT_OVER));
        }
        if (attrs.containsKey(KEY_BG_RECT_STROKE_WIDTH)) {
            bgRectStrokeWidth = RenderInstruction.parseLengthUnits(attrs.remove(KEY_BG_RECT_STROKE_WIDTH));
        }
        if (attrs.containsKey(KEY_BG_RECT_ROUNDED)) {
            bgRectRound = RenderInstruction.parseLengthUnits(attrs.remove(KEY_BG_RECT_ROUNDED));
        }

        // check variables
        if (bgRectFill == 0 && bgRectStroke == 0) {
            return null;
        }

        // finally return generated icon
        return new BgRectangle(bgRectFill, bgRectStroke, bgRectStrokeWidth,
                bgRectOver, bgRectRound);
    }

	final float bgRectOver;
	final float bgRectRound;

	final private Paint paintBackFill;
	final private Paint paintBackStroke;
	
	private BgRectangle(int bgRectFill, int bgRectStroke,
                        float bgRectStrokeWidth, float bgRectOver, float bgRectRound) {
	
		this.bgRectOver = bgRectOver;
		this.bgRectRound = bgRectRound;
		
		if (bgRectFill != 0) {
			paintBackFill = new Paint(Paint.ANTI_ALIAS_FLAG);
			paintBackFill.setColor(bgRectFill);
		} else {
			paintBackFill = null;
		}
		
		if (bgRectStroke != 0) {
			paintBackStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
			paintBackStroke.setStyle(Style.STROKE);
			paintBackStroke.setColor(bgRectStroke);
			paintBackStroke.setStrokeWidth(bgRectStrokeWidth);
		} else {
			paintBackStroke = null;
		}
	}
	
	public void draw(Canvas c, PaintContainerPointText ptc) {
		RectF rect = new RectF();
		rect.left = ptc.x + ptc.boundary.left - bgRectOver;
		rect.top = ptc.y + ptc.boundary.top - bgRectOver;
		rect.right = ptc.x + ptc.boundary.right + bgRectOver;
		rect.bottom = ptc.y + ptc.boundary.bottom + bgRectOver;
		if (ptc.paintFill.getTextAlign() == Align.CENTER) {
			rect.offset(-1 * (ptc.boundary.width() / 2), 0);
		} else if (ptc.paintFill.getTextAlign() == Align.RIGHT) {
			rect.offset(-1 * ptc.boundary.width(), 0);
		}

		drawRect(c, rect, paintBackFill);
		drawRect(c, rect, paintBackStroke);
	}

	private void drawRect(Canvas c, RectF rect, Paint paint) {
		if (paint != null) {
			if (bgRectRound > 0.0f) {
				c.drawRoundRect(rect, bgRectRound, bgRectRound, paint);	
			} else {
				c.drawRect(rect, paint);
			}
		}
	}
}
