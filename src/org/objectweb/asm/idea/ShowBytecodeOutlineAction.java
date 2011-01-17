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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.util.ASMifierClassVisitor;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.Semaphore;


/**
 * Given a java file (or any file which generates classes), tries to locate a .class file. If the compilation state is
 * not up to date, performs an automatic compilation of the class. If the .class file can be located, generates bytecode
 * instructions for the class and ASMified code, and displays them into a tool window.
 *
 * @author Cédric Champeau
 */
public class ShowBytecodeOutlineAction extends AnAction {

    private final static String NO_CLASS_FOUND = "// couldn't generate bytecode view, no .class file found";

    @Override
    public void update(final AnActionEvent e) {
        final Editor editor = e.getData(PlatformDataKeys.EDITOR);
        final VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        final Presentation presentation = e.getPresentation();
        if (editor == null || virtualFile == null) {
            presentation.setEnabled(false);
            return;
        }
        final Project project = editor.getProject();
        if (project == null) {
            presentation.setEnabled(false);
            return;
        }
        final PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        presentation.setEnabled(psiFile instanceof PsiClassOwner);
    }

    public void actionPerformed(AnActionEvent e) {
        final Editor editor = e.getData(PlatformDataKeys.EDITOR);
        final VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        if (editor == null) return;
        final Project project = editor.getProject();
        if (project == null || virtualFile == null) return;
        final PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (psiFile instanceof PsiClassOwner) {
            final Module module = ModuleUtil.findModuleForPsiElement(psiFile);
            final CompilerModuleExtension cme = CompilerModuleExtension.getInstance(module);
            final CompilerManager compilerManager = CompilerManager.getInstance(project);
            final VirtualFile[] files = {virtualFile};
            final CompileScope compileScope = compilerManager.createFilesCompileScope(files);
            if ("class".equals(virtualFile.getExtension())) {
                updateToolWindowContents(project, virtualFile);
            } else {
                final Application application = ApplicationManager.getApplication();
                application.runWriteAction(new Runnable() {
                    public void run() {
                        FileDocumentManager.getInstance().saveDocument(editor.getDocument());
                    }
                });
                application.executeOnPooledThread(new Runnable() {
                    public void run() {
                        final VirtualFile[] result = {null};
                        VirtualFile[] outputDirectories = cme == null ? null : cme.getOutputRoots(true);
                        if (outputDirectories != null && compilerManager.isUpToDate(compileScope)) {
                            result[0] = findClassFile(outputDirectories, psiFile);
                            if (result[0]==null && cme!=null) {
                                // check if file is in test output directory

                            }
                        } else {
                            final Semaphore semaphore = new Semaphore(1);
                            try {
                                semaphore.acquire();
                            } catch (InterruptedException e1) {
                                result[0] = null;
                            }
                            application.invokeLater(new Runnable() {
                                public void run() {
                                    compilerManager.compile(files, new CompileStatusNotification() {
                                        public void finished(boolean aborted, int errors, int warnings, final CompileContext compileContext) {
                                            if (errors == 0) {
                                                VirtualFile[] outputDirectories = cme.getOutputRoots(true);
                                                if (outputDirectories != null) {
                                                    result[0] = findClassFile(outputDirectories, psiFile);
                                                }
                                            }
                                            semaphore.release();
                                        }
                                    }, true);
                                }
                            });
                            try {
                                semaphore.acquire();
                            } catch (InterruptedException e1) {
                                result[0] = null;
                            }
                        }
                        application.invokeLater(new Runnable() {
                            public void run() {
                                updateToolWindowContents(project, result[0]);
                            }
                        });
                    }
                });
            }
        }
    }

    private VirtualFile findClassFile(final VirtualFile[] outputDirectories, final PsiFile psiFile) {
        return ApplicationManager.getApplication().runReadAction(new Computable<VirtualFile>() {
            public VirtualFile compute() {
                VirtualFile targetFile = null;
                if (outputDirectories != null && psiFile instanceof PsiClassOwner) {
                    PsiClassOwner psiJavaFile = (PsiClassOwner) psiFile;
                    for (PsiClass psiClass : psiJavaFile.getClasses()) {
                        final String qualifiedName = psiClass.getQualifiedName();
                        if (qualifiedName != null) {
                            final String path = qualifiedName.replace('.', '/') + ".class";
                            for (VirtualFile outputDirectory : outputDirectories) {
                                final VirtualFile file = outputDirectory.findFileByRelativePath(path);
                                if (file != null && file.exists()) {
                                    targetFile = file;
                                    break;
                                }
                            }
                            if (targetFile!=null) break;
                        }
                    }
                }
                return targetFile;
            }
        });
    }

    /**
     * Reads the .class file, processes it through the ASM TraceVisitor and ASMifier to update the contents of the two
     * tabs of the tool window.
     *
     * @param project the project instance
     * @param file    the class file
     */
    private void updateToolWindowContents(final Project project, final VirtualFile file) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
                if (file==null) {
                    BytecodeOutline.getInstance(project).setCode(NO_CLASS_FOUND);
                    BytecodeASMified.getInstance(project).setCode(NO_CLASS_FOUND);
                    ToolWindowManager.getInstance(project).getToolWindow("ASM").activate(null);
                    return;
                }
                StringWriter stringWriter = new StringWriter();
                ClassVisitor visitor = new TraceClassVisitor(new PrintWriter(stringWriter));
                ClassReader reader = null;
                try {
                    reader = new ClassReader(file.contentsToByteArray());
                } catch (IOException e) {
                    return;
                }
                reader.accept(visitor, 0);
                BytecodeOutline.getInstance(project).setCode(stringWriter.toString());
                stringWriter.getBuffer().setLength(0);
                visitor = new ASMifierClassVisitor(new PrintWriter(stringWriter));
                reader.accept(visitor, 0);
                final BytecodeASMified asmified = BytecodeASMified.getInstance(project);
                PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText("asm.java", stringWriter.toString());
                CodeStyleManager.getInstance(project).reformat(psiFile);
                asmified.setCode(psiFile.getText());
                ToolWindowManager.getInstance(project).getToolWindow("ASM").activate(null);
            }
        });
    }

}
