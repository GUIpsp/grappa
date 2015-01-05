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

import com.github.parboiled1.grappa.transform.CodeBlock;
import com.google.common.base.Preconditions;
import me.qmx.jitescript.util.CodegenUtils;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LocalVariableNode;
import org.parboiled.Rule;
import org.parboiled.matchers.VarFramingMatcher;
import org.parboiled.support.Var;
import org.parboiled.transform.ParserClassNode;
import org.parboiled.transform.RuleMethod;

import javax.annotation.Nonnull;

import static org.objectweb.asm.Opcodes.ARETURN;

/**
 * Inserts code for wrapping the created rule into a VarFramingMatcher if the
 * method contains local variables assignable to {@link Var}.
 */
public final class VarFramingGenerator
    implements RuleMethodProcessor
{
    @Override
    public boolean appliesTo(@Nonnull final ParserClassNode classNode,
        @Nonnull final RuleMethod method)
    {
        Preconditions.checkNotNull(classNode, "classNode");
        Preconditions.checkNotNull(method, "method");
        return !method.getLocalVarVariables().isEmpty();
    }

    @Override
    public void process(@Nonnull final ParserClassNode classNode,
        @Nonnull final RuleMethod method)
        throws Exception
    {
        Preconditions.checkNotNull(classNode, "classNode");
        Preconditions.checkNotNull(method, "method");
        final InsnList instructions = method.instructions;

        AbstractInsnNode ret = instructions.getLast();
        while (ret.getOpcode() != ARETURN)
            ret = ret.getPrevious();

        final CodeBlock block = CodeBlock.newCodeBlock();

        block.newobj(CodegenUtils.p(VarFramingMatcher.class))
            .dup_x1()
            .swap();

        createVarFieldArray(block, method);

        block.invokespecial(CodegenUtils.p(VarFramingMatcher.class), "<init>",
            CodegenUtils.sig(void.class, Rule.class, Var[].class));

        instructions.insertBefore(ret, block.getInstructionList());

        method.setBodyRewritten();
    }

    private static void createVarFieldArray(final CodeBlock block,
        final RuleMethod method)
    {
        final int count = method.getLocalVarVariables().size();

        block.bipush(count).anewarray(CodegenUtils.p(Var.class));

        LocalVariableNode var;
        String varName;

        for (int i = 0; i < count; i++) {
            var = method.getLocalVarVariables().get(i);
            varName = method.name + ':' + var.name;

            block.dup()
                .bipush(i)
                .aload(var.index)
                .dup()
                .ldc(varName)
                .invokevirtual(CodegenUtils.p(Var.class), "setName",
                    CodegenUtils.sig(void.class, String.class))
                .aastore();
        }
    }
}