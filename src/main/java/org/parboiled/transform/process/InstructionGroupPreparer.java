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

import com.github.parboiled1.grappa.transform.InstructionGroupHasher;
import com.google.common.base.Preconditions;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.parboiled.transform.InstructionGraphNode;
import org.parboiled.transform.InstructionGroup;
import org.parboiled.transform.ParserClassNode;
import org.parboiled.transform.RuleMethod;

import javax.annotation.Nonnull;
import java.util.List;

public final class InstructionGroupPreparer
    implements RuleMethodProcessor
{
    private RuleMethod method;

    @Override
    public boolean appliesTo(@Nonnull final ParserClassNode classNode,
        @Nonnull final RuleMethod method)
    {
        Preconditions.checkNotNull(classNode, "classNode");
        Preconditions.checkNotNull(method, "method");
        return method.containsExplicitActions() || method.containsVars();
    }

    @Override
    public void process(@Nonnull final ParserClassNode classNode,
        @Nonnull final RuleMethod method)
    {
        this.method = Preconditions.checkNotNull(method, "method");

        // prepare groups for later stages
        for (final InstructionGroup group: method.getGroups()) {
            extractInstructions(group);
            extractFields(group);
            InstructionGroupHasher.hash(group, classNode.name);
        }
    }

    // move all group instructions except for the root from the underlying
    // method into the groups Insnlist
    private void extractInstructions(final InstructionGroup group)
    {
        AbstractInsnNode insn;
        for (final InstructionGraphNode node: group.getNodes()) {
            if (node == group.getRoot())
                continue;
            insn = node.getInstruction();
            method.instructions.remove(insn);
            group.getInstructions().add(insn);
        }
    }

    // create FieldNodes for all xLoad instructions
    private static void extractFields(final InstructionGroup group)
    {
        final List<FieldNode> fields = group.getFields();

        VarInsnNode insn;
        for (final InstructionGraphNode node : group.getNodes()) {
            if (!node.isXLoad())
                continue;

            insn = (VarInsnNode) node.getInstruction();

            // check whether we already have a field for the var with this index
            int index;
            for (index = 0; index < fields.size(); index++)
                if (fields.get(index).access == insn.var)
                    break;

            // if we don't, create a new field for the var
            if (index == fields.size()) {
                /*
                 * TODO: fix hack below
                 *
                 * CAUTION, HACK!: for brevity we reuse the access field and
                 * the value field of the FieldNode for keeping track of the
                 * original var index as well as the FieldNodes Type
                 * (respectively) so we need to make sure that we correct
                 * for this when the field is actually written
                 */
                final Type type = node.getResultValue().getType();
                fields.add(new FieldNode(insn.var, "field$" + index,
                    type.getDescriptor(), null, type));
            }

            // normalize the instruction so instruction groups that are
            // identical except for the variable indices are still mapped to
            // the same group class (name)
            insn.var = index;
        }
    }
}