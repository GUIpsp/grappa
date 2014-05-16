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

package org.parboiled.parserunners;

import com.github.parboiled1.grappa.annotations.WillBeFinal;
import com.github.parboiled1.grappa.annotations.WillBeRemoved;
import com.google.common.base.Preconditions;
import org.parboiled.MatchHandler;
import org.parboiled.MatcherContext;
import org.parboiled.Rule;
import org.parboiled.buffers.InputBuffer;
import org.parboiled.support.ParsingResult;

/**
 * The most basic of all {@link ParseRunner} implementations. It runs a rule
 * against a given input text and builds a corresponding {@link ParsingResult}
 * instance. However, it does not report any parse errors nor recover from them.
 * Instead it simply marks the ParsingResult as "unmatched" if the input is not
 * valid with regard to the rule grammar.It never causes the parser to perform
 * more than one parsing run and is the fastest way to determine whether a given
 * input conforms to the rule grammar.
 */
@WillBeFinal(version = "1.1")
public class BasicParseRunner<V>
    extends AbstractParseRunner<V>
    implements MatchHandler
{

    /**
     * Create a new BasicParseRunner instance with the given rule and input text
     * and returns the result of its {@link #run(String)} method invocation.
     *
     * @param rule the parser rule to run
     * @param input the input text to run on
     * @return the ParsingResult for the parsing run
     *
     * @deprecated As of 0.11.0 you should use the "regular" constructor and one
     * of the "run" methods rather than this static method.
     */
    @Deprecated
    @WillBeRemoved(version = "1.1")
    public static <V> ParsingResult<V> run(final Rule rule, final String input)
    {
        Preconditions.checkNotNull(rule, "rule");
        Preconditions.checkNotNull(input, "input");
        return new BasicParseRunner<V>(rule).run(input);
    }

    /**
     * Creates a new BasicParseRunner instance for the given rule.
     *
     * @param rule the parser rule
     */
    public BasicParseRunner(final Rule rule)
    {
        super(rule);
    }

    @Override
    public ParsingResult<V> run(final InputBuffer inputBuffer)
    {
        Preconditions.checkNotNull(inputBuffer, "inputBuffer");
        resetValueStack();

        final MatcherContext<V> rootContext
            = createRootContext(inputBuffer, this, true);
        final boolean matched = rootContext.runMatcher();
        return createParsingResult(matched, rootContext);
    }

    @Override
    public boolean match(final MatcherContext<?> context)
    {
        return context.getMatcher().match(context);
    }
}
