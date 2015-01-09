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

package com.github.parboiled1.grappa.matchers;

import com.github.parboiled1.grappa.matchers.base.AbstractMatcher;
import com.github.parboiled1.grappa.matchers.base.Matcher;
import com.google.common.base.Preconditions;
import org.parboiled.MatcherContext;

import static org.parboiled.support.Chars.escape;

/**
 * A {@link Matcher} matching a single character out of a given range of characters.
 */
@SuppressWarnings("ImplicitNumericConversion")
public final class CharRangeMatcher
    extends AbstractMatcher
{
    private final char lowerBound;
    private final char upperBound;

    public CharRangeMatcher(final char lowerBound, final char upperBound)
    {
        super(escape(lowerBound) + ".." + escape(upperBound));
        Preconditions.checkArgument(lowerBound < upperBound);
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public char getLowerBound()
    {
        return lowerBound;
    }

    public char getUpperBound()
    {
        return upperBound;
    }

    @Override
    public <V> boolean match(final MatcherContext<V> context)
    {
        final char c = context.getCurrentChar();
        if (c < lowerBound || c > upperBound)
            return false;

        context.advanceIndex(1);
        context.createNode();
        return true;
    }
}