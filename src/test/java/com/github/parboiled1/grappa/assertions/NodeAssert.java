package com.github.parboiled1.grappa.assertions;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.assertj.core.api.AbstractAssert;
import org.parboiled.Node;
import org.parboiled.buffers.InputBuffer;

import javax.annotation.Nonnull;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A parse tree dump has such a node at its root
 *
 * <p>TODO: not sure about the not null assertion here</p>
 *
 * @param <V> values produced by this node tree
 */
public final class NodeAssert<V>
    extends AbstractAssert<NodeAssert<V>, Node<V>>
{
    private final InputBuffer buffer;

    NodeAssert(final Node<V> actual, final InputBuffer buffer)
    {
        super(Preconditions.checkNotNull(actual), NodeAssert.class);
        this.buffer = buffer;
    }

    private NodeAssert<V> doHasLabel(@Nonnull final String expectedLabel)
    {
        final String actualLabel = actual.getLabel();
        assertThat(actualLabel).overridingErrorMessage(
            "node's label is null! I didn't expect it to be"
        ).isNotNull();
        assertThat(actualLabel).overridingErrorMessage(
            "node's label is not what was expected!\n"
            + "Expected: '%s'\nActual  : '%s'\n", expectedLabel, actualLabel
        ).isEqualTo(expectedLabel);
        return this;
    }

    NodeAssert<V> hasLabel(@Nonnull final Optional<String> label)
    {
        return label.isPresent() ? doHasLabel(label.get()) : this;
    }

    private NodeAssert<V> doHasMatch(@Nonnull final String expectedMatch)
    {
        final String actualMatch
            = buffer.extract(actual.getStartIndex(), actual.getEndIndex());
        assertThat(actualMatch).overridingErrorMessage(
            "rule did not match what was expected!\n"
            + "Expected: -->%s<--\nActual  : -->%s<--\n",
            expectedMatch, actualMatch
        ).isEqualTo(expectedMatch);
        return this;
    }

    NodeAssert<V> hasMatch(@Nonnull final Optional<String> match)
    {
        return match.isPresent() ? doHasMatch(match.get()) : this;
    }
}