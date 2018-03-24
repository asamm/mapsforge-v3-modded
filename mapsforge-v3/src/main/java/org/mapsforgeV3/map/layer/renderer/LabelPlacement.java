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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import org.mapsforgeV3.core.model.Tile;

import android.graphics.Paint.Align;

/**
 * This class place the labels for POIs, area labels and normal labels. The main target is avoiding collisions of these
 * different labels.
 */
class LabelPlacement {
	
	/**
	 * This class holds the reference positions for the two and four point greedy algorithms.
	 */
	static class ReferencePosition {
		final float height;
		final int nodeNumber;
		final float width;
		final float x;
		final float y;
		final PaintContainerPointText ptc;

		private ReferencePosition(float x, float y, int nodeNumber, float width, float height,
				PaintContainerPointText ptc) {
			this.x = x;
			this.y = y;
			this.nodeNumber = nodeNumber;
			this.width = width;
			this.height = height;
			this.ptc = ptc;
		}
	}

	private static final class ReferencePositionHeightComparator implements Comparator<ReferencePosition>, Serializable {
		private static final long serialVersionUID = 1L;
		static final ReferencePositionHeightComparator INSTANCE = new ReferencePositionHeightComparator();

		private ReferencePositionHeightComparator() {
			// do nothing
		}

		@Override
		public int compare(ReferencePosition x, ReferencePosition y) {
			if (x.ptc.priority != y.ptc.priority) {
				if (x.ptc.priority < y.ptc.priority) {
					return 1;
				} else if (x.ptc.priority > y.ptc.priority) {
					return -1;
				}
			}
			
			if (x.y - x.height < y.y - y.height) {
				return -1;
			}

			if (x.y - x.height > y.y - y.height) {
				return 1;
			}
			return 0;
		}
	}

	private static final class ReferencePositionYComparator implements Comparator<ReferencePosition>, Serializable {
		
		private static final long serialVersionUID = 1L;
		
		static final ReferencePositionYComparator INSTANCE = new ReferencePositionYComparator();

		private ReferencePositionYComparator() {
			// do nothing
		}

		@Override
		public int compare(ReferencePosition x, ReferencePosition y) {
			if (x.ptc.priority != y.ptc.priority) {
				if (x.ptc.priority < y.ptc.priority) {
					return 1;
				} else if (x.ptc.priority > y.ptc.priority) {
					return -1;
				}
			}

			if (x.y < y.y) {
				return -1;
			}

			if (x.y > y.y) {
				return 1;
			}

			return 0;
		}
	}

	private int labelDistanceToLabel = 2;
	private int labelDistanceToSymbol = 2;
	private int startDistanceToSymbols = 4;
	private int symbolDistanceToSymbol = 2;

	// size of tiles
	private final int mTileSize;
	// cache for dependency management
	final DependencyCache dependencyCache;
	// cached link to current handled label
	PaintContainerPointText label;
	ReferencePosition referencePosition;
	PaintContainerSymbol symbolContainer;

	LabelPlacement(int tileSize) {
		this.mTileSize = tileSize;
		this.dependencyCache = new DependencyCache(tileSize);
	}
	
	/**
	 * The inputs are all the label and symbol objects of the current tile. The output is overlap free label and symbol
	 * placement with the greedy strategy. The placement model is either the two fixed point or the four fixed point
	 * model.
	 * 
	 * @param labels
	 *            labels from the current tile.
	 * @param symbols
	 *            symbols of the current tile.
	 * @param areaLabels
	 *            area labels from the current tile.
	 * @param cT
	 *            current tile with the x,y- coordinates and the zoom level.
	 * @return the processed list of labels.
	 */
	List<PaintContainerPointText> placeLabels(List<PaintContainerPointText> labels,
			List<PaintContainerSymbol> symbols, List<PaintContainerPointText> areaLabels, Tile cT) {
		List<PaintContainerPointText> returnLabels = labels;
		this.dependencyCache.generateTileAndDependencyOnTile(cT);
//Logger.v("XXX", "placeLabels(), tile:" + cT.toString() + ", data:" + labels.size() + ", " + symbols.size() + ", " + areaLabels.size());
		// sort data by priority
		Collections.sort(returnLabels);
		Collections.sort(symbols);
		Collections.sort(areaLabels);

		// handle Area labels
		preprocessAreaLabels(areaLabels);

		// handle single labels
		preprocessLabels(returnLabels);

		// handle symbols
		preprocessSymbols(symbols);

		// remove labels that do not have drawn symbol
		removeEmptySymbolReferences(returnLabels, symbols);

		// remove symbols that goes over area symbols
		removeOverlappingSymbolsWithAreaLabels(symbols, areaLabels);

		// handle dependencyCache
		this.dependencyCache.removeOverlappingObjectsWithDependencyOnTile(returnLabels, areaLabels, symbols);
		if (!returnLabels.isEmpty()) {
			returnLabels = processFourPointGreedy(returnLabels, symbols, areaLabels);
		}
		this.dependencyCache.fillDependencyOnTile(returnLabels, symbols, areaLabels);
		return returnLabels;
	}

	/**************************************************/
	/*                 AREA LABELS                    */
	/**************************************************/
	
	private void preprocessAreaLabels(List<PaintContainerPointText> areaLabels) {
		// firstly center labels
		centerLabels(areaLabels);
		
		// remove labels that are out of tile
		removeOutOfTileTextContainers(areaLabels);

		// remove labels that overlap areas
		removeOverlappingAreaLabels(areaLabels);

		// handle dependency cache
		if (!areaLabels.isEmpty()) {
			this.dependencyCache.removeAreaLabelsInAlreadyDrawnAreas(areaLabels);
		}
	}
	
	/**
	 * Centers the labels.
	 * @param labels labels to center
	 */
	private void centerLabels(List<PaintContainerPointText> labels) {
		for (int i = 0, n = labels.size(); i < n; i++) {
			PaintContainerPointText label = labels.get(i);
			label.x = label.x - label.boundary.width() / 2;
		}
	}
	
	/**
	 * This method removes all the area labels, that overlap each other. So that the output is collision free
	 * @param areaLabels area labels from the actual tile
	 */
	private void removeOverlappingAreaLabels(List<PaintContainerPointText> areaLabels) {
		int dis = this.labelDistanceToLabel;
		usedAreaLabels.clear();

		for (int x = 0; x < areaLabels.size(); x++) {
			label = areaLabels.get(x);
			float r1x1 = label.x - dis;
			float r1y1 = label.y - dis;
			float r1x2 = label.x + label.boundary.width() + dis;
			float r1y2 = label.y + label.boundary.height() + dis; 

			usedAreaLabels.add(label.text);
			for (int y = x + 1; y < areaLabels.size(); y++) {
				if (y != x) {
					label = areaLabels.get(y);
					
					// remove overlapping areas or same texts
					if (intersects(r1x1, r1y1, r1x2, r1y2, 
							label.x, label.y,
							label.x + label.boundary.width(),
							label.y + label.boundary.height()) ||
							usedAreaLabels.contains(label.text)) {
						areaLabels.remove(y);
						y--;
					}
				}
			}
		}
	}
	
	private void preprocessLabels(List<PaintContainerPointText> labels) {
		// only remove labels out of tile
		removeOutOfTileTextContainers(labels);
		
		// firstly center labels
		centerLabels(labels);
	}

	/**************************************************/
	/*                    SYMBOLS                     */
	/**************************************************/
	
	private void preprocessSymbols(List<PaintContainerSymbol> symbols) {
		// remove symbols out of tile
		removeOutOfTileSymbols(symbols);
		
		// remove overlap symbols
		removeOverlappingSymbols(symbols);
		
		// handle dependency cache
		this.dependencyCache.removeSymbolsFromDrawnAreas(symbols);
	}

	/**
	 * This method removes the Symbols, that are not visible in the actual tile.
	 * @param symbols Symbols from the actual tile
	 */
	private void removeOutOfTileSymbols(List<PaintContainerSymbol> symbols) {
		for (int i = symbols.size() - 1; i >= 0; i--) {
			this.symbolContainer = symbols.get(i);

			if (this.symbolContainer.x > mTileSize) {
				symbols.remove(i);
			} else if (this.symbolContainer.y > mTileSize) {
				symbols.remove(i);
			} else if (this.symbolContainer.x + this.symbolContainer.symbol.getWidth() < 0.0f) {
				symbols.remove(i);
			} else if (this.symbolContainer.y + this.symbolContainer.symbol.getHeight() < 0.0f) {
				symbols.remove(i);
			}
		}
	}
	
	/**
	 * This method removes all the Symbols, that overlap each other. So that the output is collision free.
	 * @param symb symbols from the actual tile
	 */
	private void removeOverlappingSymbols(List<PaintContainerSymbol> symbols) {
		int dis = this.symbolDistanceToSymbol;

		for (int x = 0; x < symbols.size(); x++) {
			symbolContainer = symbols.get(x);
			
			// create border for new symbol
			float r1x1 = symbolContainer.x - dis;
			float r1y1 = symbolContainer.y - dis;
			float r1x2 = symbolContainer.x + symbolContainer.symbol.getWidth() + dis;
			float r1y2 = symbolContainer.y + symbolContainer.symbol.getHeight() + dis;
			
			for (int y = x + 1; y < symbols.size(); y++) {
				if (y != x) {
					symbolContainer = symbols.get(y);

					// check forceDraw action
					if (symbolContainer.forceDraw) {
						continue;
					}
					
					// test symbols
					if (intersects(r1x1, r1y1, r1x2, r1y2,
							symbolContainer.x, symbolContainer.y, 
							symbolContainer.x + symbolContainer.symbol.getWidth(), 
							symbolContainer.y + symbolContainer.symbol.getHeight())) {
						symbols.remove(y);
						y--;						
					}
				}
			}
		}
	}
	
	/**************************************************/
	/*                     TOOLS                      */
	/**************************************************/
	
	private void removeEmptySymbolReferences(List<PaintContainerPointText> nodes, List<PaintContainerSymbol> symbols) {
		for (int i = 0, n = nodes.size(); i < n; i++) {
			PaintContainerPointText node = nodes.get(i);
			if (!symbols.contains(node.symbol)) {
				node.symbol = null;
			}
		}
	}
	
	/**
	 * Removes the the symbols that overlap with area labels.
	 * @param symbols list of symbols
	 * @param pTC list of labels
	 */
	private void removeOverlappingSymbolsWithAreaLabels(List<PaintContainerSymbol> symbols,
			List<PaintContainerPointText> pTC) {
		int dis = this.labelDistanceToSymbol;

		int size = pTC.size();
		for (int x = 0; x < size; x++) {
			this.label = pTC.get(x);

			float r1x1 = label.x - dis;
			float r1y1 = label.y - label.boundary.height() - dis;
			float r1x2 = label.x + label.boundary.width() + dis;
			float r1y2 = label.y + dis;

			for (int y = symbols.size() - 1; y >= 0; y--) {
				this.symbolContainer = symbols.get(y);

				if (intersects(r1x1, r1y1, r1x2, r1y2, 
						symbolContainer.x, symbolContainer.y,
						symbolContainer.x + symbolContainer.symbol.getWidth(),
						symbolContainer.y + symbolContainer.symbol.getHeight())) {
					symbols.remove(y);
				}
			}
		}
	}
	
	/**
	 * This method removes the labels, that are not visible in the actual tile.
	 * @param labels Labels from the actual tile
	 */
	private void removeOutOfTileTextContainers(List<PaintContainerPointText> labels) {
		for (int i = labels.size() - 1; i >= 0; i--) {
			label = labels.get(i);

			boolean valid = true;
			if (label.paintFill.getTextAlign() == Align.LEFT) {
				if (label.x > mTileSize) {
					valid = false;
				} else if (label.x + label.boundary.width() < 0.0f) {
					valid = false;
				}	
			} else if (label.paintFill.getTextAlign() == Align.CENTER) {
				if (label.x - label.boundary.width() / 2 > mTileSize) {
					valid = false;
				} else if (label.x + label.boundary.width() / 2 < 0.0f) {
					valid = false;
				}
			} else if (label.paintFill.getTextAlign() == Align.RIGHT) {
				if (label.x - label.boundary.width() > mTileSize) {
					valid = false;
				} else if (label.x < 0.0f) {
					valid = false;
				}
			}
			
			// check result
			if (!valid) {
				labels.remove(i);
				continue;
			}
			
//			// test also Y axis - TODO not working perfectly
//			if (this.label.y - this.label.boundary.height() > mTileSize) {
//				labels.remove(i);
//				Logger.d("XXX", "invalid Y");
//			} else if (this.label.y < 0.0f) {
//				labels.remove(i);
//				Logger.d("XXX", "invalid Y");
//			}
//			} else if (label.y + label.boundary.top > mTileSize) {
//				labels.remove(i);
//			} else if (label.y - label.boundary.bottom < 0.0f) {
//				labels.remove(i);
//			}
		}
	}
	
	/**************************************************/
	/*                  FINAL TASK                    */
	/**************************************************/
	
	/**
	 * This method uses an adapted greedy strategy for the fixed four position model, above, under left and right form
	 * the point of interest. It uses no priority search tree, because it will not function with symbols only with
	 * points. Instead it uses two minimum heaps. They work similar to a sweep line algorithm but have not a O(n log n
	 * +k) runtime. To find the rectangle that has the top edge, I use also a minimum Heap. The rectangles are sorted by
	 * their y coordinates.
	 * 
	 * @param labels
	 *            label positions and text
	 * @param symbols
	 *            symbol positions
	 * @param areaLabels
	 *            area label positions and text
	 * @return list of labels without overlaps with symbols and other labels by the four fixed position greedy strategy
	 */
	private List<PaintContainerPointText> processFourPointGreedy(List<PaintContainerPointText> labels,
			List<PaintContainerSymbol> symbols, List<PaintContainerPointText> areaLabels) {
//Log.d("XXX", "start - labels:" + labels.size() + ", first:" + labels.get(0));
		List<PaintContainerPointText> resolutionSet = new ArrayList<PaintContainerPointText>();

		// Array for the generated reference positions around the points of interests
		ReferencePosition[] refPos = new ReferencePosition[(labels.size()) * 4];

		// lists that sorts the reference points after the minimum top edge y position
		PriorityQueue<ReferencePosition> priorUp = new PriorityQueue<ReferencePosition>(labels.size() * 4 * 2
				+ labels.size() / 10 * 2, ReferencePositionYComparator.INSTANCE);
		// lists that sorts the reference points after the minimum bottom edge y position
		PriorityQueue<ReferencePosition> priorDown = new PriorityQueue<ReferencePosition>(labels.size() * 4 * 2
				+ labels.size() / 10 * 2, ReferencePositionHeightComparator.INSTANCE);

		int dis = this.startDistanceToSymbols;

		// creates the reference positions
		for (int z = 0, size = labels.size(); z < size; z++) {
			PaintContainerPointText ptc = labels.get(z);
			if (ptc == null) {
				continue;
			}
			
			if (ptc.symbol != null) {
				// up
				refPos[z * 4] = new ReferencePosition(ptc.x - ptc.boundary.width() / 2, ptc.y
						- ptc.symbol.symbol.getHeight() / 2 - dis, z, ptc.boundary.width(), ptc.boundary.height(),
						ptc);
				// down
				refPos[z * 4 + 1] = new ReferencePosition(ptc.x - ptc.boundary.width() / 2, ptc.y
						+ ptc.symbol.symbol.getHeight() / 2 + ptc.boundary.height() + dis, z, ptc.boundary.width(),
						ptc.boundary.height(), ptc);
				// left
				refPos[z * 4 + 2] = new ReferencePosition(ptc.x - ptc.symbol.symbol.getWidth() / 2
						- ptc.boundary.width() - dis, ptc.y + ptc.boundary.height() / 2, z, ptc.boundary.width(),
						ptc.boundary.height(), ptc);
				// right
				refPos[z * 4 + 3] = new ReferencePosition(ptc.x + ptc.symbol.symbol.getWidth() / 2 + dis, ptc.y
						+ ptc.boundary.height() / 2 - 0.1f, z, ptc.boundary.width(), ptc.boundary.height(),
						ptc);
			} else {
				Align textAlign = ptc.paintFill.getTextAlign();
				float x = ptc.x; // Align.LEFT
				if (textAlign == Align.CENTER) {
					x = ptc.x - (ptc.boundary.width() / 2);
				} else if (textAlign == Align.RIGHT) {
					x = ptc.x - ptc.boundary.width();
				}
				refPos[z * 4] = new ReferencePosition(x, ptc.y, z, 
						ptc.boundary.width(), ptc.boundary.height(), ptc);
				refPos[z * 4 + 1] = null;
				refPos[z * 4 + 2] = null;
				refPos[z * 4 + 3] = null;
			}
		}

		// check data
		removeNonValidateReferencePosition(refPos, symbols, areaLabels);

		// do while it gives reference positions
		for (int i = 0; i < refPos.length; i++) {
			if (refPos[i] != null) {
//Log.d("XXX", " i:" + i + ", text:" + refPos[i].ptc.text + ", p:" + + refPos[i].ptc.priority);
				priorUp.add(refPos[i]);
				priorDown.add(refPos[i]);
			}
		}

		// handle data
		LinkedList<ReferencePosition> linkedRef = new LinkedList<ReferencePosition>();
		while (priorUp.size() != 0) {
			this.referencePosition = priorUp.remove();
			this.label = labels.get(this.referencePosition.nodeNumber);
			resolutionSet.add(referencePosition.ptc);

			// return result if no more data exits
			if (priorUp.size() == 0) {
				return resolutionSet;
			}

			// remove references
			priorUp.remove(refPos[this.referencePosition.nodeNumber * 4 + 0]);
			priorUp.remove(refPos[this.referencePosition.nodeNumber * 4 + 1]);
			priorUp.remove(refPos[this.referencePosition.nodeNumber * 4 + 2]);
			priorUp.remove(refPos[this.referencePosition.nodeNumber * 4 + 3]);

			priorDown.remove(refPos[this.referencePosition.nodeNumber * 4 + 0]);
			priorDown.remove(refPos[this.referencePosition.nodeNumber * 4 + 1]);
			priorDown.remove(refPos[this.referencePosition.nodeNumber * 4 + 2]);
			priorDown.remove(refPos[this.referencePosition.nodeNumber * 4 + 3]);

			linkedRef.clear();
			while (priorDown.size() != 0) {
				if (priorDown.peek().x < this.referencePosition.x + this.referencePosition.width) {
					linkedRef.add(priorDown.remove());
				} else {
					break;
				}
			}
			
			// brute Force collision test (faster then sweep line for a small amount of objects)
			for (int i = 0; i < linkedRef.size(); i++) {
				ReferencePosition rp = linkedRef.get(i);
				if ((rp.x <= this.referencePosition.x + this.referencePosition.width)
						&& (rp.y >= this.referencePosition.y - rp.height)
						&& (rp.y <= this.referencePosition.y + rp.height)) {
					priorUp.remove(rp);
					linkedRef.remove(i);
					i--;
				}
			}
			priorDown.addAll(linkedRef);
		}
//Log.d("XXX", "end - labels:" + resolutionSet.size() + ", first:" + resolutionSet.get(0));
		return resolutionSet;
	}

	/**
	 * The greedy algorithms need possible label positions, to choose the best among them. This method removes the
	 * reference points, that are not validate. Not validate means, that the Reference overlap with another symbol or
	 * label or is outside of the tile.
	 * 
	 * @param refPos
	 *            list of the potential positions
	 * @param symbols
	 *            actual list of the symbols
	 * @param areaLabels
	 *            actual list of the area labels
	 */
	private void removeNonValidateReferencePosition(ReferencePosition[] refPos, List<PaintContainerSymbol> symbols,
			List<PaintContainerPointText> areaLabels) {
		int dis = labelDistanceToSymbol;

		// remove texts above symbols
		for (int i = 0, m = symbols.size(); i < m; i++) {
			this.symbolContainer = symbols.get(i);
			
			float r1x1 = symbolContainer.x - dis;
			float r1y1 = symbolContainer.y - dis;
			float r1x2 = symbolContainer.x + symbolContainer.symbol.getWidth() + dis;
			float r1y2 = symbolContainer.y + symbolContainer.symbol.getHeight() + dis;
			
			for (int y = 0; y < refPos.length; y++) {
				if (refPos[y] != null && intersects(r1x1, r1y1, r1x2, r1y2,
						refPos[y].x, refPos[y].y - refPos[y].height,
						refPos[y].x + refPos[y].width, refPos[y].y) &&
						refPos[y].ptc.priority < symbolContainer.priority) {
					refPos[y] = null;
				}
			}
		}

		dis = labelDistanceToLabel;

		for (int i = 0, m = areaLabels.size(); i < m; i++) {
			PaintContainerPointText areaLabel = areaLabels.get(i);
//Log.d("XXX", " testAreaLabel:" + areaLabel.text + ", p:" + areaLabel.priority);

			float r1x1 = areaLabel.x - dis;
			float r1y1 = areaLabel.y - areaLabel.boundary.height() - dis;
			float r1x2 = areaLabel.x + areaLabel.boundary.width() + dis;
			float r1y2 = areaLabel.y + dis;

			for (int y = 0; y < refPos.length; y++) {
				if (refPos[y] == null) {
					continue;
				}
				if (intersects(r1x1, r1y1, r1x2, r1y2,
						refPos[y].x, refPos[y].y - refPos[y].height,
						refPos[y].x + refPos[y].width, refPos[y].y) &&
						refPos[y].ptc.priority < areaLabel.priority) {
					refPos[y] = null;
				}
			}
		}

		this.dependencyCache.removeReferencePointsFromDependencyCache(refPos);
	}

	private ArrayList<String> usedAreaLabels = new ArrayList<String>();

	private static boolean intersects(float r1x1, float r1y1, float r1x2, float r1y2, 
			float r2x1, float r2y1, float r2x2, float r2y2) {
        return r1x1 < r2x2 && r2x1 < r1x2 && r1y1 < r2y2 && r2y1 < r1y2;
    }
}
