/*
 * Copyright (C) 2009-2011 Mathias Doenitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.parboiled.transform;

import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.Cached;
import org.parboiled.annotations.DontLabel;
import org.parboiled.annotations.ExplicitActionsOnly;
import org.parboiled.annotations.Label;
import org.parboiled.annotations.SuppressNode;
import org.parboiled.support.Var;

import static java.lang.Integer.parseInt;

@SuppressWarnings("UnusedDeclaration")
@BuildParseTree
public class TestParser extends BaseParser<Integer> {

    protected int integer;
    private int privateInt;

    public Rule RuleWithoutAction() {
        return sequence('a', 'b');
    }

    @Label("harry")
    public Rule RuleWithNamedLabel() {
        return sequence('a', 'b');
    }

    @SuppressNode
    public Rule RuleWithLeaf() {
        return sequence('a', 'b');
    }

    public Rule RuleWithDirectImplicitAction() {
        return sequence('a', integer == 0, 'b', 'c');
    }

    public Rule RuleWithIndirectImplicitAction() {
        return sequence('a', 'b', action() || integer == 5);
    }

    public Rule RuleWithDirectExplicitAction() {
        return sequence('a', ACTION(action() && integer > 0), 'b');
    }

    public Rule RuleWithIndirectExplicitAction() {
        return sequence('a', 'b', ACTION(integer < 0 && action()));
    }

    public Rule RuleWithIndirectImplicitParamAction(final int param) {
        return sequence('a', 'b', integer == param);
    }

    public Rule RuleWithComplexActionSetup(final int param) {
        int i = 26, j = 18;
        final Var<String> string = new Var<String>("text");
        i += param;
        j -= i;
        return sequence('a' + i, i > param + j, string, ACTION(integer + param < string.get().length() - i - j));
    }

    public Rule BugIn0990() {
        final Var<Integer> var = new Var<Integer>();
        return firstOf("10", "2");
    }

    @DontLabel
    public Rule RuleWith2Returns(final int param) {
        if (param == integer) {
            return sequence('a', ACTION(action()));
        } else {
            return EOI;
        }
    }

    @DontLabel
    public Rule RuleWithSwitchAndAction(final int param) {
        switch (param) {
            case 0: return sequence(EMPTY, push(1));
        }
        return null;
    }

    @ExplicitActionsOnly
    public Rule RuleWithExplicitActionsOnly(final int param) {
        final Boolean b = integer == param;
        return sequence('a', 'b', b);
    }

    @Cached
    public Rule RuleWithCachedAnd2Params(final String string, final long aLong) {
        return sequence(string, aLong == integer);
    }

    public Rule RuleWithFakeImplicitAction(final int param) {
        final Boolean b = integer == param;
        return sequence('a', 'b', b);
    }

    public Rule NumberRule() {
        return sequence(
                oneOrMore(charRange('0', '9')).suppressNode(),
                push(parseInt(match()))
        );
    }

    // actions

    public boolean action() {
        return true;
    }

}
