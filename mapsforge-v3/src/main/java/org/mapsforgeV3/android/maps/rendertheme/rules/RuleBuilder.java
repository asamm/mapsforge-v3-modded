package org.mapsforgeV3.android.maps.rendertheme.rules;

import org.mapsforgeV3.android.maps.rendertheme.RenderThemeHandler;
import org.xml.sax.Attributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.regex.Pattern;

/**
 * Created by menion on 28/05/15.
 * Asamm Software, s. r. o.
 */
public class RuleBuilder {

    private static final Pattern SPLIT_PATTERN = Pattern.compile("\\|");
    private static final String STRING_NEGATION = "~";
    private static final String STRING_WILDCARD = "*";

    private static final String CAT = "cat";
    private static final String CLOSED = "closed";
	private static final String COUNTRY = "country";
    private static final String E = "e";
    private static final String K = "k";
    private static final String STYLE = "style";
    private static final String V = "v";
    private static final String ZOOM_MAX = "zoom-max";
    private static final String ZOOM_MIN = "zoom-min";

    private static ClosedMatcher getClosedMatcher(Closed closed) {
        switch (closed) {
            case YES:
                return ClosedWayMatcher.getInstance();
            case NO:
                return LinearWayMatcher.getInstance();
            case ANY:
                return AnyMatcher.getInstance();
        }

        throw new IllegalArgumentException("unknown enum value: " + closed);
    }

    private static ElementMatcher getElementMatcher(Element element) {
        switch (element) {
            case NODE:
                return ElementNodeMatcher.getInstance();
            case WAY:
                return ElementWayMatcher.getInstance();
            case ANY:
                return AnyMatcher.getInstance();
        }

        throw new IllegalArgumentException("unknown enum value: " + element);
    }

    private static AttributeMatcher getKeyMatcher(List<String> keyList) {
        if (STRING_WILDCARD.equals(keyList.get(0))) {
            return AnyMatcher.getInstance();
        }

        AttributeMatcher attributeMatcher = Rule.MATCHERS_CACHE_KEY.get(keyList);
        if (attributeMatcher == null) {
            if (keyList.size() == 1) {
                attributeMatcher = new SingleKeyMatcher(keyList.get(0));
            } else {
                attributeMatcher = new MultiKeyMatcher(keyList);
            }
            Rule.MATCHERS_CACHE_KEY.put(keyList, attributeMatcher);
        }
        return attributeMatcher;
    }

    private static AttributeMatcher getValueMatcher(List<String> valueList) {
//Logger.d(TAG, "getValueMatcher(" + valueList + ")");
        if (STRING_WILDCARD.equals(valueList.get(0))) {
            return AnyMatcher.getInstance();
        }

        AttributeMatcher attributeMatcher = Rule.MATCHERS_CACHE_VALUE.get(valueList);
        if (attributeMatcher == null) {
            if (valueList.size() == 1) {
                attributeMatcher = new SingleValueMatcher(valueList.get(0));
            } else {
                attributeMatcher = new MultiValueMatcher(valueList);
            }
            Rule.MATCHERS_CACHE_VALUE.put(valueList, attributeMatcher);
        }
        return attributeMatcher;
    }

    // CREATOR ITSELF

    private String elementName = null;
    private Stack<Rule> ruleStack = null;

    private Element element = null;
    private String keys = null;
    private String values = null;
    private String mCategory = null;
	private String mCountryCodes = null;
    private Closed closed = Closed.ANY;
    private byte zoomMin = 0;
    private byte zoomMax = Byte.MAX_VALUE;
    private String style = null;

    public RuleBuilder(String elementName, Attributes attributes, Stack<Rule> ruleStack) {
        this.elementName = elementName;
        this.ruleStack = ruleStack;

        // parse values
        for (int i = 0; i < attributes.getLength(); ++i) {
            String name = attributes.getLocalName(i);
            String value = attributes.getValue(i);

            switch (name) {
                case E:
                    this.element = Element.valueOf(value.toUpperCase(Locale.ENGLISH));
                    break;
                case K:
                    this.keys = value;
                    break;
                case V:
                    this.values = value;
                    break;
                case CAT:
                    this.mCategory = value;
                    break;
				case COUNTRY:
					this.mCountryCodes = value;
					break;
                case CLOSED:
                    this.closed = Closed.valueOf(value.toUpperCase(Locale.ENGLISH));
                    break;
                case STYLE:
                    this.style = value;
                    break;
                case ZOOM_MIN:
                    this.zoomMin = Byte.parseByte(value);
                    break;
                case ZOOM_MAX:
                    this.zoomMax = Byte.parseByte(value);
                    break;
                default:
                    RenderThemeHandler.logUnknownAttribute(elementName, name, value, i);
                    break;
            }
        }
    }

    /**
     * Construct rule based on loaded parameters.
     * @return created rule
     */
    public Rule build() {
        // validate data and create a rule
        validate();
        return createRule();
    }

    private void validate() {
        if (element == null) {
            throw new IllegalArgumentException(
                    "missing attribute e for element: " + elementName);
        } else if (keys == null) {
            throw new IllegalArgumentException(
                    "missing attribute k for element: " + elementName);
        } else if (values == null) {
            throw new IllegalArgumentException(
                    "missing attribute v for element: " + elementName);
        } else if (zoomMin < 0) {
            throw new IllegalArgumentException(
                    "zoom-min must not be negative: " + zoomMin);
        } else if (zoomMax < 0) {
            throw new IllegalArgumentException(
                    "zoom-max must not be negative: " + zoomMax);
        } else if (zoomMin > zoomMax) {
            throw new IllegalArgumentException(
                    "zoom-min must be less or equal zoom-max: " + zoomMin);
        }
    }

    private Rule createRule() {
        ElementMatcher elementMatcher = getElementMatcher(element);
        ClosedMatcher closedMatcher = getClosedMatcher(closed);
        List<String> keyList = new ArrayList<>(
                Arrays.asList(SPLIT_PATTERN.split(keys)));
        List<String> valueList = new ArrayList<>(
                Arrays.asList(SPLIT_PATTERN.split(values)));
		String[] countryCodes = new String[0];
		if (mCountryCodes != null && mCountryCodes.length() > 0) {
			countryCodes = SPLIT_PATTERN.split(mCountryCodes.trim().toLowerCase());
		}

        // generate rule based on loaded parameters
        Rule rule;
        if (valueList.remove(STRING_NEGATION)) {
            NegativeMatcher attributeMatcher = new NegativeMatcher(keyList, valueList);
            rule = new NegativeRule(elementMatcher, closedMatcher, zoomMin,
                    zoomMax, attributeMatcher, mCategory, countryCodes);
        } else {
            AttributeMatcher keyMatcher = getKeyMatcher(keyList);
            AttributeMatcher valueMatcher = getValueMatcher(valueList);
            rule = new PositiveRule(elementMatcher, closedMatcher, zoomMin,
                    zoomMax, keyMatcher, valueMatcher, mCategory, countryCodes);
        }

        // set style
        if (style != null && style.length() > 0) {
            rule.setStyles(SPLIT_PATTERN.split(style));
        }

        // return rule
        return rule;
    }
}
