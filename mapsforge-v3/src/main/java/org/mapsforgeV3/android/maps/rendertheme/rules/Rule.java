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
package org.mapsforgeV3.android.maps.rendertheme.rules;

import org.mapsforgeV3.android.maps.rendertheme.RenderCallback;
import org.mapsforgeV3.android.maps.rendertheme.RenderTheme;
import org.mapsforgeV3.android.maps.rendertheme.renderinstruction.RenderInstruction;
import org.mapsforgeV3.core.model.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Rule {

	// static containers for matchers
	static final Map<List<String>, AttributeMatcher> MATCHERS_CACHE_KEY =
            new HashMap<>();
    static final Map<List<String>, AttributeMatcher> MATCHERS_CACHE_VALUE =
            new HashMap<>();

	// reference to parent render theme
	private RenderTheme mTheme;
    // list of instructions
	private RenderInstruction[] mInstr;
	// temporary container of instructions during loading
	private final List<RenderInstruction> mInstrLoad;
    // list of attached rules
	private Rule[] mSubRules;
	// temporary container of sub-rules during loading
	private final List<Rule> mSubRulesLoad;

	final ClosedMatcher closedMatcher;
	final ElementMatcher elementMatcher;
	final byte zoomMax;
	final byte zoomMin;
    // category parameter from 0.5 version themes
    public String category;
	// country code definition
	final String[] countryCodes;

	// list of styles that matches current rule
	private String[] mStyle;

	Rule(ElementMatcher elementMatcher, ClosedMatcher closedMatcher,
			byte zoomMin, byte zoomMax, String category, String[] countryCodes) {
		this.elementMatcher = elementMatcher;
		this.closedMatcher = closedMatcher;
		this.zoomMin = zoomMin;
		this.zoomMax = zoomMax;
        this.category = category;
		this.countryCodes = countryCodes;

		this.mInstrLoad = new ArrayList<>(4);
		this.mSubRulesLoad = new ArrayList<>(4);
	}

	/**
	 * Get attached render theme. This theme is available only after rule is fully initialized!
	 * @return instance of theme
	 */
	public RenderTheme getTheme() {
		return mTheme;
	}

	/**
     * Set Locus internal styles.
     */
    protected void setStyles(String[] styles) {
        this.mStyle = styles;
    }

	/**
	 * Check if current rule match required style of theme,
	 * @param requiredStyle required style of theme
	 * @return <code>true</code> if rule is valid
	 */
    public Rule validateRule(String requiredStyle) {
		// if required style is not defined, we don't need to do any checks as all
		// rules are automatically valid
		if (requiredStyle == null || requiredStyle.length() == 0) {
			return this;
		}

		// check style of rule
		boolean styleValid = false;
		if (mStyle == null || mStyle.length == 0) {
			styleValid = true;
		} else {
			// iterate over styles
            for (String style : mStyle) {
                if (style.equals(requiredStyle)) {
                    styleValid = true;
                    break;
                }
            }
		}

		// handle validity
		if (!styleValid) {
			return null;
		} else {
			// validate all sub-rules
			List<Rule> subRulesCopy = new ArrayList<>(mSubRulesLoad);
			mSubRulesLoad.clear();
			for (int i = 0, m = subRulesCopy.size(); i < m; i++) {
				Rule subRule = subRulesCopy.get(i).validateRule(requiredStyle);
				if (subRule != null) {
					mSubRulesLoad.add(subRule);
				}
			}

			// return current optimized rule
			return this;
		}
	}

	/**
	 * Add rendering instruction to current rule.
	 * @param instruction instruction
	 */
	public void addRenderingInstruction(RenderInstruction instruction) {
		this.mInstrLoad.add(instruction);
	}

	/**
	 * Add sub-rule to current rule.
	 * @param rule new subRule
	 */
	public void addSubRule(Rule rule) {
		this.mSubRulesLoad.add(rule);
	}

	abstract boolean matchesNode(List<Tag> tags, byte zoomLevel);

	abstract boolean matchesWay(List<Tag> tags, byte zoomLevel, Closed closed);

	public void matchNode(RenderCallback renderCallback, List<Tag> tags, byte zoomLevel) {
		RenderTheme.countTestNodes++;
		if (matchesNode(tags, zoomLevel)) {
			for (RenderInstruction instr : mInstr) {
				instr.renderNode(renderCallback, tags);
			}
			for (Rule rule : mSubRules) {
				rule.matchNode(renderCallback, tags, zoomLevel);
			}
		}
	}

	public void matchWay(RenderCallback renderCallback, List<Tag> tags,
                         byte zoomLevel, Closed closed, List<RenderInstruction> matchingList) {
		RenderTheme.countTestWays++;
		if (matchesWay(tags, zoomLevel, closed)) {
			for (RenderInstruction instr : mInstr) {
				instr.renderWay(renderCallback, tags);
				matchingList.add(instr);
			}
			for (Rule rule : mSubRules) {
				rule.matchWay(renderCallback, tags, zoomLevel, closed, matchingList);
			}
		}
	}

	/**************************************************/
	// RULE LIFE-CYCLE
	/**************************************************/

	/**
	 * Finish initialization of rule.
	 * @param theme current theme
	 */
	public void onComplete(RenderTheme theme) {
		// store theme reference
		this.mTheme = theme;

		// clear matches
		MATCHERS_CACHE_KEY.clear();
		MATCHERS_CACHE_VALUE.clear();

		// finalize containers for instructions
		mInstr = new RenderInstruction[mInstrLoad.size()];
		mInstrLoad.toArray(mInstr);
		mInstrLoad.clear();

		// finalize containers for sub-rules
		mSubRules = new Rule[mSubRulesLoad.size()];
		mSubRulesLoad.toArray(mSubRules);
		for (Rule subRule : mSubRules) {
			subRule.onComplete(theme);
		}
		mSubRulesLoad.clear();
	}

	/**
	 * Destroy instance of render theme.
	 */
	public void onDestroy() {
		for (RenderInstruction instr : mInstr) {
			instr.destroy();
		}
		for (Rule subRule : mSubRules) {
			subRule.onDestroy();
		}
	}

	public void prepareRule(float scaleStroke, float scaleText, byte zoomLevel) {
		for (RenderInstruction instr : mInstr) {
			instr.prepare(getTheme(), scaleStroke, scaleText, zoomLevel);
		}
		for (Rule subRule : mSubRules) {
			subRule.prepareRule(scaleStroke, scaleText, zoomLevel);
		}
	}

	/**
	 * Create optimized version of rule for faster usage.
	 * @param zoomLevel current visible zoom level
	 * @param mapCountryCode current map country code
	 * @return optimized rule or 'null' if rule do not match
	 */
	public Rule createOptimized(byte zoomLevel, String mapCountryCode) {
		// check zoom level
		if (zoomMin > zoomLevel || zoomMax < zoomLevel) {
			return null;
		}

		// check "country code"
		boolean valid = true;
		if (countryCodes.length > 0 && mapCountryCode.length() > 0) {
			valid = false;
			for (String cc : countryCodes) {
				if (cc.equals("*")) {
					valid = true;
				} else if (cc.equals(mapCountryCode)) {
					valid = true;
					break;
				} else if (cc.startsWith("!") && cc.substring(1).equals(mapCountryCode)) {
					valid = false;
					break;
				}
			}
		}
		if (!valid) {
			return null;
		}
		
		// generate optimized
		Rule ruleOptimized = null;
		if (this instanceof PositiveRule) {
			PositiveRule pr = (PositiveRule) this;
			ruleOptimized = new PositiveRule(elementMatcher, closedMatcher,
					zoomMin, zoomMax, pr.keyMatcher, pr.valueMatcher, pr.category, pr.countryCodes);
		} else if (this instanceof NegativeRule) {
			NegativeRule nr = (NegativeRule) this;
			ruleOptimized = new NegativeRule(elementMatcher, closedMatcher,
					zoomMin, zoomMax, nr.negativeMatcher, nr.category, nr.countryCodes);
		}
		
		// add all instructions and sub rules
		if (ruleOptimized != null) {
			for (RenderInstruction instr : mInstr) {
				ruleOptimized.addRenderingInstruction(instr);
			}

			for (Rule subRule : mSubRules) {
				subRule = subRule.createOptimized(zoomLevel, mapCountryCode);
				if (subRule != null) {
					ruleOptimized.addSubRule(subRule);
				}
			}
		}
		return ruleOptimized;
	}

	/**
	 * Get number of all sub-rules + this rule. Values is always at least 1, for current rule.
	 * @return number of rule + sub-rules.
	 */
	public int getNumOfRules() {
		int count = 1;
		for (Rule subRule : mSubRules) {
			count += subRule.getNumOfRules();
		}
		return count;
	}

	public Rule[] getSubRules() {
		return mSubRules;
	}

	public RenderInstruction[] getInstructions() {
		return mInstr;
	}
}
