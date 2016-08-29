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

package io.github.victoryacovlev.erlyide.fxui.editor;

import io.github.victoryacovlev.erlyide.erlangtools.*;
import io.github.victoryacovlev.erlyide.fxui.mainwindow.FontSizeAjuctable;
import io.github.victoryacovlev.erlyide.project.ProjectFile;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;
import javafx.stage.Popup;
import org.fxmisc.richtext.*;
import org.fxmisc.richtext.model.RichTextChange;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.io.*;
import java.time.Duration;
import java.util.*;

public class ErlangCodeArea extends CodeArea implements FontSizeAjuctable {

    private final ErlangCompiler erlScan;
    private List<ErlErrorInfo> errors = new LinkedList<>();
    private int mainFontSize = 12;
    private int presentationModeFontSize = 16;
    private boolean presentationMode = false;

    public String getModuleName() {
        return moduleName;
    }

    private final ProjectFile projectFile;
    private String moduleName;

    private Popup errorOrWarningPopup;
    private Label errorOrWarningPopupLabel;

    private List<EventHandler<ModuleNameChangeEvent>> moduleNameChangeEventListeners = new LinkedList<>();

    // Workaround on RichTextFX 0.7-M2 bug causing to
    // MouseOverTextEvent.MOUSE_OVER_TEXT_END occurs immediately
    // when text scrolled
    Point2D mouseTextOverBegin = null;
    double mouseMovePosX = 0.0;
    double mouseMovePosY = 0.0;



    public ErlangCodeArea(ProjectFile file, ErlangCompiler compiler) {
        super("");
        this.projectFile = file;
        try {
            InputStream initialInputStream = new FileInputStream(this.projectFile.getFile());
            Scanner s = new Scanner(initialInputStream);
            s.useDelimiter("\\A");
            String initialText = s.next();
            initialInputStream.close();
            insertText(0, initialText);
            getUndoManager().mark();
            moveTo(0);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        erlScan = compiler;
        setParagraphGraphicFactory(LineNumberFactory.get(this));
        richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
                .subscribe(this::handleTextChange);
        applyHighlightings();

        errorOrWarningPopup = new Popup();
        errorOrWarningPopupLabel = new Label();
        errorOrWarningPopupLabel.getStyleClass().add("message_popup");
        errorOrWarningPopup.getContent().add(errorOrWarningPopupLabel);

        ErlangCodeArea self = this;

        addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_BEGIN, e -> {
            final int chIdx = e.getCharacterIndex();
            final int lineNo = getLineNumberByCharIndex(chIdx);
            final int erlangLineNo = lineNo + 1;
            String messages = null;
            for (ErlErrorInfo erlErrorInfo : errors) {
                if (erlangLineNo==erlErrorInfo.line) {
                    if (null==messages)
                        messages = "";
                    else
                        messages+="\n";
                    String message = ErlErrorInfo.ERROR==erlErrorInfo.type ? "Error: " : "Warning: ";
                    message += erlErrorInfo.message;
                    messages += message;
                }
            }
            if (null!=messages) {
                Point2D pos = e.getScreenPosition();
                mouseTextOverBegin = pos;
                final double x = pos.getX();
                final double yOffset = getBaselineOffset();
                final double y = pos.getY();
                errorOrWarningPopupLabel.setText(messages);
                errorOrWarningPopup.show(self.getParent().getParent(), x, y + yOffset);
//                System.out.println("Show popup: " + e.getScreenPosition().toString());
            }
            else {
                errorOrWarningPopup.hide();
            }
        });

        setOnMouseMoved(event -> {
            if (event.getEventType() == MouseEvent.MOUSE_MOVED) {
                mouseMovePosX = event.getScreenX();
                mouseMovePosY = event.getScreenY();
            }
        });

        addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_END, e -> {
            // Workaround on bug in RichTextFX
            final double Thereshold = 1.0;
            if (mouseTextOverBegin!=null) {
                double distanceX = Math.abs(mouseTextOverBegin.getX()-mouseMovePosX);
                double distanceY = Math.abs(mouseTextOverBegin.getY()-mouseMovePosY);
//                System.out.println("Distance: "+Double.toString(distanceX)+","+Double.toString(distanceY));
//                System.out.println("Mouse pos: " + mouseMovePos.toString());
                if (distanceX >= Thereshold || distanceY >= Thereshold) {
//                    System.out.println("Hide popup - mouse leave");
                    errorOrWarningPopup.hide();
                }
            }
        });

        setMouseOverTextDelay(Duration.ofMillis(50));

        addEventHandler(ProjectBuildFinishedEvent.PROJECT_BUILD_FINISHED, this::handleProjectBuildFinished);
    }

    private void handleProjectBuildFinished(ProjectBuildFinishedEvent event) {
        synchronized (errors) {
            errors.clear();
            for (ErlErrorInfo erlErrorInfo : event.getResult().getErrorsAndWarnings()) {
                if (erlErrorInfo.moduleFileName.equals(projectFile.getFile().getAbsolutePath())) {
                    errors.add(erlErrorInfo);
                }
            }
        }
        Platform.runLater(this::applyHighlightings);
        event.consume();
    }

    private int getLineNumberByCharIndex(int chIdx) {
        final String text = getText();
        int result = 0;
        for (int i=0; i<Integer.min(chIdx, text.length()); ++i) {
            final char ch = text.charAt(i);
            if ('\n'==ch)
                result++;
        }
        return result;
    }

    private void applyHighlightings() {
        final String text = getText();
        StyleSpans<Collection<String>> highlights = computeHighlighting(text);
        setStyleSpans(0, highlights);
    }

    private void handleTextChange(RichTextChange<Collection<String>,Collection<String>> change) {
        applyHighlightings();
        if (errorOrWarningPopup!=null && errorOrWarningPopup.isShowing())
            errorOrWarningPopup.hide();
        TextEditedEvent event = new TextEditedEvent(this, null);
        fireEvent(event);

    }

    private StyleSpans<Collection<String>> computeHighlighting(String text) {
        final String lines[] = text.split("\n");
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        int lineStartPos = 0;
        for (int lineNo=0; lineNo<lines.length; ++lineNo) {
            final String lineText = lines[lineNo];
            int itemEnd = lineStartPos;
            if (lineText.length() > 0) {
                SplitResult scanResult = erlScan.splitLineIntoLexems(lineText, lineNo+1);
                extractModuleName(scanResult.tokens, lineText);
                for (ErlToken token : scanResult.tokens) {
                    final int start = lineStartPos + token.column - 1;
                    int length = token.text.length();
                    String styleClass = ErlToken.Category.WHITE_SPACE==token.type ? null :
                            token.type.toString().toLowerCase();
                    if (ErlToken.Category.STRING==token.type ||
                            ErlToken.Category.ATOM==token.type && lineText.charAt(token.column-1)=='\'' ||
                            ErlToken.Category.FUNCTION==token.type && lineText.charAt(token.column-1)=='\''
                            ) {
                        length += 2;
                    }
                    spansBuilder.add(Collections.emptyList(), start-itemEnd);
                    Set<String> tokenSpans = null;
                    final String errorClass = errorClassAtLine(lineNo+1);
                    if (errorClass==null) {
                        tokenSpans = Collections.singleton(styleClass);
                    }
                    else {
                        Set<String> tokenClasses = new HashSet<>(Arrays.asList(styleClass, errorClass));
                        tokenSpans = tokenClasses; // TODO optimize me!
                    }
                    spansBuilder.add(tokenSpans, length);
                    itemEnd = start + length;
                }
            }
            int remainingLength = lineStartPos + lineText.length() - itemEnd;
            spansBuilder.add(Collections.emptyList(), remainingLength);
            lineStartPos += lineText.length();
            if (text.length() > lineStartPos && text.charAt(lineStartPos)=='\n')
                spansBuilder.add(Collections.singleton("nl"), 1);
            lineStartPos++;
        }
        return spansBuilder.create();
    }

    private String errorClassAtLine(int lineNo) {
        String result = null;
        synchronized (errors) {
            for (ErlErrorInfo errorInfo : errors) {
                if (errorInfo.line == lineNo) {
                    if (result == null || !result.equals("compiler_error")) {
                        switch (errorInfo.type) {
                            case ErlErrorInfo.ERROR:
                                result = "compiler_error";
                                break;
                            case ErlErrorInfo.WARNNING:
                                result = "compiler_warning";
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        }
        return result;
    }


    public void saveFileIfChanged() {
        if (!getUndoManager().isAtMarkedPosition()) {
            saveFile();
        }
    }

    public void saveFile() {
        final String text = getText();
        if (moduleName==null) {
            computeHighlighting(text);
        }
        final String dir = projectFile.getFile().getParent();
        final String name = projectFile.getFile().getName();
        final String correctName = moduleName + ".erl";
        final String correctPath = dir + "/" + correctName;
//        File newFile = new File(correctPath);
        if (name != correctName) {
            projectFile.setName(correctPath);
//            if (projectFile.getFile().exists())
//                projectFile.getFile().renameTo(newFile);
        }
        try {
            FileOutputStream fs = new FileOutputStream(projectFile.getFile());
            OutputStreamWriter writer = new OutputStreamWriter(fs, "UTF-8");
            BufferedWriter bufferedWriter = new BufferedWriter(writer);
            bufferedWriter.write(text);
            bufferedWriter.close();
            getUndoManager().mark();
            FileSavedEvent event = new FileSavedEvent(this, null);
            fireEvent(event);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String extractModuleName(final List<ErlToken> tokenList, final String line) {
        String result = null;
        for (ErlToken token : tokenList) {
            if (ErlToken.Category.MODULE==token.type) {
                final String textBefore = line.substring(0, token.column-1).trim();
                if (textBefore.equals("-module(")) {
                    result = token.text;
                    break;
                }
            }
        }
        if (result!=null) {
            setModuleName(result);
        }
        return result;
    }

    public void setModuleName(String moduleName) {
        if (null!=moduleName && ErlangCompiler.isValidModuleName(moduleName)) {
            if (this.moduleName==null || !this.moduleName.equals(moduleName)) {
                this.moduleName = moduleName;
                ModuleNameChangeEvent event = new ModuleNameChangeEvent(this, null, moduleName);
                fireEvent(event);
            }
        }
    }

    public ProjectFile getProjectFile() {
        return projectFile;
    }


    @Override
    public void setMainFontSize(int fontSize) {
        this.mainFontSize = fontSize;
        updateFont();
    }

    @Override
    public void setPresentationModeFontSize(int fontSize) {
        this.presentationModeFontSize = fontSize;
        updateFont();
    }

    @Override
    public void setPresentationMode(boolean presentationMode) {
        this.presentationMode = presentationMode;
        updateFont();
    }

    private void updateFont() {
        int size = presentationMode ? presentationModeFontSize : mainFontSize;
        setStyle("-fx-font-size: " + Integer.toString(size) + "; -fx-font-family: monospace;");
    }

}
