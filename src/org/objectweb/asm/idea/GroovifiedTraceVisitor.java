/*
 *
 *  Copyright 2011 Cédric Champeau
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */

package org.objectweb.asm.idea;
/**
 * Created by IntelliJ IDEA.
 * User: cedric
 * Date: 17/01/11
 * Time: 22:07
 */

import org.objectweb.asm.*;
import org.objectweb.asm.commons.EmptyVisitor;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.util.AbstractVisitor;
import org.objectweb.asm.util.TraceAnnotationVisitor;
import org.objectweb.asm.util.TraceClassVisitor;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;

/**
 * A customized trace visitor which outputs code compatible with the Groovy @groovyx.ast.bytecode.Bytecode AST
 * transform.
 */
public class GroovifiedTraceVisitor extends TraceClassVisitor {

    public GroovifiedTraceVisitor(PrintWriter pw) {
        super(pw);
    }

    @Override
    protected TraceMethodVisitor createTraceMethodVisitor() {
        return new GroovyTraceMethodVisitor();
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
        buf.setLength(0);
        buf.append('\n');
        if ((access & Opcodes.ACC_DEPRECATED) != 0) {
            buf.append(tab).append("// DEPRECATED\n");
        }
        buf.append(tab).append("// access flags 0x").append(Integer.toHexString(access).toUpperCase()).append('\n');
        buf.append(tab).append("@groovyx.ast.bytecode.Bytecode\n");
        Method method = new Method(name, desc);

        buf.append(tab);
        appendAccess(access);
        if ((access & Opcodes.ACC_NATIVE) != 0) {
            buf.append("native ");
        }
        buf.append(method.getReturnType().getClassName());
        buf.append(' ');
        buf.append(name);
        buf.append('(');
        final Type[] argumentTypes = method.getArgumentTypes();
        char arg = 'a';
        for (int j = 0, argumentTypesLength = argumentTypes.length; j < argumentTypesLength; j++) {
            final Type type = argumentTypes[j];
            buf.append(type.getClassName());
            buf.append(' ');
            buf.append(arg);
            if (j < argumentTypesLength - 1) buf.append(',');
            arg++;
        }
        buf.append(')');
        if (exceptions != null && exceptions.length > 0) {
            buf.append(" throws ");
            for (int i = 0; i < exceptions.length; ++i) {
                appendDescriptor(INTERNAL_NAME, exceptions[i].replace('/', '.'));
                if (i < exceptions.length - 1) buf.append(',');
            }
        }
        buf.append(" {");
        buf.append('\n');
        text.add(buf.toString());

        GroovyTraceMethodVisitor tcv = (GroovyTraceMethodVisitor) createTraceMethodVisitor();
        text.add(tcv.getText());
        text.add("  }\n");
        if (cv != null) {
            tcv.setMethodVisitor(cv.visitMethod(access, name, desc, signature, exceptions));
        }

        return tcv;
    }

    /**
     * Appends a string representation of the given access modifiers to {@link #buf buf}.
     *
     * @param access some access modifiers.
     */
    private void appendAccess(final int access) {
        if ((access & Opcodes.ACC_PUBLIC) != 0) {
            buf.append("public ");
        }
        if ((access & Opcodes.ACC_PRIVATE) != 0) {
            buf.append("private ");
        }
        if ((access & Opcodes.ACC_PROTECTED) != 0) {
            buf.append("protected ");
        }
        if ((access & Opcodes.ACC_FINAL) != 0) {
            buf.append("final ");
        }
        if ((access & Opcodes.ACC_STATIC) != 0) {
            buf.append("static ");
        }
        if ((access & Opcodes.ACC_SYNCHRONIZED) != 0) {
            buf.append("synchronized ");
        }
        if ((access & Opcodes.ACC_VOLATILE) != 0) {
            buf.append("volatile ");
        }
        if ((access & Opcodes.ACC_TRANSIENT) != 0) {
            buf.append("transient ");
        }
        if ((access & Opcodes.ACC_ABSTRACT) != 0) {
            buf.append("abstract ");
        }
        if ((access & Opcodes.ACC_STRICT) != 0) {
            buf.append("strictfp ");
        }
        if ((access & Opcodes.ACC_ENUM) != 0) {
            buf.append("enum ");
        }
    }

    protected static class GroovyTraceMethodVisitor extends TraceMethodVisitor {

        private static final EmptyVisitor EMPTY_ANNOTATION_VISITOR = new EmptyVisitor();

        @Override
        public void visitFrame(final int type, final int nLocal, final Object[] local, final int nStack, final Object[] stack) {
            // frames are not supported
        }

        @Override
        public void visitLineNumber(final int line, final Label start) {
            // line numbers are not necessary
        }

        public void visitInsn(final int opcode) {
            buf.setLength(0);
            buf.append(tab2).append(OPCODES[opcode].toLowerCase()).append('\n');
            text.add(buf.toString());

            if (mv != null) {
                mv.visitInsn(opcode);
            }
        }

        public void visitIntInsn(final int opcode, final int operand) {
            buf.setLength(0);
            buf.append(tab2)
                    .append(OPCODES[opcode].toLowerCase())
                    .append(' ')
                    .append(opcode == Opcodes.NEWARRAY
                            ? TYPES[operand]
                            : Integer.toString(operand))
                    .append('\n');
            text.add(buf.toString());

            if (mv != null) {
                mv.visitIntInsn(opcode, operand);
            }
        }

        public void visitVarInsn(final int opcode, final int var) {
            buf.setLength(0);
            buf.append(tab2)
                    .append(OPCODES[opcode].toLowerCase())
                    .append(' ')
                    .append(var)
                    .append('\n');
            text.add(buf.toString());

            if (mv != null) {
                mv.visitVarInsn(opcode, var);
            }
        }

        public void visitTypeInsn(final int opcode, final String type) {
            buf.setLength(0);
            final String opcodeStr = OPCODES[opcode];
            buf.append(tab2).append("NEW".equals(opcodeStr) ? "_new" : "INSTANCEOF".equals(opcodeStr) ? "_instanceof" : opcodeStr.toLowerCase()).append(' ');
            buf.append('\'');
            appendDescriptor(INTERNAL_NAME, type);
            buf.append('\'');
            buf.append('\n');
            text.add(buf.toString());

            if (mv != null) {
                mv.visitTypeInsn(opcode, type);
            }
        }

        public void visitFieldInsn(
                final int opcode,
                final String owner,
                final String name,
                final String desc) {
            buf.setLength(0);
            buf.append(tab2).append(OPCODES[opcode].toLowerCase()).append(' ');
            buf.append('\'');
            appendDescriptor(INTERNAL_NAME, owner);
            buf.append('.').append(name).append("','");
            appendDescriptor(FIELD_DESCRIPTOR, desc);
            buf.append('\'');
            buf.append('\n');
            text.add(buf.toString());

            if (mv != null) {
                mv.visitFieldInsn(opcode, owner, name, desc);
            }
        }

        public void visitMethodInsn(
                final int opcode,
                final String owner,
                final String name,
                final String desc) {
            buf.setLength(0);
            buf.append(tab2).append(OPCODES[opcode].toLowerCase()).append(' ');
            buf.append('\'');
            appendDescriptor(INTERNAL_NAME, owner);
            buf.append('.').append(name).append("','");
            appendDescriptor(METHOD_DESCRIPTOR, desc);
            buf.append('\'');
            buf.append('\n');
            text.add(buf.toString());

            if (mv != null) {
                mv.visitMethodInsn(opcode, owner, name, desc);
            }
        }

        public void visitJumpInsn(final int opcode, final Label label) {
            buf.setLength(0);
            buf.append(tab2).append(OPCODES[opcode].equals("GOTO") ? "_goto" : OPCODES[opcode].toLowerCase()).append(' ');
            appendLabel(label);
            buf.append('\n');
            text.add(buf.toString());

            if (mv != null) {
                mv.visitJumpInsn(opcode, label);
            }
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(final int parameter, final String desc, final boolean visible) {
            return EMPTY_ANNOTATION_VISITOR;
        }

        @Override
        public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
            return EMPTY_ANNOTATION_VISITOR;
        }

        @Override
        public AnnotationVisitor visitAnnotationDefault() {
            return EMPTY_ANNOTATION_VISITOR;
        }

        /**
         * Appends the name of the given label to {@link #buf buf}. Creates a new label name if the given label does not
         * yet have one.
         *
         * @param l a label.
         */
        protected void appendLabel(final Label l) {
            String name = (String) labelNames.get(l);
            if (name == null) {
                name = "l" + labelNames.size();
                labelNames.put(l, name);
            }
            buf.append(name);
        }

        public void visitLabel(final Label label) {
            buf.setLength(0);
            buf.append(ltab);
            appendLabel(label);
            buf.append('\n');
            text.add(buf.toString());

            if (mv != null) {
                mv.visitLabel(label);
            }
        }

        public void visitLdcInsn(final Object cst) {
            buf.setLength(0);
            buf.append(tab2).append("ldc ");
            if (cst instanceof String) {
                AbstractVisitor.appendString(buf, (String) cst);
            } else if (cst instanceof Type) {
                buf.append(((Type) cst).getDescriptor()).append(".class");
            } else if (cst instanceof Float) {
                buf.append(cst).append('f');
            } else if (cst instanceof Double) {
                buf.append(cst).append('d');
            } else if (cst instanceof Integer) {
                buf.append(cst).append('i');
            } else {
                buf.append(cst);
            }
            buf.append('\n');
            text.add(buf.toString());

            if (mv != null) {
                mv.visitLdcInsn(cst);
            }
        }

        public void visitIincInsn(final int var, final int increment) {
            buf.setLength(0);
            buf.append(tab2)
                    .append("iinc ")
                    .append(var)
                    .append(' ')
                    .append(increment)
                    .append('\n');
            text.add(buf.toString());

            if (mv != null) {
                mv.visitIincInsn(var, increment);
            }
        }

        public void visitTableSwitchInsn(
                final int min,
                final int max,
                final Label dflt,
                final Label[] labels) {
            buf.setLength(0);
            buf.append(tab2).append("tableswitch(\n");
            for (int i = 0; i < labels.length; ++i) {
                buf.append(tab3).append(min + i).append(": ");
                appendLabel(labels[i]);
                buf.append(",\n");
            }
            buf.append(tab3).append("default: ");
            appendLabel(dflt);
            buf.append(tab2).append("\n)\n");
            text.add(buf.toString());

            if (mv != null) {
                mv.visitTableSwitchInsn(min, max, dflt, labels);
            }
        }

        public void visitLookupSwitchInsn(
                final Label dflt,
                final int[] keys,
                final Label[] labels) {
            buf.setLength(0);
            buf.append(tab2).append("lookupswitch(\n");
            for (int i = 0; i < labels.length; ++i) {
                buf.append(tab3).append(keys[i]).append(": ");
                appendLabel(labels[i]);
                buf.append(",\n");
            }
            buf.append(tab3).append("default: ");
            appendLabel(dflt);
            buf.append(tab2).append("\n)\n");
            text.add(buf.toString());

            if (mv != null) {
                mv.visitLookupSwitchInsn(dflt, keys, labels);
            }
        }

        public void visitMultiANewArrayInsn(final String desc, final int dims) {
            buf.setLength(0);
            buf.append(tab2).append("multianewarray ");
            buf.append('\'');
            appendDescriptor(FIELD_DESCRIPTOR, desc);
            buf.append("\',").append(dims).append('\n');
            text.add(buf.toString());

            if (mv != null) {
                mv.visitMultiANewArrayInsn(desc, dims);
            }
        }

        public void visitTryCatchBlock(
                final Label start,
                final Label end,
                final Label handler,
                final String type) {
            buf.setLength(0);
            buf.append(tab2).append("trycatchblock ");
            appendLabel(start);
            buf.append(',');
            appendLabel(end);
            buf.append(',');
            appendLabel(handler);
            buf.append(',');
            if (type != null) {
                buf.append('\'');
                appendDescriptor(INTERNAL_NAME, type);
                buf.append('\'');
            } else {
                appendDescriptor(INTERNAL_NAME, type);
            }
            buf.append('\n');
            text.add(buf.toString());

            if (mv != null) {
                mv.visitTryCatchBlock(start, end, handler, type);
            }
        }

        @Override
        protected TraceAnnotationVisitor createTraceAnnotationVisitor() {
            return new TraceAnnotationVisitor() {
                @Override
                public AnnotationVisitor visitAnnotation(final String name, final String desc) {
                    return EMPTY_ANNOTATION_VISITOR;
                }
            };
        }

        @Override
        public void visitLocalVariable(final String name, final String desc, final String signature, final Label start, final Label end, final int index) {
        }

        @Override
        public void visitMaxs(final int maxStack, final int maxLocals) {
        }

        public void setMethodVisitor(final MethodVisitor methodVisitor) {
            this.mv = methodVisitor;
        }
    }


}
