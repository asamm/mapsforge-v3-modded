/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforgeV3.map.layer.renderer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.Typeface;

import org.mapsforgeV3.android.maps.mapgenerator.MapGenerator.ExtraRenderingHandler;
import org.mapsforgeV3.android.maps.rendertheme.tools.CurveStyle;
import org.mapsforgeV3.core.model.Tile;

import java.util.Collections;
import java.util.List;

/**
 * A CanvasRasterer uses a Canvas for drawing.
 * 
 * @see <a href="http://developer.android.com/reference/android/graphics/Canvas.html">Canvas</a>
 */
class CanvasRasterer {

	private static final Paint PAINT_BITMAP_FILTER = new Paint(Paint.FILTER_BITMAP_FLAG);
	private static final Paint PAINT_TILE_COORDINATES = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_TILE_COORDINATES_STROKE = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_TILE_FRAME = new Paint(Paint.ANTI_ALIAS_FLAG);

	static {
		PAINT_TILE_COORDINATES.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		PAINT_TILE_COORDINATES.setTextSize(20);

		PAINT_TILE_COORDINATES_STROKE.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		PAINT_TILE_COORDINATES_STROKE.setStyle(Paint.Style.STROKE);
		PAINT_TILE_COORDINATES_STROKE.setStrokeWidth(5);
		PAINT_TILE_COORDINATES_STROKE.setTextSize(20);
		PAINT_TILE_COORDINATES_STROKE.setColor(Color.WHITE);
	}

	// path object
	private final Path path;
	// matrix for transformations
	private final Matrix symbolMatrix;

	// main picture
	private Picture picture;
	// canvas generated from picture
	private Canvas canvas;

	CanvasRasterer(int tileSize) {
		picture = new Picture();
		canvas = picture.beginRecording(tileSize, tileSize);
		symbolMatrix = new Matrix();
		path = new Path();
		path.setFillType(Path.FillType.EVEN_ODD);
	}

    void setCustomCanvas(Canvas canvas) {
        this.canvas = canvas;
    }

	void finish(Canvas canvas) {
		picture.draw(canvas);
	}
	
	private void drawTileCoordinate(String string, int offsetY) {
		canvas.drawText(string, 20, offsetY, PAINT_TILE_COORDINATES_STROKE);
		canvas.drawText(string, 20, offsetY, PAINT_TILE_COORDINATES);
	}

	void drawNodes(List<PaintContainerPointText> nodes, List<PaintContainerPointText> areaLabels) {
		// merge nodes and labels
		nodes.addAll(areaLabels);
		Collections.sort(areaLabels);
		
		for (int index = nodes.size() - 1; index >= 0; --index) {
			PaintContainerPointText ptc = nodes.get(index);

			// draw background rectangle
			if (ptc.bgRect != null) {
				ptc.bgRect.draw(canvas, ptc);
			}
			
			// draw text
			if (ptc.paintStroke != null) {
				this.canvas.drawText(ptc.text,
						ptc.x, ptc.y, ptc.paintStroke);
			}

			this.canvas.drawText(ptc.text,
					ptc.x, ptc.y, ptc.paintFill);
		}
	}

	void drawSymbols(List<PaintContainerSymbol> symbolContainers) {
//Logger.d("CanvasRasterer", "drawSymbols(), size:" + symbolContainers.size());
		for (int index = symbolContainers.size() - 1; index >= 0; --index) {
			PaintContainerSymbol symbolContainer = symbolContainers.get(index);
//Logger.d("CanvasRasterer", "  symbol:" + symbolContainer.symbol + ", " + symbolContainer.x + ", " +
//			symbolContainer.y + ", " + symbolContainer.alignCenter + ", " + symbolContainer.priority +
//			", scale:" + symbolContainer.scale + ", rotate:" + symbolContainer.rotation);
			symbolMatrix.reset();
			if (symbolContainer.alignCenter) {
				int pivotX = symbolContainer.symbol.getWidth() >> 1;
				int pivotY = symbolContainer.symbol.getHeight() >> 1;
				this.symbolMatrix.setRotate(symbolContainer.rotation, pivotX, pivotY);
				this.symbolMatrix.postScale(symbolContainer.scale, symbolContainer.scale);
				this.symbolMatrix.postTranslate(symbolContainer.x - pivotX, symbolContainer.y - pivotY);
			} else {
				this.symbolMatrix.setRotate(symbolContainer.rotation);
				this.symbolMatrix.postScale(symbolContainer.scale, symbolContainer.scale);
				this.symbolMatrix.postTranslate(symbolContainer.x, symbolContainer.y);
			}
			
			// finally draw image
			canvas.drawBitmap(symbolContainer.symbol, this.symbolMatrix, PAINT_BITMAP_FILTER);
		}
	}

	void drawTileCoordinates(Tile tile) {
		drawTileCoordinate("X: " + tile.tileX, 30);
		drawTileCoordinate("Y: " + tile.tileY, 60);
		drawTileCoordinate("Z: " + tile.zoomLevel, 90);
	}

	void drawTileFrame(int tileSize) {
		this.canvas.drawLines(new float[] {
				0, 0, 0, tileSize,
				0, tileSize, tileSize, tileSize,
				tileSize, tileSize, tileSize, 0},
				PAINT_TILE_FRAME);
	}

	void drawWayNames(List<PaintContainerWayText> wayTextContainers) {
		for (int index = wayTextContainers.size() - 1; index >= 0; --index) {
			PaintContainerWayText wtc = wayTextContainers.get(index);
			this.path.rewind();

			float[] textCoordinates = wtc.coordinates;
			this.path.moveTo(textCoordinates[0], textCoordinates[1]);
			for (int i = 2; i < textCoordinates.length; i += 2) {
				this.path.lineTo(textCoordinates[i], textCoordinates[i + 1]);
			}
//Logger.w("XXX", "drawWayNames(), wtc:" + wtc.text + ", " + wtc.coordinates + ", bgRect:" + wtc.bgRect);
//			// draw background rectangle
//			if (wtc.bgRect != null) {
//				wtc.bgRect.draw(canvas, PointTextContainer.
//						generateTextBoundary(wtc.text, wtc.paintFill,
//								wtc.paintStroke));
//			}

			// draw text "stroke"
			if (wtc.paintStroke != null) {
				this.canvas.drawTextOnPath(wtc.text, this.path,
						wtc.horOffset, wtc.verOffset, wtc.paintStroke);
			}

            // draw text "fill"
			this.canvas.drawTextOnPath(wtc.text, this.path,
                    wtc.horOffset, wtc.verOffset, wtc.paintFill);
        }
	}

	private static final Object drawWaysLock = new Object();
	
	void drawWays(int zoomLevel, List<PaintContainerShape>[][] ways,
			ExtraRenderingHandler extraHandler) {
		if (extraHandler == null || !extraHandler.drawWaysSynchronized()) {
			drawWaysPrivate(zoomLevel, ways, extraHandler);
		} else {
			synchronized (drawWaysLock) {
				drawWaysPrivate(zoomLevel, ways, extraHandler);
			}
		}
	}
	
	private void drawWaysPrivate(int zoomLevel, List<PaintContainerShape>[][] drawWays,
			ExtraRenderingHandler extraHandler) {
		int levelsPerLayer = drawWays[0].length;
		for (int layer = 0, layers = drawWays.length; layer < layers; ++layer) {
			// get required parameters for certain level
			List<PaintContainerShape>[] shapePaintContainers = drawWays[layer];
			boolean extraDraw = extraHandler != null &&
					extraHandler.handleRenderWay(zoomLevel, layer);

			// iterate over data in level
			for (int level = 0; level < levelsPerLayer; ++level) {
				List<PaintContainerShape> wayList = shapePaintContainers[level];

				for (int index = wayList.size() - 1; index >= 0; --index) {
					PaintContainerShape shapePaintContainer = wayList.get(index);
					this.path.rewind();

                    // prepare path
					switch (shapePaintContainer.shape.getShapeType()) {
                        case CIRCLE:
                            prepareWayShapeCircle(shapePaintContainer);
							break;
						case WAY:
                            prepareWayShapePolyline(shapePaintContainer);
							break;
                    }
					
					// finally draw path
					if (extraDraw) {
						extraHandler.renderWay(canvas, path, layer, level);
					} else {
//                        this.canvas.drawLine();
						if (shapePaintContainer.paintBorder != null) {
							this.canvas.drawPath(this.path, shapePaintContainer.paintBorder);
						}
						this.canvas.drawPath(this.path, shapePaintContainer.paint);
					}
				}
			}
			
			// notify about end of layer
			if (extraDraw) {
				extraHandler.renderWayFinished(canvas, zoomLevel, layer);
			}
		}
	}

    private void prepareWayShapeCircle(PaintContainerShape shapePaintContainer) {
        ContainerCircle circleContainer = (ContainerCircle) shapePaintContainer.shape;
        this.path.addCircle(
                circleContainer.x, circleContainer.y,
                circleContainer.radius,
                Path.Direction.CCW);
    }

    private void prepareWayShapePolyline(PaintContainerShape shapePaintContainer) {
        ContainerWay wayContainer = (ContainerWay) shapePaintContainer.shape;
        float[][] coordinates = wayContainer.coordinates;

        // iterate over all coordinates
		for (float[] coordinate : coordinates) {
			// make sure that the coordinates sequence is not empty
			float[] coords = coordinate;
			if (coords == null || coords.length <= 2) {
				continue;
			}

//			if (coords.length != 8) {
//				continue;
//			}

			// compute parallel path
			if (shapePaintContainer.vOffset != 0.0f) {
				coords = computeParallelPath(coords, shapePaintContainer.vOffset);
			}

			// iterate over lines based on curveStyle
			if (shapePaintContainer.curveStyle == CurveStyle.CUBIC) {
				// prepare variables
				float[] p1 = new float[]{coords[0], coords[1]};
				float[] p2 = new float[]{0.0f, 0.0f};
				float[] p3 = new float[]{0.0f, 0.0f};

				// add first point
				this.path.moveTo(p1[0], p1[1]);
				for (int i = 1; i < coords.length / 2; i++) {
					// get ending coordinates
					p3[0] = coords[2 * i];
					p3[1] = coords[2 * i + 1];
					p2[0] = (p1[0] + p3[0]) / 2.0f;
					p2[1] = (p1[1] + p3[1]) / 2.0f;

					// add spline over middle point and end on 'end' point
					this.path.quadTo(p1[0], p1[1], p2[0], p2[1]);

					// store end point as start point for next section
					p1[0] = p3[0];
					p1[1] = p3[1];
				}

				// add last segment
				this.path.quadTo(p2[0], p2[1], p3[0], p3[1]);
			} else {
				// construct line
				this.path.moveTo(coords[0], coords[1]);
				for (int i = 1; i < coords.length / 2; i++) {
					this.path.lineTo(coords[2 * i], coords[2 * i + 1]);
				}
			}
		}
    }

    /**
     * Computes a polyline with distance dy parallel to given coordinates.
     * http://objectmix.com/graphics/132987-draw-parallel-polyline-algorithm-needed.html
     */
    private static float[] computeParallelPath(float[] p, float dy) {
        int n = p.length - 2;
        float[] u = new float[n];
        float[] h = new float[p.length];

        // generate an array U[] of unity vectors of each direction
        for (int k = 0; k < n; k += 2) {
            float c = p[k + 2] - p[k];
            float s = p[k + 3] - p[k + 1];
            float l = (float) Math.sqrt(c * c + s * s);
            if (l == 0) {
                u[k] = 0;
                u[k + 1] = 0;
            } else {
                u[k] = c / l;
                u[k + 1] = s / l;
            }
        }

        // for the start point calculate the normal
        h[0] = p[0] - dy * u[1];
        h[1] = p[1] + dy * u[0];

        // for 1 to N-1 calculate the intersection of the offset lines
        for (int k = 2; k < n; k += 2) {
            float l = dy / (1 + u[k] * u[k - 2] + u[k + 1] * u[k - 1]);

			// reduce huge values on angles close to 180°
			if (l > 1000.0f) {
				l = 1000.0f;
			} else if (l < -1000.0f) {
				l = -1000.0f;
			}

			// compute intersection values
            h[k] = p[k] - l * (u[k + 1] + u[k - 1]);
            h[k + 1] = p[k + 1] + l * (u[k] + u[k - 2]);
        }

        // for the end point use the normal
        h[n] = p[n] - dy * u[n - 1];
        h[n + 1] = p[n + 1] + dy * u[n - 2];

        // return result
        return h;
    }
}
