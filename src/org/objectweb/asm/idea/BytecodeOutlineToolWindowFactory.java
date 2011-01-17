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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.ContentFactory;

/**
 * ASM ToolWindow factory
 */
public class BytecodeOutlineToolWindowFactory implements ToolWindowFactory{
	public void createToolWindowContent(final Project project, final ToolWindow toolWindow) {
		BytecodeOutline outline = BytecodeOutline.getInstance(project);
		BytecodeASMified asmified = BytecodeASMified.getInstance(project);
        GroovifiedView groovified = GroovifiedView.getInstance(project);
		toolWindow.getContentManager().addContent(ContentFactory.SERVICE.getInstance().createContent(outline, "Bytecode", false));
		toolWindow.getContentManager().addContent(ContentFactory.SERVICE.getInstance().createContent(asmified, "ASMified", false));
		toolWindow.getContentManager().addContent(ContentFactory.SERVICE.getInstance().createContent(groovified, "Groovified", false));
	}
}
