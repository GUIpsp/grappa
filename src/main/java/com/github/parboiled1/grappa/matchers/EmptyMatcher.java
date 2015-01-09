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
import org.parboiled.MatcherContext;

/**
 * A {@link Matcher} that always successfully matches nothing.
 */
public final class EmptyMatcher
    extends AbstractMatcher
{
    public EmptyMatcher()
    {
        super("EMPTY");
    }

    @Override
    public <V> boolean match(final MatcherContext<V> context)
    {
        context.createNode();
        return true;
    }
}