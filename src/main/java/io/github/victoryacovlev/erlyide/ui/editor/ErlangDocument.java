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

package io.github.victoryacovlev.erlyide.ui.editor;

import com.ericsson.otp.erlang.*;
import io.github.victoryacovlev.erlyide.ErlangVM;
import io.github.victoryacovlev.erlyide.erlangtools.ErlErrorInfo;
import io.github.victoryacovlev.erlyide.erlangtools.ErlangCompiler;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.Token;

import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import java.io.*;
import java.util.LinkedList;
import java.util.List;

public class ErlangDocument extends RSyntaxDocument {

    String moduleName = null;
    String fileName = null;
    List<ModuleNameChangeListener> moduleNameChangeListeners = new LinkedList<>();
    List<ChangeListener> marksChangedListeners = new LinkedList<>();
    List<ErlangLineMark> lineMarks = new LinkedList<>();

    final ErlangCompiler compiler;
    final Parser parser;
    List<ErlErrorInfo> errorsAndWarnings = null;
    private boolean modifiedFromLastSave = false;

    public List<ErlErrorInfo> getErrorsAndWarnings() {
        return errorsAndWarnings;
    }

    public ErlangDocument(final String moduleName, final String fileName, final ErlangCompiler compiler, final Parser parser) {
        super("");
        this.moduleName = moduleName;
        this.fileName = fileName;
        this.compiler = compiler;
        this.parser = parser;
        addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                detectCurrentModuleName(e);
                checkForLinesChanged(e);
                modifiedFromLastSave = true;
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                detectCurrentModuleName(e);
                checkForLinesChanged(e);
                modifiedFromLastSave = true;
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                detectCurrentModuleName(e);
                checkForLinesChanged(e);
            }
        });
    }

    private void checkForLinesChanged(DocumentEvent e) {
        if (e.getType()!=DocumentEvent.EventType.CHANGE && errorsAndWarnings!=null) {
            final int offset = e.getOffset();
            try {
                final String text = getText(0, getLength());
                final String textBefore = text.substring(0, offset);
                final String textAfter = text.substring(offset);
                final int changeLineNumber = lineEndsCount(textBefore) + 1;
                // TODO rewrite this part in more effective way using iterators
                LinkedList<ErlErrorInfo> newErrors = new LinkedList<>();
                boolean sometingChanged = false;
                for (ErlErrorInfo info : errorsAndWarnings) {
                    if (info.line == changeLineNumber) {
                        // drop it
                        sometingChanged = true;
                    }
                    else if (info.line < changeLineNumber) {
                        // keep it
                        newErrors.add(info);
                    }
                    else if (info.line > changeLineNumber) {
                        // calculate new line position
                        int change = 0;
                        final String changeText = getText(e.getOffset(), e.getLength());
                        final int newLinesInChange = lineEndsCount(changeText);
                        if (e.getType() == DocumentEvent.EventType.INSERT)
                            change = newLinesInChange;
                        else
                            change = -newLinesInChange;
                        info.line += change;
                        newErrors.add(info);
                        sometingChanged = true;
                    }
                }
                if (sometingChanged) {

                }
                errorsAndWarnings = newErrors;
            } catch (BadLocationException e1) {
                e1.printStackTrace();
            }
        }
    }

    private static int lineEndsCount(final CharSequence text) {
        int result = 0;
        for (int i=0; i<text.length(); ++i) {
            final char ch = text.charAt(i);
            if ('\n' == ch) result++;
        }
        return result;
    }

    private void detectCurrentModuleName(DocumentEvent e) {
        if (DocumentEvent.EventType.CHANGE == e.getType()) {
            final int firstLine = e.getOffset();
            final int line = e.getLength();
            String newModuleName = null;
            for (int i=firstLine; i<=line; ++i) {
                Token firstLineToken = getTokenListForLine(i);
                int match = 0;
                while (firstLineToken!=null && newModuleName==null) {
                    if (!firstLineToken.isCommentOrWhitespace() && firstLineToken.getLexeme()!=null) {
                        if (0 == match && firstLineToken.getLexeme().equals("-")) {
                            match = 1;
                        }
                        else if (1 == match && firstLineToken.getLexeme().equals("module")) {
                            match = 2;
                        }
                        else if (2 == match && firstLineToken.getLexeme().equals("(")) {
                            match = 3;
                        }
                        else if (3 == match && Token.RESERVED_WORD_2==firstLineToken.getType()) {
                            newModuleName = firstLineToken.getLexeme();
                            break;
                        }
                        else {
                            break;
                        }
                    }
                    firstLineToken = firstLineToken.getNextToken();
                }
                if (newModuleName!=null)
                    break;
            }

            if (newModuleName!=null) {
                newModuleName = newModuleName.trim();
                if (newModuleName.length() >= 1 && !newModuleName.equalsIgnoreCase(this.moduleName)) {
                    this.moduleName = newModuleName;
                    for (ModuleNameChangeListener listener : moduleNameChangeListeners) {
//                        listener.moduleNameChanged(new ModuleNameChangeEvent(this, newModuleName));
                    }
                }
            }
        }
    }

    public void addModuleNameChangeListener(ModuleNameChangeListener listener) {
        moduleNameChangeListeners.add(listener);
    }

    public void addMarksChangedListener(ChangeListener listener) {
        marksChangedListeners.add(listener);
    }

    public void save() {
        File oldFile = new File(fileName);
        final String dir = oldFile.getParent();
        final String name = oldFile.getName();
        final String correctName = moduleName + ".erl";
        final String correctPath = dir + "/" + correctName;
        File newFile = new File(correctPath);
        if (name != correctName) {
            if (oldFile.exists())
                oldFile.renameTo(newFile);
            fileName = correctPath;
        }
        try {
            FileOutputStream fs = new FileOutputStream(newFile);
            OutputStreamWriter writer = new OutputStreamWriter(fs, "UTF-8");
            BufferedWriter bufferedWriter = new BufferedWriter(writer);
            final String programText = getText(0, getLength());
            bufferedWriter.write(programText);
            bufferedWriter.close();
            modifiedFromLastSave = false;

        } catch (BadLocationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseErrorsAndWarnings(OtpErlangList modulesList, int type) {
        for (OtpErlangObject moduleObject : modulesList) {
            OtpErlangTuple moduleTuple = (OtpErlangTuple) moduleObject;
            final String moduleFileName = ((OtpErlangString) moduleTuple.elementAt(0)).stringValue();
            if (moduleFileName.equals(fileName)) {
                OtpErlangList itemsList = (OtpErlangList) moduleTuple.elementAt(1);
                for (OtpErlangObject itemObj : itemsList) {
                    OtpErlangTuple itemTuple = (OtpErlangTuple) itemObj;
                    try {
                        final int lineNumber = ((OtpErlangLong) itemTuple.elementAt(0)).intValue();
                        final String moduleName = ((OtpErlangAtom) itemTuple.elementAt(1)).atomValue();
                        final OtpErlangObject errorDescriptor = itemTuple.elementAt(2);
                        ErlangVM vm = ErlangVM.getInstance();
                        OtpErlangTuple vmResult = (OtpErlangTuple) vm.run(moduleName, "format_error", new OtpErlangList(errorDescriptor));
                        OtpErlangList messageList = (OtpErlangList) vmResult.elementAt(1);
                        OtpErlangTuple vmResult2 = (OtpErlangTuple) vm.run("lists", "flatten", new OtpErlangList(new OtpErlangObject[]{messageList}));
                        final String message = ((OtpErlangString)vmResult2.elementAt(1)).stringValue();

                    } catch (OtpErlangRangeException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (OtpAuthException e) {
                        e.printStackTrace();
                    } catch (OtpErlangDecodeException e) {
                        e.printStackTrace();
                    } catch (OtpErlangExit otpErlangExit) {
                        otpErlangExit.printStackTrace();
                    }
                }
            }
        }
    }

    public boolean isModifiedFromLastSave() {
        return modifiedFromLastSave;
    }

    public String getFileName() {
        return fileName;
    }
}
