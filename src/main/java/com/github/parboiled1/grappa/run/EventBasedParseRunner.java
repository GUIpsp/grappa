/*
 * Copyright (C) 2015 Francis Galiegue <fgaliegue@gmail.com>
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

package com.github.parboiled1.grappa.run;

import com.github.parboiled1.grappa.buffers.InputBuffer;
import com.github.parboiled1.grappa.matchers.base.Matcher;
import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import org.parboiled.MatchHandler;
import org.parboiled.MatcherContext;
import org.parboiled.Rule;
import org.parboiled.parserunners.AbstractParseRunner;
import org.parboiled.parserunners.ParseRunner;
import org.parboiled.support.ParsingResult;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * The most basic of all {@link ParseRunner} implementations. It runs a rule
 * against a given input text and builds a corresponding {@link ParsingResult}
 * instance. However, it does not report any parse errors nor recover from them.
 * Instead it simply marks the ParsingResult as "unmatched" if the input is not
 * valid with regard to the rule grammar.It never causes the parser to perform
 * more than one parsing run and is the fastest way to determine whether a given
 * input conforms to the rule grammar.
 */
@ParametersAreNonnullByDefault
public final class EventBasedParseRunner<V>
    extends AbstractParseRunner<V>
    implements MatchHandler
{
    private final EventBus bus = new EventBus();
    /**
     * Creates a new BasicParseRunner instance for the given rule.
     *
     * @param rule the parser rule
     */
    public EventBasedParseRunner(final Rule rule)
    {
        super(rule);
    }

    public void registerListener(final Object listener)
    {
        bus.register(listener);
    }

    @Override
    public ParsingResult<V> run(final InputBuffer inputBuffer)
    {
        Preconditions.checkNotNull(inputBuffer, "inputBuffer");
        resetValueStack();

        final MatcherContext<V> rootContext
            = createRootContext(inputBuffer, this);
        final boolean matched = rootContext.runMatcher();
        return createParsingResult(matched, rootContext);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public <T> boolean match(final MatcherContext<T> context)
    {
        final Matcher matcher = context.getMatcher();

        final PreMatchEvent<T> preMatchEvent
            = new PreMatchEvent<>(context);
        bus.post(preMatchEvent);

        final boolean match = matcher.match(context);

        final MatchEvent<T> postMatchEvent = match
            ? new MatchSuccessEvent<>(context)
            : new MatchFailureEvent<>(context);
        bus.post(postMatchEvent);

        return match;
    }
}