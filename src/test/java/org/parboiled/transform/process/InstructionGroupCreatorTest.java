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

package org.parboiled.transform.process;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.parboiled.transform.InstructionGraphNode;
import org.parboiled.transform.RuleMethod;
import org.parboiled.transform.TestParser;
import org.testng.annotations.Test;

import java.util.List;
import java.util.zip.CRC32;

import static org.parboiled.transform.AsmTestUtils.getMethodInstructionList;
import static org.testng.Assert.assertEquals;

public class InstructionGroupCreatorTest extends TransformationTest {

    private final List<RuleMethodProcessor> processors = ImmutableList.of(
            new UnusedLabelsRemover(),
            new ReturnInstructionUnifier(),
            new InstructionGraphCreator(),
            new ImplicitActionsConverter(),
            new InstructionGroupCreator()
    );

    @SuppressWarnings("FieldCanBeLocal")
    private String dotSource;

    @Test(enabled = false)
    public void testInstructionGraphing() throws Exception {
        setup(TestParser.class);

        testMethodAnalysis("RuleWithComplexActionSetup", 724347041L);
        //renderToGraphViz(dotSource);
    }

    private void testMethodAnalysis(final String methodName, final long dotSourceCRC) throws Exception {
        final RuleMethod method = processMethod(methodName, processors);

        dotSource = generateDotSource(method);
        final long crc = computeCRC(dotSource);
        if (crc != dotSourceCRC) {
            System.err.println("Invalid dotSource CRC for method '" + methodName + "': " + crc + 'L');
            assertEquals(dotSource, "");
        }
    }

    private String generateDotSource(final RuleMethod method) {
        Preconditions.checkNotNull(method, "method");

        // generate graph attributes
        final StringBuilder sb = new StringBuilder()
                .append("digraph G {\n")
                .append("fontsize=10;\n")
                .append("label=\"")
                .append(getMethodInstructionList(method).replace("\n", "\\l").replace("\"", "\'"))
                .append("\";\n");

        // legend
        sb.append(" Action [penwidth=2.0,style=filled,fillcolor=skyblue];\n");
        sb.append(" VarInit [penwidth=2.0,style=filled,fillcolor=grey];\n");
        sb.append(" XLoad [penwidth=2.0,color=orange];\n");
        sb.append(" XStore [penwidth=2.0,color=red];\n");
        sb.append(" CallOnContextAware [penwidth=2.0];\n");
        sb.append(" Action -> Capture -> VarInit -> ContextSwitch -> XLoad -> XStore -> CallOnContextAware;\n");

        for (int i = 0; i < method.getGraphNodes().size(); i++) {
            final InstructionGraphNode node = method.getGraphNodes().get(i);
            // generate node
            final boolean isSpecial = node.isActionRoot() || node.isVarInitRoot() ||
                    node.isXLoad() || node.isXStore() || node.isCallOnContextAware();
            sb.append(" ").append(i)
                    .append(" [")
                    .append(isSpecial ? "penwidth=2.0," : "penwidth=1.0,")
                    .append(node.isActionRoot() ? "color=skyblue," : "")
                    .append(node.isVarInitRoot() ? "color=grey," : "")
                    .append(node.isXLoad() ? "color=orange," : "")
                    .append(node.isXStore() ? "color=red," : "")
                    .append(node.getGroup() != null && node.getGroup().getRoot().isActionRoot() ?
                            "style=filled,fillcolor=\"/pastel15/" + (method.getGroups()
                                    .indexOf(node.getGroup()) + 1) + "\"," : "")
                    .append(node.getGroup() != null && node.getGroup().getRoot().isVarInitRoot() ?
                            "style=filled,fillcolor=grey," : "")
                    .append("fontcolor=black];\n");

            // generate edges
            for (final InstructionGraphNode pred : node.getPredecessors()) {
                sb.append(" ").append(method.getGraphNodes().indexOf(pred)).append(" -> ").append(i)
                        .append(";\n");
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    private static long computeCRC(final String text) throws Exception {
        final CRC32 crc32 = new CRC32();
        final byte[] buf = text.getBytes("UTF8");
        crc32.update(buf);
        return crc32.getValue();
    }

}