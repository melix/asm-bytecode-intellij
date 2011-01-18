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
 * Date: 07/01/11
 * Time: 22:18
 */

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diff.DiffContent;
import com.intellij.openapi.diff.DiffManager;
import com.intellij.openapi.diff.DiffRequest;
import com.intellij.openapi.diff.SimpleContent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.ui.PopupHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Base class for editors which displays bytecode or ASMified code.
 */
public class ACodeView extends SimpleToolWindowPanel implements Disposable {
    private static final String DIFF_WINDOW_TITLE = "Show differences from previous class contents";
    private static final String[] DIFF_TITLES = {"Previous version", "Current version"};
    protected final Project project;

    protected final ToolWindowManager toolWindowManager;
    protected final KeymapManager keymapManager;
    private final String extension;


    protected Editor editor;
    protected Document document;
    // used for diff view
    private String previousCode;
    private VirtualFile previousFile;

    public ACodeView(final ToolWindowManager toolWindowManager, KeymapManager keymapManager, final Project project, final String fileExtension) {
        super(true, true);
        this.toolWindowManager = toolWindowManager;
        this.keymapManager = keymapManager;
        this.project = project;
        this.extension = fileExtension;
        setupUI();
    }

    public ACodeView(final ToolWindowManager toolWindowManager, KeymapManager keymapManager, final Project project) {
        this(toolWindowManager, keymapManager, project, "java");
    }

    private void setupUI() {
        final EditorFactory editorFactory = EditorFactory.getInstance();
        document = editorFactory.createDocument("");
        editor = editorFactory.createEditor(document, project, FileTypeManager.getInstance().getFileTypeByExtension(extension), true);

        final JComponent editorComponent = editor.getComponent();
        add(editorComponent);
        final AnAction diffAction = createShowDiffAction();
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(diffAction);
        final ActionManager actionManager = ActionManager.getInstance();
        final ActionToolbar actionToolBar = actionManager.createActionToolbar("ASM", group, true);
        final JPanel buttonsPanel = new JPanel(new BorderLayout());
        buttonsPanel.add(actionToolBar.getComponent(), BorderLayout.CENTER);
        PopupHandler.installPopupHandler(editor.getContentComponent(), group, "ASM", actionManager);
        setToolbar(buttonsPanel);
    }

    public void setCode(final VirtualFile file, final String code) {
        final String text = document.getText();
        if (previousFile == null || file == null || previousFile.getPath().equals(file.getPath()) && !Constants.NO_CLASS_FOUND.equals(text)) {
            if (file != null) previousCode = text;
        } else if (!previousFile.getPath().equals(file.getPath())) {
            previousCode = ""; // reset previous code
        }
        document.setText(code);
        if (file != null) previousFile = file;
    }



    public void dispose() {
        if (editor != null) {
            final EditorFactory editorFactory = EditorFactory.getInstance();
            editorFactory.releaseEditor(editor);
            editor = null;
        }
    }

    private AnAction createShowDiffAction() {
        return new ShowDiffAction();
    }

    private class ShowDiffAction extends AnAction {

        public ShowDiffAction() {
            super("Show differences",
                    "Shows differences from the previous version of bytecode for this file",
                    IconLoader.getIcon("/actions/diffWithCurrent.png"));
        }

        @Override
        public void update(final AnActionEvent e) {
            e.getPresentation().setEnabled(!"".equals(previousCode) && (previousFile!=null));
        }

        @Override
        public boolean displayTextInToolbar() {
            return true;
        }

        @Override
        public void actionPerformed(final AnActionEvent e) {
            DiffManager.getInstance().getDiffTool().show(new DiffRequest(project) {
                @Override
                public DiffContent[] getContents() {
                    // there must be a simpler way to obtain the file type
                    PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText("asm." + extension, "");
                    final DiffContent currentContent = previousFile == null ? new SimpleContent("") : new SimpleContent(document.getText(), psiFile.getFileType());
                    final DiffContent oldContent = new SimpleContent(previousCode == null ? "" : previousCode, psiFile.getFileType());
                    return new DiffContent[]{
                            oldContent,
                            currentContent
                    };
                }

                @Override
                public String[] getContentTitles() {
                    return DIFF_TITLES;
                }

                @Override
                public String getWindowTitle() {
                    return DIFF_WINDOW_TITLE;
                }
            });
        }
    }
}
