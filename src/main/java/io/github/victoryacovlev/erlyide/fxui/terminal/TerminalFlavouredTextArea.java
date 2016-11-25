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

package io.github.victoryacovlev.erlyide.fxui.terminal;

import io.github.victoryacovlev.erlyide.fxui.mainwindow.FontSizeAjuctable;
import javafx.event.Event;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;

import java.util.*;

public class TerminalFlavouredTextArea extends TextArea implements FontSizeAjuctable {
    private CommandProcessor commandProcessor = null;
    private int allowEditCaretPosition = 0;
    private LinkedList<HistoryElement> history = new LinkedList<>();
    private int currentHistoryIndex = -1;
    private HistoryElement unsavedHistoryElement = new HistoryElement();

    private int mainFontSize = 12;
    private int presentationModeFontSize = 16;
    private boolean presentationMode = false;

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

    private final class HistoryElement {
        String command;
        int offset;
    }

    public TerminalFlavouredTextArea() {
        addEventFilter(KeyEvent.KEY_TYPED, this::handleKeyTyped);
        addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
        MenuItem actionCopy = new MenuItem("Copy");
        actionCopy.setOnAction(event -> copy());
        ContextMenu contextMenu = new ContextMenu(actionCopy);
        contextMenu.setOnShown(event -> {
            IndexRange sel = getSelection();
            actionCopy.setDisable(sel.getLength()==0);
        });
        setContextMenu(contextMenu);
    }

    private void handleKeyPressed(KeyEvent event) {
        if (event.getCode()==KeyCode.UP) {
            if (currentHistoryIndex < history.size()-1) {
                saveHistoryElement();
                currentHistoryIndex ++;
                loadHistoryElement();
            }
            event.consume();
        }
        if (event.getCode()==KeyCode.DOWN) {
            if (currentHistoryIndex > -1) {
                saveHistoryElement();
                currentHistoryIndex --;
                loadHistoryElement();
            }
            event.consume();
        }
        if (event.getCode()==KeyCode.LEFT || event.getCode()==KeyCode.BACK_SPACE) {
            if (getCaretPosition() <= allowEditCaretPosition) {
                event.consume();
            }
        }
        if (event.getCode()==KeyCode.DELETE) {
            if (getCaretPosition() <= allowEditCaretPosition) {
                event.consume();
            }
        }
        if (event.getCode()==KeyCode.BACK_SPACE || event.getCode()==KeyCode.DELETE) {
            if (getSelection().getStart() < allowEditCaretPosition) {
                event.consume();
            }
        }
        if (event.getCode()==KeyCode.HOME && !event.isShiftDown()) {
            positionCaret(allowEditCaretPosition);
            event.consume();
        }
        if (event.getCode()==KeyCode.HOME && event.isShiftDown()) {
            selectPositionCaret(allowEditCaretPosition);
            event.consume();
        }
        if (event.isShortcutDown()) {
            if (event.getCode()==KeyCode.V) {
                // Can paste only at end
                positionCaret(getLength());
            }
            else if (event.getCode()!=KeyCode.C) {
                List<KeyCode> allowedCodes = Arrays.asList(KeyCode.C, KeyCode.N, KeyCode.S, KeyCode.W, KeyCode.O);
                KeyCode code = event.getCode();
                if (!allowedCodes.contains(code)) {
                    event.consume();
                }
            }
        }
    }

    private void loadHistoryElement() {
        HistoryElement hist = currentHistoryIndex >=0 ? history.get(currentHistoryIndex) : unsavedHistoryElement;
        deleteText(allowEditCaretPosition, getLength());
        appendText(hist.command);
        positionCaret(allowEditCaretPosition + hist.offset);
    }

    private void saveHistoryElement() {
        HistoryElement hist = currentHistoryIndex >=0 ? history.get(currentHistoryIndex) : unsavedHistoryElement;
        hist.command = getCurrentCommand();
        hist.offset = getText().length() - allowEditCaretPosition;
    }

    private String getCurrentCommand() {
        String text = getText();
        String command = text.substring(allowEditCaretPosition);
        return command;
    }

    private void handleKeyTyped(KeyEvent event) {
        int caretPosition = getCaretPosition();
        if (getSelection().getStart() < allowEditCaretPosition) {
            deselect();
        }
        if (caretPosition < allowEditCaretPosition) {
            positionCaret(allowEditCaretPosition);
        }
        if (event.getCharacter().equals("\r")) {
            String command = getCurrentCommand();
            if (commandProcessor != null) {
                commandProcessor.processCommand(command);
                HistoryElement hist = new HistoryElement();
                hist.command = command.trim();
                hist.offset = hist.command.length();
                history.addFirst(hist);
                currentHistoryIndex = -1;
            }
        }
    }

    public void setCommandProcessor(CommandProcessor commandProcessor) {
        this.commandProcessor = commandProcessor;
        commandProcessor.install(this);
    }

    public CommandProcessor getCommandProcessor() {
        return commandProcessor;
    }

    public void appendTextFromCommandProcessor(String text) {
        appendText(text);
        allowEditCaretPosition = getText().length();

    }


}
