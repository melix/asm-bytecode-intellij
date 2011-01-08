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
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;

import javax.swing.*;
import java.awt.*;

/**
 * Base class for editors which displays bytecode or ASMified code.
 */
public class ACodeView extends JPanel implements Disposable {
	protected final Project project;
	protected final ToolWindowManager toolWindowManager;
	protected final KeymapManager keymapManager;

	private final String extension;
	protected Editor editor;
	protected Document document;


	public ACodeView(final ToolWindowManager toolWindowManager, KeymapManager keymapManager, final Project project, final String fileExtension) {
		super(new BorderLayout());
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

		add(editor.getComponent());
	}

	public void setCode(final String code) {
		document.setText(code);
	}


	public void dispose() {
		if (editor != null) {
			final EditorFactory editorFactory = EditorFactory.getInstance();
			editorFactory.releaseEditor(editor);
			editor = null;
		}
	}
}
