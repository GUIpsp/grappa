/*
 * Copyright (c) 2009-2010 Ken Wenzel and Mathias Doenitz
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.parboiled.transform.process;

import com.github.fge.grappa.exceptions.InvalidGrammarException;
import com.github.fge.grappa.transform.ClassCache;
import com.google.common.base.Preconditions;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import com.github.fge.grappa.transform.base.InstructionGraphNode;
import com.github.fge.grappa.transform.base.InstructionGroup;
import com.github.fge.grappa.transform.base.ParserClassNode;
import com.github.fge.grappa.transform.base.RuleMethod;

import javax.annotation.Nonnull;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.parboiled.transform.AsmUtils.getClassConstructor;
import static org.parboiled.transform.AsmUtils.getClassField;
import static org.parboiled.transform.AsmUtils.getClassMethod;

public final class InstructionGroupCreator
    implements RuleMethodProcessor
{
    private final Map<String, Integer> memberModifiers
        = new HashMap<>();
    private RuleMethod method;

    @Override
    public boolean appliesTo(@Nonnull final ParserClassNode classNode,
        @Nonnull final RuleMethod method)
    {
        Objects.requireNonNull(classNode, "classNode");
        Objects.requireNonNull(method, "method");
        return method.containsExplicitActions() || method.containsVars();
    }

    @Override
    public void process(@Nonnull final ParserClassNode classNode,
        @Nonnull final RuleMethod method)
    {
        this.method = Objects.requireNonNull(method, "method");

        // create groups
        createGroups();

        // prepare groups for later stages
        for (final InstructionGroup group: method.getGroups()) {
            sort(group);
            markUngroupedEnclosedNodes(group);
            verify(group);
        }

        // check all non-group node for illegal accesses
        for (final InstructionGraphNode node : method.getGraphNodes())
            if (node.getGroup() == null)
                verifyAccess(node);
    }

    private void createGroups()
    {
        InstructionGroup group;

        for (final InstructionGraphNode node: method.getGraphNodes()) {
            if (!(node.isActionRoot() || node.isVarInitRoot()))
                continue;

            group = new InstructionGroup(node);
            markGroup(node, group);
            method.getGroups().add(group);
        }
    }

    private void markGroup(
        final InstructionGraphNode node, final InstructionGroup group) {

        final boolean condition = node == group.getRoot()
            || !(node.isActionRoot() || node.isVarInitRoot());

        if (!condition)
            throw new InvalidGrammarException("method " + method.name
                + " contains illegal nested ACTION/Var constructs");

        if (node.getGroup() != null)
            return; // already visited

        node.setGroup(group);

        if (node.isXLoad())
            return;

        if (!node.isVarInitRoot()) {
            for (final InstructionGraphNode pred : node.getPredecessors())
                markGroup(pred, group);
            return;
        }

        if (node.getPredecessors().size() != 2)
            throw new InvalidGrammarException("FIXME: find error message");
        // only color the second predecessor branch
        markGroup(node.getPredecessors().get(1), group);
    }

    // sort the group instructions according to their method index
    private void sort(final InstructionGroup group)
    {
        final Comparator<InstructionGraphNode> comparator
            = new MethodIndexComparator(method.instructions);
        Collections.sort(group.getNodes(), comparator);
    }

    // also capture all group nodes "hidden" behind xLoads
    private void markUngroupedEnclosedNodes(final InstructionGroup group)
    {
        InstructionGraphNode node;
        boolean keepGoing;
        List<InstructionGraphNode> graphNodes;
        int startIndex, endIndex;

        do {
            keepGoing = false;
            graphNodes = method.getGraphNodes();
            startIndex = getIndexOfFirstInsn(group);
            endIndex = getIndexOfLastInsn(group);

            for (int i = startIndex; i < endIndex; i++) {
                node = graphNodes.get(i);
                if (node.getGroup() != null)
                    continue;

                markGroup(node, group);
                sort(group);
                keepGoing = true;
            }
        } while (keepGoing);
    }

    private void verify(final InstructionGroup group)
    {
        final List<InstructionGraphNode> nodes = group.getNodes();
        final int sizeMinus1 = nodes.size() - 1;

        // verify all instruction except for the last one (which must be the
        // root)
        Preconditions.checkState(nodes.get(sizeMinus1) == group.getRoot());

        InstructionGraphNode node;

        for (int i = 0; i < sizeMinus1; i++) {
            node = nodes.get(i);

            if (node.isXStore())
                throw new InvalidGrammarException("An action or Var initializer"
                    + " in method " + method.name + " contains an illegal write"
                    + " to a local variable/parameter");

            verifyAccess(node);
        }

        final int i = getIndexOfLastInsn(group) - getIndexOfFirstInsn(group);

        if (i == sizeMinus1)
            return;

        throw new InvalidGrammarException("error during bytecode analysis of" +
            " rule method " + method.name + ": discontinuous group block");
    }

    private void verifyAccess(final InstructionGraphNode node)
    {
        switch (node.getInstruction().getOpcode()) {
            case GETFIELD:
            case GETSTATIC:
                final FieldInsnNode field
                    = (FieldInsnNode) node.getInstruction();

                if (isPrivateField(field.owner, field.name))
                    throw new InvalidGrammarException("rule methods cannot "
                        + "access private fields (method: " + method.name
                        + ", field: " + field.name + ')');
                break;

            case INVOKEVIRTUAL:
            case INVOKESTATIC:
            case INVOKESPECIAL:
            case INVOKEINTERFACE:
                final MethodInsnNode calledMethod
                    = (MethodInsnNode) node.getInstruction();
                if (isPrivate(calledMethod.owner, calledMethod.name,
                    calledMethod.desc))
                    throw new InvalidGrammarException("method " + method.name
                        + " contains an illegal call to private method "
                        + calledMethod.name + "; make the latter protected or"
                        + " package private");
                break;
        }
    }

    private int getIndexOfFirstInsn(final InstructionGroup group)
    {
        return method.instructions
            .indexOf(group.getNodes().get(0).getInstruction());
    }

    private int getIndexOfLastInsn(final InstructionGroup group)
    {
        final List<InstructionGraphNode> graphNodes = group.getNodes();
        return method.instructions
            .indexOf(graphNodes.get(graphNodes.size() - 1).getInstruction());
    }

    private boolean isPrivateField(final String owner, final String name)
    {
        final String key = owner + '#' + name;
        Integer modifiers = memberModifiers.get(key);
        if (modifiers == null) {
            modifiers = getClassField(owner, name).getModifiers();
            memberModifiers.put(key, modifiers);
        }
        return Modifier.isPrivate(modifiers);
    }

    private boolean isPrivate(final String owner, final String name,
        final String desc)
    {
        return "<init>".equals(name) ? isPrivateInstantiation(owner, desc)
            : isPrivateMethod(owner, name, desc);
    }

    private boolean isPrivateMethod(final String owner, final String name,
        final String desc)
    {
        final String key = owner + '#' + name + '#' + desc;
        Integer modifiers = memberModifiers.get(key);
        if (modifiers == null) {
            modifiers = getClassMethod(owner, name, desc).getModifiers();
            memberModifiers.put(key, modifiers);
        }
        return Modifier.isPrivate(modifiers);
    }

    private boolean isPrivateInstantiation(final String owner,
        final String desc)
    {
        // first check whether the class is private
        Integer modifiers = memberModifiers.get(owner);
        if (modifiers == null) {
            modifiers = ClassCache.INSTANCE.loadClass(owner).getModifiers();
            //modifiers = getClassForInternalName(owner).getModifiers();
            memberModifiers.put(owner, modifiers);
        }
        if (Modifier.isPrivate(modifiers))
            return true;

        // then check whether the selected constructor is private
        final String key = owner + "#<init>#" + desc;
        modifiers = memberModifiers.get(key);
        if (modifiers == null) {
            modifiers = getClassConstructor(owner, desc).getModifiers();
            memberModifiers.put(key, modifiers);
        }
        return Modifier.isPrivate(modifiers);
    }

    private static final class MethodIndexComparator
        implements Comparator<InstructionGraphNode>
    {
        private final InsnList instructions;

        private MethodIndexComparator(@Nonnull final InsnList instructions)
        {
            this.instructions = Objects.requireNonNull(instructions);
        }

        @Override
        public int compare(final InstructionGraphNode o1,
            final InstructionGraphNode o2)
        {
            final int i1 = instructions.indexOf(o1.getInstruction());
            final int i2 = instructions.indexOf(o2.getInstruction());
            return Integer.compare(i1, i2);
        }
    }
}
