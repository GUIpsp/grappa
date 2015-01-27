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

package com.github.fge.grappa.matchers;

import com.github.fge.grappa.matchers.base.AbstractMatcher;
import com.github.fge.grappa.matchers.base.Matcher;
import com.github.fge.grappa.rules.Action;
import com.github.fge.grappa.rules.Rule;
import com.github.fge.grappa.rules.SkippableAction;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.parboiled.ContextAware;
import org.parboiled.MatcherContext;
import org.parboiled.errors.ActionError;
import org.parboiled.errors.ActionException;

import java.lang.reflect.Field;
import java.util.List;

/**
 * A {@link Matcher} that not actually matches input but runs a given parser {@link Action}.
 */
public final class ActionMatcher
    extends AbstractMatcher
{
    private final Action<?> action;
    private final List<ContextAware<?>> contextAwares = Lists.newArrayList();
    private final boolean skipInPredicates;

    public ActionMatcher(final Action<?> action)
    {
        super(Preconditions.checkNotNull(action).toString());
        this.action = action;

        skipInPredicates = action instanceof SkippableAction
            && ((SkippableAction<?>) action).skipInPredicates();

        // check whether the action is a synthetic class generated by
        // parboiled transformation if so it will take care of context
        // management itself and we can return immediately
        /*
         * NOTE: was Class<? extends Action> before, but see constructor
         */
        final Class<?> actionClass = action.getClass();

        if (actionClass.isSynthetic())
            return;

        if (action instanceof ContextAware)
            contextAwares.add((ContextAware<?>) action);

        // in order to make anonymous inner classes and other member classes
        // work seamlessly we collect the synthetic references to the outer
        // parent classes and inform them of the current parsing context if they
        // implement ContextAware
        for (final Field field: actionClass.getDeclaredFields()) {
            if (!field.isSynthetic())
                continue;
            if (!ContextAware.class.isAssignableFrom(field.getType()))
                continue;

            field.setAccessible(true);
            try {
                final ContextAware<?> contextAware
                    = (ContextAware<?>) field.get(action);
                if (contextAware != null)
                    contextAwares.add(contextAware);
            } catch (IllegalAccessException ignored) {
                // ignore
            } finally {
                field.setAccessible(false);
            }
        }
    }

    @Override
    public MatcherType getType()
    {
        return MatcherType.ACTION;
    }

    @Override
    public <V> MatcherContext<V> getSubContext(final MatcherContext<V> context)
    {
        final MatcherContext<V> subContext = context.getBasicSubContext();
        subContext.setMatcher(this);
        // if we have already matched something we must be in a sequence at the
        // second or later position the subcontext contains match data that the
        // action might want to access, so we use the existing subcontext
        // without reinitializing
        return context.getCurrentIndex() > context.getStartIndex()
            ? subContext
            : context.getSubContext(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> boolean match(final MatcherContext<V> context)
    {
        if (skipInPredicates && context.inPredicate())
            return true;

        // actions need to run in the parent context
        final MatcherContext<V> parentContext = context.getParent();
        for (final ContextAware<?> contextAware: contextAwares)
            ((ContextAware<V>) contextAware).setContext(parentContext);


        try {
            final Object valueStackSnapshot
                = context.getValueStack().takeSnapshot();
            if (!((Action<V>) action).run(parentContext)) {
                // failing actions are not allowed to change the ValueStack
                context.getValueStack().restoreSnapshot(valueStackSnapshot);
                return false;
            }

            // since we initialize the actions own context only partially in
            // getSubContext(MatcherContext) (in order to be able to still
            // access the previous subcontexts fields in action expressions) we
            // need to make sure to not accidentally advance the current index
            // of our parent with some old index from a previous subcontext, so
            // we explicitly set the marker here
            context.setCurrentIndex(parentContext.getCurrentIndex());
            return true;
        } catch (ActionException e) {
            final ActionError error = new ActionError(context.getInputBuffer(),
                context.getCurrentIndex(), e.getMessage(), context.getPath(),
                e);
            context.getParseErrors().add(error);
            return false;
        }
    }

    @Override
    public Rule suppressNode()
    {
        return this; // actions are already "suppressNode"
    }

    @Override
    public boolean canMatchEmpty()
    {
        // By definition; an action has read only access to the parse context
        return true;
    }
}
