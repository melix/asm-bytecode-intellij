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
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.objectweb.asm.idea.config.ASMPluginComponent;
import reloc.org.objectweb.asm.ClassReader;
import reloc.org.objectweb.asm.ClassVisitor;
import reloc.org.objectweb.asm.util.ASMifier;
import reloc.org.objectweb.asm.util.TraceClassVisitor;

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

    @Override
    public void update(final AnActionEvent e) {
        final VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        final Project project = e.getData(PlatformDataKeys.PROJECT);
        final Presentation presentation = e.getPresentation();
        if (project == null || virtualFile == null) {
            presentation.setEnabled(false);
            return;
        }
        final PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        presentation.setEnabled(psiFile instanceof PsiClassOwner);
    }

    public void actionPerformed(AnActionEvent e) {
        final VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        final Project project = e.getData(PlatformDataKeys.PROJECT);
        if (project == null || virtualFile == null) return;
        final PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (psiFile instanceof PsiClassOwner) {
            final Module module = ModuleUtil.findModuleForPsiElement(psiFile);
            final CompilerModuleExtension cme = module != null ? CompilerModuleExtension.getInstance(module) : null;
            final CompilerManager compilerManager = CompilerManager.getInstance(project);
            final VirtualFile[] files = {virtualFile};
            if ("class".equals(virtualFile.getExtension())) {
                updateToolWindowContents(project, virtualFile);
            } else if (!virtualFile.isInLocalFileSystem() && !virtualFile.isWritable()) {
                // probably a source file in a library
                final PsiClass[] psiClasses = ((PsiClassOwner) psiFile).getClasses();
                if (psiClasses.length>0) {
                    updateToolWindowContents(project, psiClasses[0].getOriginalElement().getContainingFile().getVirtualFile());
                }
            } else {
                final Application application = ApplicationManager.getApplication();
                application.runWriteAction(new Runnable() {
                    public void run() {
                        FileDocumentManager.getInstance().saveAllDocuments();
                    }
                });
                application.executeOnPooledThread(new Runnable() {
                    public void run() {
                        final CompileScope compileScope = compilerManager.createFilesCompileScope(files);
                        final VirtualFile[] result = {null};
                        final VirtualFile[] outputDirectories = cme == null ? null : cme.getOutputRoots(true);
                        final Semaphore semaphore = new Semaphore(1);
                        try {
                            semaphore.acquire();
                        } catch (InterruptedException e1) {
                            result[0] = null;
                        }
                        if (outputDirectories != null && compilerManager.isUpToDate(compileScope)) {
                          application.invokeLater( new Runnable() {
                            public void run() {
                              result[0] = findClassFile( outputDirectories, psiFile );
                              semaphore.release();
                            }
                          } );
                        } else {
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
                                    });
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
            if( outputDirectories != null && psiFile instanceof PsiClassOwner ) {
              FileEditor editor = FileEditorManager.getInstance( psiFile.getProject() ).getSelectedEditor( psiFile.getVirtualFile() );
              int caretOffset = editor == null ? -1 : ((PsiAwareTextEditorImpl)editor).getEditor().getCaretModel().getOffset();
              if( caretOffset >= 0 ) {
                PsiClass psiClass = findClassAtCaret( psiFile, caretOffset );
                if( psiClass != null ) {
                  return getClassFile( psiClass );
                }
              }
              PsiClassOwner psiJavaFile = (PsiClassOwner)psiFile;
              for( PsiClass psiClass : psiJavaFile.getClasses() ) {
                final VirtualFile file = getClassFile( psiClass );
                if( file != null ) {
                  return file;
                }
              }
            }
            return null;
          }

          private VirtualFile getClassFile( PsiClass psiClass ) {
            StringBuilder sb = new StringBuilder( psiClass.getQualifiedName() );
            while( psiClass.getContainingClass() != null ) {
              sb.setCharAt( sb.lastIndexOf( "." ), '$' );
              psiClass = psiClass.getContainingClass();
            }
            String classFileName = sb.toString().replace( '.', '/' ) + ".class";
            for( VirtualFile outputDirectory : outputDirectories ) {
              final VirtualFile file = outputDirectory.findFileByRelativePath( classFileName );
              if( file != null && file.exists() ) {
                return file;
              }
            }
            return null;
          }

          private PsiClass findClassAtCaret( PsiFile psiFile, int caretOffset ) {
            PsiElement elem = psiFile.findElementAt( caretOffset );
            while( elem != null ) {
              if( elem instanceof PsiClass ) {
                return (PsiClass)elem;
              }
              elem = elem.getParent();
            }
            return null;
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
                    BytecodeOutline.getInstance(project).setCode(file, Constants.NO_CLASS_FOUND);
                    BytecodeASMified.getInstance(project).setCode(file, Constants.NO_CLASS_FOUND);
                    GroovifiedView.getInstance(project).setCode(file, Constants.NO_CLASS_FOUND);
                    ToolWindowManager.getInstance(project).getToolWindow("ASM").activate(null);
                    return;
                }
                StringWriter stringWriter = new StringWriter();
                ClassVisitor visitor = new TraceClassVisitor(new PrintWriter(stringWriter));
                ClassReader reader = null;
                try {
                    file.refresh(false, false);
                    reader = new ClassReader(file.contentsToByteArray());
                } catch (IOException e) {
                    return;
                }
                int flags = 0;
                final ASMPluginComponent config = project.getComponent(ASMPluginComponent.class);
                if (config.isSkipDebug()) flags = flags | ClassReader.SKIP_DEBUG;
                if (config.isSkipFrames()) flags = flags | ClassReader.SKIP_FRAMES;
                if (config.isExpandFrames()) flags = flags | ClassReader.EXPAND_FRAMES;
                if (config.isSkipCode()) flags = flags | ClassReader.SKIP_CODE;

                reader.accept(visitor, flags);
                BytecodeOutline.getInstance(project).setCode(file, stringWriter.toString());
                stringWriter.getBuffer().setLength(0);
                reader.accept(new TraceClassVisitor(null, new GroovifiedTextifier(config.getCodeStyle()), new PrintWriter(stringWriter)), ClassReader.SKIP_FRAMES|ClassReader.SKIP_DEBUG);
                GroovifiedView.getInstance(project).setCode(file,stringWriter.toString());
                stringWriter.getBuffer().setLength(0);
                reader.accept(new TraceClassVisitor(null,
                        new ASMifier(),
                        new PrintWriter(stringWriter)), flags);
                final BytecodeASMified asmified = BytecodeASMified.getInstance(project);
                PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText("asm.java", stringWriter.toString());
                CodeStyleManager.getInstance(project).reformat(psiFile);
                asmified.setCode(file,psiFile.getText());
                ToolWindowManager.getInstance(project).getToolWindow("ASM").activate(null);
            }
        });
    }
}
