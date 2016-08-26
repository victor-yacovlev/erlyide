/*
 * Copyright 2016 Victor Yacovlev <v.yacovlev@gmail.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.github.victoryacovlev.erlide.ui.editor;

import com.ericsson.otp.erlang.OtpAuthException;
import com.github.victoryacovlev.erlide.ErlangVM;
import com.github.victoryacovlev.erlide.erlangtools.ErlangCompiler;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.io.IOException;
import java.io.InputStream;

public class ErlangSyntaxTextArea extends RSyntaxTextArea implements ChangeListener {

    final private ErlangVM vm;
    final private Parser parser;
    final private ErlangCompiler compiler;
    final private ErlangDocument document;

    public ErlangSyntaxTextArea(final String moduleName, final String fileName, final String initialText) {
        super();
        ErlangVM erlangVm = null;
        try {
            erlangVm = ErlangVM.getInstance();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (OtpAuthException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        vm = erlangVm;
        compiler = new ErlangCompiler(vm);
        parser = new Parser(compiler);
        addParser(parser);

        document = new ErlangDocument(moduleName, fileName, compiler, parser);

        setDocument(document);

        ((ErlangDocument) getDocument()).addMarksChangedListener(this);
        final String themeName = "default";
        final String themeFileName = "/editor_themes/"+themeName+".xml";
        InputStream themeInputStream = getClass().getResourceAsStream(themeFileName);
        try {
            Theme theme = Theme.load(themeInputStream);
            theme.apply(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        setText(initialText);
        setAutoIndentEnabled(true);
        setAntiAliasingEnabled(true);
        setBracketMatchingEnabled(true);
        setTabsEmulated(true);
        setTabSize(4);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == getDocument()) {
            ErlangDocument doc = (ErlangDocument) getDocument();
        }
    }

    public void forceReparsing() {
        forceReparsing(parser);
    }
}
