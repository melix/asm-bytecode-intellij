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
import reloc.org.objectweb.asm.*;
import reloc.org.objectweb.asm.Label;
import reloc.org.objectweb.asm.Opcodes;
import reloc.org.objectweb.asm.Type;
import reloc.org.objectweb.asm.commons.Method;
import org.objectweb.asm.idea.config.GroovyCodeStyle;
import reloc.org.objectweb.asm.util.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * A customized trace visitor which outputs code compatible with the Groovy @groovyx.ast.bytecode.Bytecode AST
 * transform.
 */
public class GroovifiedTextifier extends Textifier {

    private final static String[] GROOVY_DEFAULT_IMPORTS = {
            "java.io.",
            "java.lang.",
            "java.net.",
            "java.util.",
            "groovy.lang.",
            "groovy.util."
    };

    private final static String[] ATYPES;

    static {
        ATYPES = new String[12];
        String s = "boolean,char,float,double,byte,short,int,long,";
        int j = 0;
        int i = 4;
        int l;
        while ((l = s.indexOf(',', j)) > 0) {
            ATYPES[i++] = s.substring(j, l);
            j = l + 1;
        }
    }

    private final GroovyCodeStyle codeStyle;

    public GroovifiedTextifier(final GroovyCodeStyle codeStyle) {
        super(Opcodes.ASM5);
        this.codeStyle = codeStyle;
    }

    protected Textifier createTextifier() {
        return new GroovifiedMethodTextifier(codeStyle);
    }

    @Override
    public Textifier visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
        buf.setLength(0);
        buf.append('\n');
        if ((access & Opcodes.ACC_DEPRECATED) != 0) {
            buf.append(tab).append("// @Deprecated\n");
        }
        buf.append(tab).append("@groovyx.ast.bytecode.Bytecode\n");
        Method method = new Method(name, desc);

        buf.append(tab);
        appendAccess(access);
        if ((access & Opcodes.ACC_NATIVE) != 0) {
            buf.append("native ");
        }
        buf.append(groovyClassName(method.getReturnType().getClassName()));
        buf.append(' ');
        buf.append(name);
        buf.append('(');
        final Type[] argumentTypes = method.getArgumentTypes();
        char arg = 'a';
        for (int j = 0, argumentTypesLength = argumentTypes.length; j < argumentTypesLength; j++) {
            final Type type = argumentTypes[j];
            buf.append(groovyClassName(type.getClassName()));
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

        GroovifiedMethodTextifier tcv = (GroovifiedMethodTextifier) createTextifier();
        text.add(tcv.getText());
        text.add("  }\n");
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

    private static String groovyClassName(String className) {
        for (String anImport : GROOVY_DEFAULT_IMPORTS) {
            if (className.startsWith(anImport)) return className.substring(anImport.length());
        }
        return className;
    }

    protected static class GroovifiedMethodTextifier extends Textifier {

        private final GroovyCodeStyle codeStyle;
        private static final Textifier EMPTY_TEXTIFIER = new Textifier(Opcodes.ASM5) {
            @Override
            public List<Object> getText() {
                return Collections.emptyList();
            }
        };

        public GroovifiedMethodTextifier(final GroovyCodeStyle codeStyle) {
            super(Opcodes.ASM5);
            this.codeStyle = codeStyle;
        }

        private boolean isLegacy() {
            return codeStyle == GroovyCodeStyle.LEGACY;
        }

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
        }

        public void visitIntInsn(final int opcode, final int operand) {
            buf.setLength(0);
            buf.append(tab2)
                    .append(OPCODES[opcode].toLowerCase())
                    .append(' ')
                    .append(opcode == Opcodes.NEWARRAY
                            ? (isLegacy() ? TYPES[operand] : ATYPES[operand])
                            : Integer.toString(operand))
                    .append('\n');
            text.add(buf.toString());
        }

        public void visitVarInsn(final int opcode, final int var) {
            buf.setLength(0);
            buf.append(tab2)
                    .append(OPCODES[opcode].toLowerCase())
                    .append(' ')
                    .append(var)
                    .append('\n');
            text.add(buf.toString());
        }

        public void visitTypeInsn(final int opcode, final String type) {
            buf.setLength(0);
            final String opcodeStr = OPCODES[opcode];
            buf.append(tab2).append("NEW".equals(opcodeStr) ?
                    (isLegacy() ? "_new" : "newobject")
                    : "INSTANCEOF".equals(opcodeStr) ?
                    (isLegacy() ? "_instanceof" : "instance of:") : opcodeStr.toLowerCase()).append(' ');
            if (isLegacy()) {
                buf.append('\'');
                appendDescriptor(INTERNAL_NAME, type);
                buf.append('\'');
            } else {
                buf.append(groovyClassName(type.replace('/', '.')));
            }
            buf.append('\n');
            text.add(buf.toString());
        }

        public void visitFieldInsn(
                final int opcode,
                final String owner,
                final String name,
                final String desc) {
            buf.setLength(0);
            buf.append(tab2).append(OPCODES[opcode].toLowerCase()).append(' ');
            if (isLegacy()) {
                buf.append('\'');
                appendDescriptor(INTERNAL_NAME, owner);
                buf.append('.').append(name).append("','");
                appendDescriptor(FIELD_DESCRIPTOR, desc);
                buf.append('\'');
            } else {
                buf.append(groovyClassName(Type.getObjectType(owner).getClassName()));
                buf.append('.');
                buf.append(name);
                buf.append(" >> ");
                buf.append(groovyClassName(Type.getReturnType(desc).getClassName()));
            }
            buf.append('\n');
            text.add(buf.toString());

        }

        public void visitMethodInsn(
                final int opcode,
                final String owner,
                final String name,
                final String desc) {
            buf.setLength(0);
            buf.append(tab2).append(OPCODES[opcode].toLowerCase()).append(' ');
            if (isLegacy()) {
                buf.append('\'');
                appendDescriptor(INTERNAL_NAME, owner);
                buf.append('.').append(name).append("','");
                appendDescriptor(METHOD_DESCRIPTOR, desc);
                buf.append('\'');
            } else {
                buf.append(groovyClassName(Type.getObjectType(owner).getClassName()));
                buf.append('.');
                if ("<init>".equals(name)) buf.append('"');
                buf.append(name);
                if ("<init>".equals(name)) buf.append('"');
                buf.append('(');
                final Type[] types = Type.getArgumentTypes(desc);
                for (int i = 0; i < types.length; i++) {
                    Type type = types[i];
                    buf.append(groovyClassName(type.getClassName()));
                    if (i < types.length - 1) buf.append(',');
                }
                buf.append(") >> ");
                buf.append(groovyClassName(Type.getReturnType(desc).getClassName()));
            }
            buf.append('\n');
            text.add(buf.toString());
        }

        public void visitJumpInsn(final int opcode, final Label label) {
            buf.setLength(0);
            buf.append(tab2).append(
                    OPCODES[opcode].equals("GOTO") ?
                            (isLegacy() ? "_goto" : "go to:")
                            : OPCODES[opcode].toLowerCase()).append(' ');
            appendLabel(label);
            buf.append('\n');
            text.add(buf.toString());
        }

        @Override
        public Textifier visitParameterAnnotation(final int parameter, final String desc, final boolean visible) {
            return EMPTY_TEXTIFIER;
        }

        @Override
        public Textifier visitAnnotation(final String desc, final boolean visible) {
            return EMPTY_TEXTIFIER;
        }

        @Override
        public Textifier visitAnnotationDefault() {
            return EMPTY_TEXTIFIER;
        }

        /**
         * Appends the name of the given label to {@link #buf buf}. Creates a new label name if the given label does not
         * yet have one.
         *
         * @param l a label.
         */
        protected void appendLabel(final Label l) {
            if (labelNames == null) {
                labelNames = new HashMap<reloc.org.objectweb.asm.Label, String>();
            }
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
            if (codeStyle == GroovyCodeStyle.GROOVIFIER_0_2_0) buf.append(':');
            buf.append('\n');
            text.add(buf.toString());
        }

        public void visitLdcInsn(final Object cst) {
            buf.setLength(0);
            buf.append(tab2).append("ldc ");
            if (cst instanceof String) {
                Printer.appendString(buf, (String) cst);
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

        }

        public void visitIincInsn(final int var, final int increment) {
            buf.setLength(0);
            buf.append(tab2)
                    .append("iinc ")
                    .append(var)
                    .append(',')
                    .append(increment)
                    .append('\n');
            text.add(buf.toString());
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
        }

        public void visitMultiANewArrayInsn(final String desc, final int dims) {
            buf.setLength(0);
            buf.append(tab2).append("multianewarray ");
            if (isLegacy()) {
                buf.append('\'');
                appendDescriptor(FIELD_DESCRIPTOR, desc);
                buf.append("\'");
            } else {
                buf.append(groovyClassName(Type.getType(desc).getClassName()));
            }
            buf.append(',').append(dims).append('\n');
            text.add(buf.toString());
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
                if (isLegacy()) {
                    buf.append('\'');
                    appendDescriptor(INTERNAL_NAME, type);
                    buf.append('\'');
                } else {
                    buf.append(groovyClassName(type.replace('/', '.')));
                }
            } else {
                appendDescriptor(INTERNAL_NAME, type);
            }
            buf.append('\n');
            text.add(buf.toString());

        }

        @Override
        public void visitLocalVariable(final String name, final String desc, final String signature, final Label start, final Label end, final int index) {
        }

        @Override
        public void visitMaxs(final int maxStack, final int maxLocals) {
        }

    }


}
