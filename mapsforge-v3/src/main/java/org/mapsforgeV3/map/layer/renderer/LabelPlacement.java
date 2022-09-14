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

import android.graphics.Paint.Align;

import com.asamm.locus.mapsforge.utils.Utils;

import org.mapsforgeV3.core.model.Tile;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * This class place the labels for POIs, area labels and normal labels. The main target is avoiding collisions of these
 * different labels.
 */
class LabelPlacement {

    /**
     * Tag for logger.
     */
    private static final String TAG = "LabelPlacement";

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

    private final int labelDistanceToLabel = 2;
    private final int labelDistanceToSymbol = 2;
    private final int startDistanceToSymbols = 4;
    private final int symbolDistanceToSymbol = 2;

    // size of tiles
    private final int tileSize;
    // cache for dependency management
    private final DependencyCache dependencyCache;

    // cached link to current handled label
    PaintContainerPointText label;
    ReferencePosition referencePosition;
    PaintContainerSymbol symbolContainer;

    LabelPlacement(int tileSize) {
        this.tileSize = tileSize;
        this.dependencyCache = new DependencyCache(tileSize);
    }

    /**
     * The inputs are all the label and symbol objects of the current tile. The output is overlap free label and symbol
     * placement with the greedy strategy. The placement model is either the two fixed point or the four fixed point
     * model.
     *
     * @param nodes     labels from the current tile.
     * @param symbols    symbols of the current tile.
     * @param areaLabels area labels from the current tile.
     * @param cT         current tile with the x,y- coordinates and the zoom level.
     * @return the processed list of labels.
     */
    List<PaintContainerPointText> placeLabels(List<PaintContainerPointText> nodes,
            List<PaintContainerSymbol> symbols, List<PaintContainerPointText> areaLabels, Tile cT) {
        List<PaintContainerPointText> nodesFinal = nodes;
        this.dependencyCache.generateTileAndDependencyOnTile(cT);

        // sort data by priority
        Collections.sort(nodesFinal);
        Collections.sort(symbols);
        Collections.sort(areaLabels);
//        Utils.getHandler().logD(TAG, "placeLabels(" +
//                nodes.size() + ", " + symbols.size() + ", " + areaLabels.size() + ", " + cT + ")");

        // handle Area labels
        preprocessAreaLabels(areaLabels);

        // handle single labels
        preprocessNodes(nodesFinal);

        // handle symbols
        preprocessSymbols(symbols);

        // remove labels that do not have drawn symbol
        removeEmptySymbolReferences(nodesFinal, symbols);

        // remove symbols that goes over area symbols
        removeOverlappingSymbolsWithAreaLabels(symbols, areaLabels);

        // handle dependencyCache
        this.dependencyCache.removeOverlappingObjectsWithDependencyOnTile(nodesFinal, areaLabels, symbols);
        if (!nodesFinal.isEmpty()) {
            nodesFinal = processFourPointGreedy(nodesFinal, symbols, areaLabels);
        }
        this.dependencyCache.fillDependencyOnTile(nodesFinal, symbols, areaLabels);
        return nodesFinal;
    }

    //*************************************************
    // AREA LABELS
    //*************************************************

    private void preprocessAreaLabels(List<PaintContainerPointText> areaLabels) {
        // center labels
        centerLabels(areaLabels);

        // remove labels that are out of tile
        removeOutOfTileLabels(areaLabels);

        // remove labels that overlap areas
        removeOverlappingAreaLabels(areaLabels);

        // handle dependency cache
        if (!areaLabels.isEmpty()) {
            this.dependencyCache.removeAreaLabelsInAlreadyDrawnAreas(areaLabels);
        }
    }

    // temporary cache for testing area labels
    private final ArrayList<String> usedAreaLabels = new ArrayList<>();

    /**
     * This method removes all the area labels, that overlap each other. So that the output is collision free
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

                    // remove same texts
                    if (usedAreaLabels.contains(label.text)) {
                        areaLabels.remove(y);
                        y--;
                        continue;
                    }

                    // check forceDraw action
                    if (label.forceDraw) {
                        continue;
                    }

                    // remove overlapping areas
                    if (intersects(r1x1, r1y1, r1x2, r1y2,
                            label.x, label.y,
                            label.x + label.boundary.width(),
                            label.y + label.boundary.height())) {
                        areaLabels.remove(y);
                        y--;
                    }
                }
            }
        }
    }

    //*************************************************
    // NODES
    //*************************************************

    private void preprocessNodes(List<PaintContainerPointText> labels) {
        // center labels
        centerLabels(labels);

        // remove labels that are out of tile
        removeOutOfTileLabels(labels);
    }

    //*************************************************
    // SYMBOLS
    //*************************************************

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
     *
     * @param symbols Symbols from the actual tile
     */
    private void removeOutOfTileSymbols(List<PaintContainerSymbol> symbols) {
        for (int i = symbols.size() - 1; i >= 0; i--) {
            this.symbolContainer = symbols.get(i);

            if (this.symbolContainer.x > tileSize) {
                symbols.remove(i);
            } else if (this.symbolContainer.y > tileSize) {
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
     *
     * @param symbols from the actual tile
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

    //*************************************************
    // TOOLS
    //*************************************************

    /**
     * Centers the labels.
     */
    private void centerLabels(List<PaintContainerPointText> labels) {
        for (int i = 0, n = labels.size(); i < n; i++) {
            PaintContainerPointText label = labels.get(i);
            label.x = label.x - label.boundary.width() / 2.0f;
        }
    }

    /**
     * This method removes the labels, that are not visible in the actual tile.
     */
    private void removeOutOfTileLabels(List<PaintContainerPointText> labels) {
        for (int i = labels.size() - 1; i >= 0; i--) {
            label = labels.get(i);

            boolean valid = true;
            if (label.paintFill.getTextAlign() == Align.LEFT) {
                if (label.x > tileSize) {
                    valid = false;
                } else if (label.x + label.boundary.width() < 0.0f) {
                    valid = false;
                }
            } else if (label.paintFill.getTextAlign() == Align.CENTER) {
                if (label.x - label.boundary.width() / 2.0f > tileSize) {
                    valid = false;
                } else if (label.x + label.boundary.width() / 2.0f < 0.0f) {
                    valid = false;
                }
            } else if (label.paintFill.getTextAlign() == Align.RIGHT) {
                if (label.x - label.boundary.width() > tileSize) {
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

    private void removeEmptySymbolReferences(List<PaintContainerPointText> labels, List<PaintContainerSymbol> symbols) {
        for (int i = 0, n = labels.size(); i < n; i++) {
            PaintContainerPointText label = labels.get(i);
            if (!symbols.contains(label.symbol)) {
                label.symbol = null;
            }
        }
    }

    /**
     * Removes the the symbols that overlap with area labels.
     *
     * @param symbols list of symbols
     * @param pTC     list of labels
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

                // check forceDraw action
                if (symbolContainer.forceDraw) {
                    continue;
                }

                if (intersects(r1x1, r1y1, r1x2, r1y2,
                        symbolContainer.x, symbolContainer.y,
                        symbolContainer.x + symbolContainer.symbol.getWidth(),
                        symbolContainer.y + symbolContainer.symbol.getHeight())) {
                    symbols.remove(y);
                }
            }
        }
    }

    //*************************************************
    // FINAL TASK
    //*************************************************

    /**
     * This method uses an adapted greedy strategy for the fixed four position model, above, under left and right form
     * the point of interest. It uses no priority search tree, because it will not function with symbols only with
     * points. Instead it uses two minimum heaps. They work similar to a sweep line algorithm but have not a O(n log n
     * +k) runtime. To find the rectangle that has the top edge, I use also a minimum Heap. The rectangles are sorted by
     * their y coordinates.
     *
     * @param nodes     label positions and text
     * @param symbols    symbol positions
     * @param areaLabels area label positions and text
     * @return list of labels without overlaps with symbols and other labels by the four fixed position greedy strategy
     */
    private List<PaintContainerPointText> processFourPointGreedy(List<PaintContainerPointText> nodes,
            List<PaintContainerSymbol> symbols, List<PaintContainerPointText> areaLabels) {
        List<PaintContainerPointText> resolutionSet = new ArrayList<>();

        // Array for the generated reference positions around the points of interests
        ReferencePosition[] refPos = new ReferencePosition[(nodes.size()) * 4];

        // lists that sorts the reference points after the minimum top edge y position
        PriorityQueue<ReferencePosition> priorUp = new PriorityQueue<>(nodes.size() * 4 * 2
                + nodes.size() / 10 * 2, ReferencePositionYComparator.INSTANCE);
        // lists that sorts the reference points after the minimum bottom edge y position
        PriorityQueue<ReferencePosition> priorDown = new PriorityQueue<>(nodes.size() * 4 * 2
                + nodes.size() / 10 * 2, ReferencePositionHeightComparator.INSTANCE);

        int dis = this.startDistanceToSymbols;

        // creates the reference positions
        for (int z = 0, size = nodes.size(); z < size; z++) {
            PaintContainerPointText ptc = nodes.get(z);
            if (ptc == null) {
                continue;
            }

            if (ptc.symbol != null) {
                // up
                refPos[z * 4] = new ReferencePosition(ptc.x - ptc.boundary.width() / 2.0f, ptc.y
                        - ptc.symbol.symbol.getHeight() / 2.0f - dis, z, ptc.boundary.width(), ptc.boundary.height(),
                        ptc);
                // down
                refPos[z * 4 + 1] = new ReferencePosition(ptc.x - ptc.boundary.width() / 2.0f, ptc.y
                        + ptc.symbol.symbol.getHeight() / 2.0f + ptc.boundary.height() + dis, z, ptc.boundary.width(),
                        ptc.boundary.height(), ptc);
                // left
                refPos[z * 4 + 2] = new ReferencePosition(ptc.x - ptc.symbol.symbol.getWidth() / 2.0f
                        - ptc.boundary.width() - dis, ptc.y + ptc.boundary.height() / 2.0f, z, ptc.boundary.width(),
                        ptc.boundary.height(), ptc);
                // right
                refPos[z * 4 + 3] = new ReferencePosition(ptc.x + ptc.symbol.symbol.getWidth() / 2.0f + dis, ptc.y
                        + ptc.boundary.height() / 2.0f - 0.1f, z, ptc.boundary.width(), ptc.boundary.height(),
                        ptc);
            } else {
                Align textAlign = ptc.paintFill.getTextAlign();
                float x = ptc.x; // Align.LEFT
                if (textAlign == Align.CENTER) {
                    x = ptc.x - (ptc.boundary.width() / 2.0f);
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
        for (ReferencePosition refPo : refPos) {
            if (refPo != null) {
                priorUp.add(refPo);
                priorDown.add(refPo);
            }
        }

        // handle data
        LinkedList<ReferencePosition> linkedRef = new LinkedList<>();
        while (priorUp.size() != 0) {
            this.referencePosition = priorUp.remove();
            this.label = nodes.get(this.referencePosition.nodeNumber);
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
        return resolutionSet;
    }

    /**
     * The greedy algorithms need possible label positions, to choose the best among them. This method removes the
     * reference points, that are not validate. Not validate means, that the Reference overlap with another symbol or
     * label or is outside of the tile.
     *
     * @param refPos     list of the potential positions
     * @param symbols    actual list of the symbols
     * @param areaLabels actual list of the area labels
     */
    private void removeNonValidateReferencePosition(ReferencePosition[] refPos, List<PaintContainerSymbol> symbols,
            List<PaintContainerPointText> areaLabels) {
        // remove node labels above symbols
        int dis = labelDistanceToSymbol;
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

        // remove node labels above the area labels
        dis = labelDistanceToLabel;
        for (int i = 0, m = areaLabels.size(); i < m; i++) {
            PaintContainerPointText areaLabel = areaLabels.get(i);

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

        // remove node labels that overlap already draw tiles
        this.dependencyCache.removeReferencePointsFromDependencyCache(refPos);
    }

    private static boolean intersects(float r1x1, float r1y1, float r1x2, float r1y2,
            float r2x1, float r2y1, float r2x2, float r2y2) {
        return r1x1 < r2x2 && r2x1 < r1x2 && r1y1 < r2y2 && r2y1 < r1y2;
    }
}
