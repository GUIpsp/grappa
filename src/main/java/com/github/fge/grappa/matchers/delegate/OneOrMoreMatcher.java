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

package com.github.fge.grappa.matchers.delegate;

import com.github.fge.grappa.matchers.MatcherType;
import com.github.fge.grappa.matchers.base.CustomDefaultLabelMatcher;
import com.github.fge.grappa.matchers.base.Matcher;
import com.google.common.base.Preconditions;
import org.parboiled.MatcherContext;
import org.parboiled.Rule;

/**
 * A {@link Matcher} that repeatedly tries its submatcher against the input.
 * Succeeds if its submatcher succeeds at least once.
 */
public final class OneOrMoreMatcher
    extends CustomDefaultLabelMatcher<OneOrMoreMatcher>
{
    private final Matcher subMatcher;

    public OneOrMoreMatcher(final Rule subRule)
    {
        super(Preconditions.checkNotNull(subRule, "subRule"), "oneOrMore");
        subMatcher = getChildren().get(0);
    }

    @Override
    public MatcherType getType()
    {
        return MatcherType.COMPOSITE;
    }

    @Override
    public <V> boolean match(final MatcherContext<V> context)
    {
        if (!subMatcher.getSubContext(context).runMatcher())
            return false;

        while (subMatcher.getSubContext(context).runMatcher())
            ; // Nothing

        context.createNode();
        return true;
    }

    @Override
    public boolean canMatchEmpty()
    {
        // Will have been checked at build time
        return false;
    }
}
