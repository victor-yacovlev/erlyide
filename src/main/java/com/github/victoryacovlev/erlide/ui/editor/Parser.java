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

import com.github.victoryacovlev.erlide.erlangtools.ErlErrorInfo;
import com.github.victoryacovlev.erlide.erlangtools.ErlangCompiler;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.parser.AbstractParser;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice;

import javax.swing.text.BadLocationException;
import java.util.LinkedList;
import java.util.List;

public class Parser extends AbstractParser {

    public class ErlangParseResult implements ParseResult {

        final Parser parser;
        final ErlangDocument document;
        final List<ErlErrorInfo> errorsAndWarnings;

        public ErlangParseResult(Parser parser, ErlangDocument document, List<ErlErrorInfo> errorsAndWarnings) {
            this.parser = parser;
            this.document = document;
            this.errorsAndWarnings = errorsAndWarnings;
        }

        @Override
        public Exception getError() {
            String firstErrorMessage = null;
            for (ErlErrorInfo errorInfo : errorsAndWarnings) {
                if (ErlErrorInfo.ERROR == errorInfo.type) {
                    firstErrorMessage = errorInfo.message;
                    break;
                }
            }
            if (firstErrorMessage!=null)
                return new Exception(firstErrorMessage);
            else return null;
        }

        @Override
        public int getFirstLineParsed() {
            return 0;
        }

        @Override
        public int getLastLineParsed() {
            try {
                final String text = document.getText(0, document.getLength());
                final String[] lines = text.split("\n");
                return lines.length - 1;
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
            return 0;
        }

        @Override
        public List<ParserNotice> getNotices() {
            if (errorsAndWarnings.isEmpty()) {
                return new LinkedList<>();
            }
            else {
                List<ParserNotice> result = new LinkedList<>();
                for (ErlErrorInfo errorInfo : errorsAndWarnings) {
                    result.add(new DefaultParserNotice(parser, errorInfo.message, errorInfo.line-1) {
                        @Override
                        public Level getLevel() {
                            switch (errorInfo.type) {
                                case ErlErrorInfo.WARNNING:
                                    return Level.WARNING;
                                case ErlErrorInfo.NO_ERROR:
                                    return Level.INFO;
                                case ErlErrorInfo.ERROR:
                                default:
                                    return Level.ERROR;
                            }
                        }
                    });
                }
                return result;
            }
        }

        @Override
        public org.fife.ui.rsyntaxtextarea.parser.Parser getParser() {
            return parser;
        }

        @Override
        public long getParseTime() {
            return 0;
        }
    }

    public Parser(ErlangCompiler compiler) {
        this.compiler = compiler;
    }

    private final ErlangCompiler compiler;

    @Override
    public ParseResult parse(RSyntaxDocument doc, String style) {
        System.out.println("Parse!");
        ErlangDocument erlangDocument = (ErlangDocument) doc;
        ErlangParseResult result = null;
        List<ErlErrorInfo> errorsAndWarnings = erlangDocument.getErrorsAndWarnings();
        if (errorsAndWarnings!=null) {
            result = new ErlangParseResult(this, erlangDocument, errorsAndWarnings);
        }
        return result;
    }

}
