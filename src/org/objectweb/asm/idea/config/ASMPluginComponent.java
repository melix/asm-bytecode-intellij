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

package org.objectweb.asm.idea.config;
/**
 * Created by IntelliJ IDEA.
 * User: cedric
 * Date: 18/01/11
 * Time: 19:51
 */

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import org.jdom.Element;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * A component created just to be able to configure the plugin.
 */
@State(
        name = ASMPluginConfiguration.COMPONENT_NAME,
        storages = {
                @Storage(id = "other", file = "$PROJECT_FILE$")
        }
)
public class ASMPluginComponent implements ProjectComponent, Configurable, PersistentStateComponent<Element> {

    private Project project;
    private boolean skipFrames = false;
    private boolean skipDebug = false;
    private boolean skipCode = false;
    private boolean expandFrames;
    private GroovyCodeStyle codeStyle = GroovyCodeStyle.LEGACY;

    private ASMPluginConfiguration configDialog;

    public ASMPluginComponent(final Project project) {
        this.project = project;
    }

    public void projectOpened() {
    }

    public void projectClosed() {
    }

    @NotNull
    public String getComponentName() {
        return "ASM Plugin";
    }

    public void initComponent() {
    }

    public void disposeComponent() {
    }

    public boolean isSkipCode() {
        return skipCode;
    }

    public void setSkipCode(final boolean skipCode) {
        this.skipCode = skipCode;
    }

    public boolean isSkipDebug() {
        return skipDebug;
    }

    public void setSkipDebug(final boolean skipDebug) {
        this.skipDebug = skipDebug;
    }

    public boolean isSkipFrames() {
        return skipFrames;
    }

    public void setSkipFrames(final boolean skipFrames) {
        this.skipFrames = skipFrames;
    }

    public GroovyCodeStyle getCodeStyle() {
        return codeStyle;
    }

    public void setCodeStyle(final GroovyCodeStyle codeStyle) {
        this.codeStyle = codeStyle;
    }

    // -------------- Configurable interface implementation --------------------------

    @Nls
    public String getDisplayName() {
        return "ASM Bytecode plugin";
    }

    public Icon getIcon() {
        return IconLoader.getIcon("/images/asm.gif");
    }

    public String getHelpTopic() {
        return null;
    }

    public JComponent createComponent() {
        if (configDialog==null) configDialog = new ASMPluginConfiguration();
        return configDialog.getRootPane();
    }

    public boolean isModified() {
        return configDialog!=null && configDialog.isModified(this);
    }

    public void apply() throws ConfigurationException {
        if (configDialog!=null) {
            configDialog.getData(this);
        }
    }

    public void reset() {
        if (configDialog!=null) {
            configDialog.setData(this);
        }
    }

    public void disposeUIResources() {
        configDialog = null;
    }

    public boolean isExpandFrames() {
        return expandFrames;
    }

    public void setExpandFrames(final boolean expandFrames) {
        this.expandFrames = expandFrames;
    }

    // -------------------- state persistence

    public Element getState() {
        Element root = new Element("state");
        Element asmNode = new Element("asm");
        asmNode.setAttribute("skipDebug", String.valueOf(skipDebug));
        asmNode.setAttribute("skipFrames", String.valueOf(skipFrames));
        asmNode.setAttribute("skipCode", String.valueOf(skipCode));
        asmNode.setAttribute("expandFrames", String.valueOf(expandFrames));
        root.addContent(asmNode);
        Element groovyNode = new Element("groovy");
        groovyNode.setAttribute("codeStyle", codeStyle.toString());
        root.addContent(groovyNode);
        return root;
    }

    public void loadState(final Element state) {
        Element asmNode = state.getChild("asm");
        if (asmNode!=null) {
            final String skipDebugStr = asmNode.getAttributeValue("skipDebug");
            if (skipDebugStr!=null) skipDebug= Boolean.valueOf(skipDebugStr);
            final String skipFramesStr = asmNode.getAttributeValue("skipFrames");
            if (skipFramesStr!=null) skipFrames= Boolean.valueOf(skipFramesStr);
            final String skipCodeStr = asmNode.getAttributeValue("skipCode");
            if (skipCodeStr!=null) skipCode = Boolean.valueOf(skipCodeStr);
            final String expandFramesStr = asmNode.getAttributeValue("expandFrames");
            if (expandFramesStr!=null) expandFrames = Boolean.valueOf(expandFramesStr);
        }
        Element groovyNode = state.getChild("groovy");
        if (groovyNode!=null) {
            String codeStyleStr = groovyNode.getAttributeValue("codeStyle");
            if (codeStyleStr!=null) codeStyle = GroovyCodeStyle.valueOf(codeStyleStr);
        }
    }

}


