/*
 * Copyright (C) 2014 Francis Galiegue <fgaliegue@gmail.com>
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

package com.github.fge.grappa.buffers;

import com.google.common.collect.Range;
import org.parboiled.support.Position;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public final class LineCounterTest
{
    @Test
    public void emptyInputIsCorrectlyHandled()
    {
        final LineCounter lineCounter = new LineCounter("");
        assertThat(lineCounter.getLineRange(1)).as("range is correct")
            .isEqualTo(Range.closedOpen(0, 0));
    }


    @Test
    public void singleLineInputIsCorrectlyHandled()
    {
        final LineCounter lineCounter = new LineCounter("hello");
        assertThat(lineCounter.getLineRange(1)).as("range is correct")
            .isEqualTo(Range.closedOpen(0, 5));
        assertThat(lineCounter.toPosition(3)).as("position is correct")
            .isEqualTo(new Position(1, 4));
    }

    @Test
    public void emptyLinesAreCorrectlyDetected()
    {
        final LineCounter lineCounter = new LineCounter("hello\r\n\n");
        assertThat(lineCounter.getLineRange(2)).as("range is correct")
            .isEqualTo(Range.closedOpen(7, 8));
        assertThat(lineCounter.getLineRange(3)).as("range is correct")
            .isEqualTo(Range.closedOpen(8, 8));
    }

    @Test
    public void nonNewlineLastLineIsCorrectlyDetected()
    {
        final LineCounter lineCounter = new LineCounter("hello\nworld");
        assertThat(lineCounter.getLineRange(2)).as("range is correct")
            .isEqualTo(Range.closedOpen(6, 11));
    }
}
