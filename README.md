# Locus LoMaps themes

LoMaps are vector based map developed especially for application [Locus Map](http://www.locusmap.eu). This repository contains mapping XML file used generation LoMaps maps and internal themes used for rendering maps in application. LoMaps are created based on [Mapsforge](https://github.com/mapsforge/mapsforge) project

## LoMaps - theme configuration

Principles for render themes are well described in original [Mapsforge documenation](https://github.com/mapsforge/mapsforge/blob/master/docs/Rendertheme.md). However Locus uses set of custom (not official) tags.

### Supported tags
#### area
*  src
*  fill
*  stroke
*  stroke-width
#### caption
*  k
*  dx
*  dy
*  font-family
*  font-style
*  font-size
*  fill
*  stroke
*  stroke-width
*  upper-case
*  scale-font-size
*  bg-rect-fill
*  bg-rect-stroke
*  bg-rect-over
*  bg-rect-stroke-width
*  bg-rect-rounded
*  priority
*  force-draw
#### circle
*  r
*  scale-radius
*  fill
*  stroke
*  stroke-width
#### line
*  curve
*  dy
*  src
*  stroke
*  stroke-width
*  stroke-dasharray
*  stroke-linecap
#### lineSymbol
*  src
*  align-center
*  repeat
*  repeat-gap
*  scale
*  scale-icon-size
#### pathText
*  k
*  font-family
*  font-style
*  font-size
*  fill
*  stroke
*  stroke-width
*  dx
*  dy
*  upper-case
*  rotate_up
*  scale-font-size
*  bg-rect-fill
*  bg-rect-stroke
*  bg-rect-over
*  bg-rect-stroke-width
*  bg-rect-rounded
#### symbol
*  src
*  scale
*  scale-icon-size
*  symbol-width
*  symbol-height
*  priority
*  force-draw

### Detailed information about tags
#### curve
Adds possibility to draw line extrapolated as a curve. It helps in some situations to create "smooth" lines:
*  value: "cubic"
*  example: `<line stroke="#A07F5F" stroke-width="0.08" curve="cubic"/>`
#### bg-rect-*
Background rectangle is very useful method of drawing a nicely looking rectangle as a background of texts.
*  bg-rect-fill
      * more in **fill**  attribute
*  bg-rect-stroke
      * more in **stroke**  attribute
*  bg-rect-over
      * space between text and border
      * value: decimal number
*  bg-rect-stroke-width
      * more in **stroke-width**
*  bg-rect-rounded
      * allow to round corners of bounding rectgangle
      * value: decimal number
*  used in: caption, pathText
#### fill
*  Fills colours of items.
*  value: colour
#### force-draw
*  useful method of drawing an icon no matter if others overlay it or not
*  value: boolean
#### rotate_up
*  default value ''rotate_up="true"''  automatically rotates text up. It mean that path text doesn't follow direction of the way but it's automatically rotate to the readable direction. This is not ideal for contour lines where text should follow the gradient. Set to ''rotate_up="true"''  and next will be in the same direction as direction of the way
#### scale
*  Scale symbol for drawing
*  default: 1.0f
*  value: decimal number
#### scale-icon-size
*  Allows to scale a symbol based on current zoom level. Very useful method of increasing the size of symbols on map when you change the zoom level. Method is defined by two values separated by comma, where 1st value is base zoom level, 2nd value is exponent. Base scale value is from attribute **scale**.
*  example: ''scale="1.5" scale-icon-size="12,1.1"''
      * for zoom levels 0 - 11, scale: **1.5**
      * for zoom level 12, scale: 1.5 * 1.0 = **1.5**
      * for zoom level 13, scale:
        * zoom level difference: 1
        * scale: 1.5 * 1.1 = **1.65**
      * for zoom level 15, scale:
        * zoom level difference: 3
        * scale: 1.5 * 1.1 * 1.1 * 1.1 = **2.00**
#### stroke
*  Stroke colours for items.
*  value: colour
#### stroke-width
*  Width of stroke color
*  value: decimal number
#### symbol-width, symbol-height
enables to resize the symbol icon to specific width or height
*  value: decimal number
#### upper-case
*  allows to display texts in upper-case
*  used in: caption, pathText
*  value: boolean

### Possible values
#### boolean
Boolean value may be written as text ''true'', ''false'' or as number ''1'' (as ''true''), ''0'' (as ''false'')
#### colour
Supported formats are: #RRGGBB #AARRGGBB 'red', 'blue', 'green', 'black', 'white', 'gray', 'cyan', 'magenta', 'yellow', 'lightgray', 'darkgray'
#### decimal number
Common decimal number
