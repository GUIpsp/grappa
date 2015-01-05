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

package org.parboiled.trees;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * A simple, immutable {@link GraphNode} implementation.
 *
 * @param <T> the actual implementation type of this ImmutableGraphNode
 */
// TODO: rename; this class IS NOT immutable.
public class ImmutableGraphNode<T extends GraphNode<T>>
    implements GraphNode<T>
{
    /*
     * TODO: cleanup that royal mess
     *
     * ImmutableLinkedList seems only to be used in a few special cases; this
     * class unfortunately allows for both the aforementioned class (which is,
     * frankly, also a mess) and regular lists.
     *
     * Find a way to separate.
     */
    private final List<T> children;

    public ImmutableGraphNode()
    {
        this(ImmutableList.<T>of());
    }

    // TODO! Null! Again! I need a gun!
    public ImmutableGraphNode(@Nonnull final List<T> children)
    {
        Preconditions.checkNotNull(children);
        /*
         * ImmutableLinkedList has no such thing as a "safe copy constructor";
         * ImmutableList (Guava's, that is) does; what is more, if the argument
         * to .copyOf() is _also_ a (Guava...) ImmutableList, it won't even make
         * a copy.
         */
        this.children = ImmutableList.copyOf(children);
    }

    @Override
    public final List<T> getChildren()
    {
        return children;
    }
}
