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
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.parboiled.MatcherContext;
import org.parboiled.support.ParsingResult;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public final class EventBasedParseRunnerTest
{
    private MatcherContext<Object> context;
    private Matcher matcher;
    private EventBasedParseRunner<Object> parseRunner;
    private ParseRunnerListener<Object> listener;

    @BeforeMethod
    public void init()
    {
        //noinspection unchecked
        context = mock(MatcherContext.class);
        matcher = mock(Matcher.class);
        parseRunner = spy(new EventBasedParseRunner<>(matcher));
        listener = spy(new ParseRunnerListener<>());
        parseRunner.registerListener(listener);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void parsingRunTriggersPreAndPostParse()
    {
        final InputBuffer buffer = mock(InputBuffer.class);
        final ParsingResult<Object> result = mock(ParsingResult.class);

        doReturn(context)
            .when(parseRunner).createRootContext(buffer, parseRunner);
        doReturn(result)
            .when(parseRunner).createParsingResult(anyBoolean(), same(context));

        final InOrder inOrder = inOrder(listener);
        
        final ArgumentCaptor<PreParseEvent> preParse
            = ArgumentCaptor.forClass(PreParseEvent.class);
        final ArgumentCaptor<PostParseEvent> postParse
            = ArgumentCaptor.forClass(PostParseEvent.class);

        parseRunner.run(buffer);

        inOrder.verify(listener).beforeParse(preParse.capture());
        inOrder.verify(listener).afterParse(postParse.capture());
        inOrder.verifyNoMoreInteractions();

        assertThat(preParse.getValue().getContext()).isSameAs(context);
        assertThat(postParse.getValue().getResult()).isSameAs(result);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void failingMatchRunTriggersPreAndFailedMatchEvents()
    {
        context = mock(MatcherContext.class);
        when(context.getMatcher()).thenReturn(matcher);
        // This is the default, but let's make it explicit
        when(matcher.match(context)).thenReturn(false);

        assertThat(parseRunner.match(context)).isFalse();

        final InOrder inOrder = inOrder(listener);

        final ArgumentCaptor<PreMatchEvent> preMatch
            = ArgumentCaptor.forClass(PreMatchEvent.class);
        final ArgumentCaptor<MatchFailureEvent> postMatch
            = ArgumentCaptor.forClass(MatchFailureEvent.class);


        inOrder.verify(listener).beforeMatch(preMatch.capture());
        inOrder.verify(listener).matchFailure(postMatch.capture());

        assertThat(preMatch.getValue().getContext()).isSameAs(context);
        assertThat(postMatch.getValue().getContext()).isSameAs(context);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void successfulMatchRunTriggersPreAndFailedMatchEvents()
    {
        final MatcherContext<Object> context = mock(MatcherContext.class);
        when(context.getMatcher()).thenReturn(matcher);
        // This is the default, but let's make it explicit
        when(matcher.match(context)).thenReturn(true);

        assertThat(parseRunner.match(context)).isTrue();

        final InOrder inOrder = inOrder(listener);

        final ArgumentCaptor<PreMatchEvent> preMatch
            = ArgumentCaptor.forClass(PreMatchEvent.class);
        final ArgumentCaptor<MatchSuccessEvent> postMatch
            = ArgumentCaptor.forClass(MatchSuccessEvent.class);


        inOrder.verify(listener).beforeMatch(preMatch.capture());
        inOrder.verify(listener).matchSuccess(postMatch.capture());
        inOrder.verifyNoMoreInteractions();

        assertThat(preMatch.getValue().getContext()).isSameAs(context);
        assertThat(postMatch.getValue().getContext()).isSameAs(context);
    }
}