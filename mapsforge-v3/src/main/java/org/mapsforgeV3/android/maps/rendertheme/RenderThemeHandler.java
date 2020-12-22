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
package org.mapsforgeV3.android.maps.rendertheme;

import android.util.Log;

import com.asamm.locus.mapsforge.utils.Utils;

import org.mapsforgeV3.android.maps.mapgenerator.RenderThemeDefinition;
import org.mapsforgeV3.android.maps.rendertheme.renderinstruction.Area;
import org.mapsforgeV3.android.maps.rendertheme.renderinstruction.Caption;
import org.mapsforgeV3.android.maps.rendertheme.renderinstruction.Circle;
import org.mapsforgeV3.android.maps.rendertheme.renderinstruction.Line;
import org.mapsforgeV3.android.maps.rendertheme.renderinstruction.LineSymbol;
import org.mapsforgeV3.android.maps.rendertheme.renderinstruction.PathText;
import org.mapsforgeV3.android.maps.rendertheme.renderinstruction.RenderInstruction;
import org.mapsforgeV3.android.maps.rendertheme.renderinstruction.Symbol;
import org.mapsforgeV3.android.maps.rendertheme.rules.Rule;
import org.mapsforgeV3.android.maps.rendertheme.rules.RuleBuilder;
import org.mapsforgeV3.core.util.IOUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

/**
 * SAX2 handler to parse XML render theme files.
 */
public final class RenderThemeHandler extends DefaultHandler {

    // tag for logger
    private static final String TAG = "RenderThemeHandler";

    private enum Element {
		RENDER_THEME,
        RENDERING_INSTRUCTION,
        RULE,
        RENDERING_STYLE
	}

	private static final String ELEMENT_NAME_RENDER_THEME = "rendertheme";
	private static final String ELEMENT_NAME_RULE = "rule";
    private static final String ELEMENT_NAME_STYLE_MENU = "stylemenu";
	private static final String UNEXPECTED_ELEMENT = "unexpected element: ";

    // STATIC TOOLS

    public interface OnThemePreparationListener {

        /**
         * Handler for styling vector themes. Allows to define which categories will be visible
         * and which hidden.
         * @param menuStyle style from theme
         * @return list of allowed categories
         */
        Set<String> getThemeCategories(XmlRenderThemeStyleMenu menuStyle);
    }

	/**
     * Generate render theme from XML configuration.
	 * @param jobTheme the JobTheme to create a RenderTheme from
	 * @return a new RenderTheme which is created by parsing the XML data from the input stream.
	 * @throws SAXException if an error occurs while parsing the render theme XML.
	 * @throws ParserConfigurationException if an error occurs while creating the XML parser.
	 * @throws IOException if an I/O error occurs while reading from the input stream.
	 */
	public static RenderTheme getRenderTheme(RenderThemeDefinition jobTheme, OnThemePreparationListener listener)
			throws SAXException, ParserConfigurationException, IOException {
		RenderThemeHandler renderThemeHandler = new RenderThemeHandler(
				jobTheme.getRelativePathPrefix(), jobTheme.getThemeStyle(), listener);
		XMLReader xmlReader = SAXParserFactory.newInstance().
				newSAXParser().getXMLReader();
		xmlReader.setContentHandler(renderThemeHandler);
		InputStream inputStream = null;
		try {
			inputStream = jobTheme.getAsStream();
			xmlReader.parse(new InputSource(inputStream));
			return renderThemeHandler.mRenderTheme;
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}

	/**
	 * Logs the given information about an unknown XML attribute.
	 * @param element the XML element name.
	 * @param name the XML attribute name.
	 * @param value the XML attribute value.
	 * @param attributeIndex the XML attribute index position.
	 */
	public static void logUnknownAttribute(String element, String name,
			String value, int attributeIndex) {
        // use only for debug
//        Utils.getHandler().logI(TAG, "unknown attribute in element '" + element +
//                "' (" + attributeIndex + "): " + name + '=' + value);
	}

    // CORE PARAMETERS

    // parsed theme
    private RenderTheme mRenderTheme;
	// relative path to theme data
	private final String mRelativePathPrefix;
	// special theme argument
	private final String mThemeStyle;
	// listener for parsing
    private final OnThemePreparationListener themePrepareListener;

    // PARAMETERS FOR RENDERING

    // current parsed rule
	private Rule mCurrentRule;
	// stack for parsed rules
	private final Stack<Rule> mRuleStack = new Stack<>();

    // PARAMETERS FOR MENUS

    private XmlRenderThemeStyleMenu mRenderThemeStyleMenu;
    private XmlRenderThemeStyleLayer mCurrentLayer;
    private Set<String> mCategories;

    // TEMPORARY PARAMETERS

    // stack for elements
    private final Stack<Element> mElementStack = new Stack<>();
    // current parsing level
    private int mLevel;
    // index counter used as ID
    private int mTagIndex;
    // reusable attribute container
    private final HashMap<String, String> mAttrs = new HashMap<>();

    // amount of all instructions
    private int mCountRulesAll;
    // amount of already added rules
    private int mCountRulesAdded;
    // amount of all instructions
    private int mCountInstructionsAll;
    // amount of already added rules
    private int mCountInstructionsAdded;

    /**
     * Private constructor for theme generator.
     * @param relativePathPrefix relative path to theme
     * @param themeStyle name of theme style (optional)
     */
	private RenderThemeHandler(String relativePathPrefix, String themeStyle, OnThemePreparationListener listener) {
		super();
        Log.d(TAG, "RenderThemeHandler(" + relativePathPrefix + ", " + themeStyle + ")");
        this.mRelativePathPrefix = relativePathPrefix;
		this.mThemeStyle = themeStyle;
        this.themePrepareListener = listener;
        this.mTagIndex = 0;
        this.mCountRulesAll = 0;
        this.mCountRulesAdded = 0;
		this.mCountInstructionsAll = 0;
        this.mCountInstructionsAdded = 0;
	}

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        try {
            // increase index
            mTagIndex++;

            // handle tag
            switch (localName) {
                case ELEMENT_NAME_RENDER_THEME:
                    checkState(localName, Element.RENDER_THEME);
                    this.mRenderTheme = RenderTheme.create(localName, attributes);
                    break;
                case ELEMENT_NAME_RULE:
                    checkState(localName, Element.RULE);
                    Rule rule = new RuleBuilder(qName, attributes, this.mRuleStack).build();
                    if (!this.mRuleStack.empty() && isVisible(rule)) {
                        this.mCurrentRule.addSubRule(rule);
                        this.mCountRulesAdded++;
                    }
                    this.mCurrentRule = rule;
                    this.mRuleStack.push(this.mCurrentRule);
                    break;
                case "area":
                    checkState(localName, Element.RENDERING_INSTRUCTION);
                    Area area = Area.create(mTagIndex,
                            convertAttributesToMap(attributes),
                            this.mLevel++, this.mRelativePathPrefix);
                    if (isVisible(area)) {
                        this.mRuleStack.peek().addRenderingInstruction(area);
                        this.mCountInstructionsAdded++;
                    }
                    break;
                case "caption":
                    checkState(localName, Element.RENDERING_INSTRUCTION);
                    Caption caption = Caption.create(mTagIndex, localName,
                            convertAttributesToMap(attributes));
                    if (isVisible(caption)) {
                        this.mCurrentRule.addRenderingInstruction(caption);
                        this.mCountInstructionsAdded++;
                    }
                    break;
                case "cat":
                    checkState(qName, Element.RENDERING_STYLE);
                    this.mCurrentLayer.addCategory(attributes.getValue("id"));
                    break;
                case "circle":
                    checkState(localName, Element.RENDERING_INSTRUCTION);
                    Circle circle = Circle.create(mTagIndex,
                            convertAttributesToMap(attributes), this.mLevel++);
                    if (isVisible(circle)) {
                        this.mCurrentRule.addRenderingInstruction(circle);
                        this.mCountInstructionsAdded++;
                    }
                    break;
                case "layer":
                    checkState(qName, Element.RENDERING_STYLE);

                    // get 'enabled' state
                    boolean enabled = false;
                    if (attributes.getValue("enabled") != null) {
                        enabled = Boolean.valueOf(attributes.getValue("enabled"));
                    }

                    // get 'visible' state
                    boolean visible = Boolean.valueOf(attributes.getValue("visible"));

                    // generate new layer
                    this.mCurrentLayer = this.mRenderThemeStyleMenu.createLayer(
                            attributes.getValue("id"), visible, enabled);
                    String parent = attributes.getValue("parent");
                    if (null != parent) {
                        XmlRenderThemeStyleLayer parentEntry = this.mRenderThemeStyleMenu.getLayer(parent);
                        if (null != parentEntry) {
                            for (String cat : parentEntry.getCategories()) {
                                this.mCurrentLayer.addCategory(cat);
                            }
                            for (XmlRenderThemeStyleLayer overlay : parentEntry.getOverlays()) {
                                this.mCurrentLayer.addOverlay(overlay);
                            }
                        }
                    }
                    break;
                case "line":
                    checkState(localName, Element.RENDERING_INSTRUCTION);
                    Line line = Line.create(mTagIndex, localName,
                            convertAttributesToMap(attributes),
                            this.mLevel++, this.mRelativePathPrefix);
                    if (isVisible(line)) {
                        this.mCurrentRule.addRenderingInstruction(line);
                        this.mCountInstructionsAdded++;
                    }
                    break;
                case "lineSymbol":
                    checkState(localName, Element.RENDERING_INSTRUCTION);
                    LineSymbol lineSymbol = LineSymbol.create(mTagIndex, localName,
                            convertAttributesToMap(attributes), this.mRelativePathPrefix);
                    if (isVisible(lineSymbol)) {
                        this.mCurrentRule.addRenderingInstruction(lineSymbol);
                        this.mCountInstructionsAdded++;
                    }
                    break;
                case "name":
                    checkState(qName, Element.RENDERING_STYLE);
                    this.mCurrentLayer.addTranslation(
                            attributes.getValue("lang"),
                            attributes.getValue("value"));
                    break;
                case "overlay":
                    checkState(qName, Element.RENDERING_STYLE);
                    XmlRenderThemeStyleLayer overlay =
                            this.mRenderThemeStyleMenu.getLayer(attributes.getValue("id"));
                    if (overlay != null) {
                        this.mCurrentLayer.addOverlay(overlay);
                    }
                    break;
                case "pathText":
                    checkState(localName, Element.RENDERING_INSTRUCTION);
                    PathText pathText = PathText.create(mTagIndex, localName,
                            convertAttributesToMap(attributes));
                    if (isVisible(pathText)) {
                        this.mCurrentRule.addRenderingInstruction(pathText);
                        this.mCountInstructionsAdded++;
                    }
                    break;
                case ELEMENT_NAME_STYLE_MENU:
                    checkState(qName, Element.RENDERING_STYLE);
                    this.mRenderThemeStyleMenu = new XmlRenderThemeStyleMenu(
                            attributes.getValue("id"),
                            attributes.getValue("defaultlang"),
                            attributes.getValue("defaultvalue"));
                    break;
                case "symbol":
                    checkState(localName, Element.RENDERING_INSTRUCTION);
                    Symbol symbol = Symbol.create(mTagIndex, localName,
                            convertAttributesToMap(attributes),
                            this.mRelativePathPrefix);
                    if (isVisible(symbol)) {
                        this.mCurrentRule.addRenderingInstruction(symbol);
                        this.mCountInstructionsAdded++;
                    }
                    break;
                default:
                    throw new SAXException("unknown element: " + localName);
            }

            // report about remaining attributes
            logUnknownAttributes(localName, mAttrs);
        } catch (IllegalArgumentException | IOException e) {
            throw new SAXException(null, e);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        this.mElementStack.pop();

        // add rule to theme
        if (ELEMENT_NAME_RULE.equals(localName)) {
            this.mRuleStack.pop();
            if (this.mRuleStack.empty()) {
                // check if cure is visible (MapsForge 0.4 system)
                if (isVisible(this.mCurrentRule)) {
                    // check if rule is valid (Locus system)
                    Rule validatedRule = this.mCurrentRule.validateRule(mThemeStyle);
                    if (validatedRule != null) {
                        this.mRenderTheme.addRule(validatedRule);
                    }
                }
            } else {
                this.mCurrentRule = this.mRuleStack.peek();
            }
        } else if (ELEMENT_NAME_STYLE_MENU.equals(localName)) {
            // when we are finished parsing the menu part of the file, we can get the
            // categories to render from the initiator. This allows the creating action
            // to select which of the menu options to choose
            this.mCategories = themePrepareListener.getThemeCategories(mRenderThemeStyleMenu);
        }
    }

	@Override
	public void endDocument() {
		if (this.mRenderTheme == null) {
			throw new IllegalArgumentException("missing element: rules");
		}

		this.mRenderTheme.setLevels(this.mLevel);
		this.mRenderTheme.complete();

//        // print results
//        Utils.getHandler().logD(TAG, "Prepare render theme, " +
//                "rules:" + mCountRulesAdded + " / " + mCountRulesAll + ", " +
//                "instr:" + mCountInstructionsAdded + " / " + mCountInstructionsAll +
//                " (tags:" + mTagIndex + ")");
	}

    @Override
    public void warning(SAXParseException exception) {
        Utils.getHandler().logE(TAG, "warning()", exception);
    }

    @Override
	public void error(SAXParseException exception) {
        Utils.getHandler().logE(TAG, "error()", exception);
	}

    private HashMap<String, String> convertAttributesToMap(Attributes attributes) {
        // place attributes to a map
        mAttrs.clear();
        for (int i = 0, m = attributes.getLength(); i < m; i++) {
            mAttrs.put(attributes.getLocalName(i), attributes.getValue(i));
        }

        // return new map
        return mAttrs;
    }

    /**
     * Iterate over all remaining attributes and report they are not used.
     * @param elementName current element name
     * @param attrs list of attributes
     */
    private void logUnknownAttributes(String elementName, HashMap<String, String> attrs) {
        // check attributes
        if (attrs == null || attrs.size() == 0) {
            return;
        }

        // iterate over remaining attributes
        for (String key : attrs.keySet()) {
            String value = attrs.get(key);
            RenderThemeHandler.logUnknownAttribute(elementName, key, value, 0);
        }
    }

    private void checkState(String elementName, Element element)
            throws SAXException {
        checkElement(elementName, element);
        this.mElementStack.push(element);
    }

    /**
     * Check if certain element is valid.
     * @param elementName name of element
     * @param element expected type
     * @throws SAXException
     */
	private void checkElement(String elementName, Element element)
			throws SAXException {
		switch (element) {
            case RENDER_THEME:
                if (!this.mElementStack.empty()) {
                    throw new SAXException(UNEXPECTED_ELEMENT + elementName);
                }
                return;

            case RULE:
                Element parentElement = this.mElementStack.peek();
                if (parentElement != Element.RENDER_THEME
                        && parentElement != Element.RULE) {
                    throw new SAXException(UNEXPECTED_ELEMENT + elementName);
                }
                return;

            case RENDERING_INSTRUCTION:
                if (this.mElementStack.peek() != Element.RULE) {
                    throw new SAXException(UNEXPECTED_ELEMENT + elementName);
                }
                return;
            case RENDERING_STYLE:
                return;
		}

        // throw exception in this case
		throw new SAXException("unknown enum value: " + element);
	}

    /**
     * Check if certain instruction is enabled by user.
     * @param instruction instruction to test
     * @return <code>true</code> if enabled
     */
    private boolean isVisible(RenderInstruction instruction) {
        // increase instructions counter
        mCountInstructionsAll++;

        // skip invalid options
        if (this.mCategories == null || instruction.getCategory() == null) {
            return true;
        }

        // test instruction
        return this.mCategories.contains(instruction.getCategory());
    }

    /**
     * Check if certain rule is enabled by user.
     * @param rule instruction to test
     * @return <code>true</code> if enabled
     */
    private boolean isVisible(Rule rule) {
        // increase instructions counter
        mCountRulesAll++;

        // skip invalid options
        if (this.mCategories == null || rule.category == null) {
            return true;
        }

        // a rule is visible if categories contain this rule's category
        return this.mCategories.contains(rule.category);
    }
}
