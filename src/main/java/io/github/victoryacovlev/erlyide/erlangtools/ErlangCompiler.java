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

package io.github.victoryacovlev.erlyide.erlangtools;

import com.ericsson.otp.erlang.*;
import io.github.victoryacovlev.erlyide.ErlangVM;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ErlangCompiler {
    private final ErlangVM vm;

    public ErlangCompiler(ErlangVM vm) {
        this.vm = vm;
    }

    private static ErlangCompiler instance = null;

    private static final List<String> KEYWORDS = new LinkedList<>(Arrays.asList(
            "after", "and", "andalso",
            "band", "begin", "bnot", "bor", "bsl", "bsr", "bxor",
            "case", "catch", "cond",
            "div",
            "end",
            "fun",
            "if",
            "let", "when",
            "not",
            "of", "or", "orelse",
            "receive", "rem",
            "try",
            "when",
            "xor"
    ));

    private static final List<String> OPERATORS_RX = new LinkedList<>(Arrays.asList(
            "<=", ">=", "=/=", "<<", ">>", "\"", "'", "->", "#", "::",
            "\\(", "\\)", "\\[", "\\]", "\\{", "\\}", "\\|\\|", "\\|", "\\.", ";", ":", ",", "%",
            "\\+", "-", "\\*", "/", "<", ">", "\\u003D", "\\u003f"
    ));

    private static final List<String> OPERATORS = new LinkedList<>(Arrays.asList(
            "<=", ">=", "=/=", "<<", ">>", "\"", "'", "->", "::",
            "(", ")", "[", "]", "{", "}", "||", "|", ".", ";", ":", ",", "%",
            "+", "-", "*", "/", "<", ">", "=", "?"
    ));

    private static final Pattern lexerPattern;

    static {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<KEYWORDS.size(); ++i) {
            sb.append("\\b"+KEYWORDS.get(i)+"\\b");
            if (i<KEYWORDS.size()-1)
                sb.append("|");
        }
        sb.append("|");
        for (int i = 0; i< OPERATORS_RX.size(); ++i) {
            sb.append(OPERATORS_RX.get(i));
            if (i<OPERATORS_RX.size()-1)
                sb.append("|");
        }
        sb.append("|\\s+");
        final String rxPattern = sb.toString();
        lexerPattern = Pattern.compile(rxPattern);
    }

    public static ErlangCompiler getInstance() {
        if (null == instance) {
            try {
                ErlangVM vm = ErlangVM.getInstance();
                instance = new ErlangCompiler(vm);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (OtpAuthException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return instance;
    }

    public SplitResult splitLineIntoLexems(final String text, int lineNo) {
        SplitResult result = splitIntoLexemsUsingErlangApi(text, lineNo);
//        SplitResult result = null;
        if (null==result) {
            result = splitLineIntoLexemsFallback(text, lineNo);
            result = specializeRegularTokens(result);
            result = removeQuotesToMatchErlangOuput(result);
            result = detectNumberForms(result);
        }
        return detectSpecialTypes(result, text);
    }

    private SplitResult detectNumberForms(SplitResult result) {
        LinkedList<ErlToken> newTokens = new LinkedList<>();
        for (int i=0; i<result.tokens.size(); ) {
            ErlToken token = result.tokens.get(i);
            if (i < result.tokens.size()-2 && ErlToken.Category.INTEGER == token.type) {
                ErlToken next = result.tokens.get(i+1);
                ErlToken nextNext = result.tokens.get(i+2);
                boolean nextIsNumberFormOperator = next.text.equals(".") || next.equals("#");
                boolean integerAfterOperator = nextNext.type == ErlToken.Category.INTEGER;
                if (nextIsNumberFormOperator && integerAfterOperator) {
                    token.text += next.text + nextNext.text;
                    if (next.text.equals("."))
                        token.type = ErlToken.Category.FLOAT;
                    i += 3;
                }
                else {
                    ++i;
                }
            }
            else {
                ++i;
            }
            newTokens.add(token);
        }
        return new SplitResult(newTokens, result.error);
    }

    private SplitResult removeQuotesToMatchErlangOuput(SplitResult result) {
        for (ErlToken token : result.tokens) {
            if (ErlToken.Category.ATOM == token.type || ErlToken.Category.STRING == token.type) {
                final String text = token.text;
                if (text.startsWith("\"") && text.endsWith("\"")) {
                    token.text = text.substring(1, text.length()-1);
                }
                else if (text.startsWith("'") && text.endsWith("'")) {
                    token.text = text.substring(1, text.length()-1);
                }
            }
        }
        return result;
    }

    private SplitResult specializeRegularTokens(SplitResult result) {
        for (ErlToken token : result.tokens) {
            if (ErlToken.Category.UNDEFINED == token.type && token.text.length() > 0) {
                final String text = token.text;
                if (isReservedWord(text)) {
                    token.type = ErlToken.Category.RESERVED_WORD;
                }
                else if (isOperator(text)) {
                    token.type = ErlToken.Category.OPERATOR;
                }
                else {
                    final char firstLetter = token.text.charAt(0);
                    final String Capitals = "_ABCDEFGHIJKLMNOPQRSTUVWXYZ";
                    final String Lowers = "abcdefghijklmnopqrstuvwxyz";
                    final String Digits = "0123456789";
                    if (Capitals.contains("" + firstLetter))
                        token.type = ErlToken.Category.VAR;
                    else if (Lowers.contains("" + firstLetter))
                        token.type = ErlToken.Category.ATOM;
                    else if (Digits.contains("" + firstLetter))
                        token.type = ErlToken.Category.INTEGER;
                }
            }
        }
        return result;
    }

    SplitResult splitIntoLexemsUsingErlangApi(final String text, int lineNo) {
        SplitResult result = null;
        try {
            OtpErlangTuple scanResult = (OtpErlangTuple)
                    vm.run("erl_scan", "string", new OtpErlangList(new OtpErlangObject[]{
                            new OtpErlangString(text),
                            new OtpErlangTuple(new OtpErlangObject[]{
                                    new OtpErlangInt(lineNo),
                                    new OtpErlangInt(1)
                            }),
//                            new OtpErlangInt(startLine),
                            new OtpErlangList(new OtpErlangObject[]{
                                    new OtpErlangAtom("return"),
                                    new OtpErlangAtom("return_white_spaces"),
                                    new OtpErlangAtom("return_comments")
                            })
                    }));
            final String status = ((OtpErlangAtom) scanResult.elementAt(0)).atomValue();
            if (status.equals("ok")) {
                List<ErlToken> tokens = new LinkedList<>();
                for (OtpErlangObject itemObject : (OtpErlangList) scanResult.elementAt(1)) {
                    OtpErlangTuple token = (OtpErlangTuple) itemObject;
                    String category = ((OtpErlangAtom)(token.elementAt(0))).atomValue();
                    OtpErlangObject location = token.elementAt(1);
                    int line = 0;
                    int col = 0;
                    if (location instanceof OtpErlangLong) {
                        col = ((OtpErlangLong) location).intValue();
                    }
                    else if (location instanceof OtpErlangTuple) {
                        OtpErlangTuple pair = (OtpErlangTuple) location;
                        line = ((OtpErlangLong) pair.elementAt(0)).intValue();
                        col = ((OtpErlangLong) pair.elementAt(1)).intValue();
                    }
                    String value = "";
                    if (token.arity() > 2 && category.length()!=1) {
                        OtpErlangObject valueObject = token.elementAt(2);
                        if (valueObject instanceof OtpErlangString) {
                            value = ((OtpErlangString) valueObject).stringValue();
                        }
                        else if (valueObject instanceof OtpErlangAtom) {
                            value = ((OtpErlangAtom) valueObject).atomValue();
                        }
                        else if (valueObject instanceof OtpErlangLong) {
                            value = Long.toString(((OtpErlangLong) valueObject).longValue());
                        }
                        else if (valueObject instanceof OtpErlangDouble) {
                            value = Double.toString(((OtpErlangDouble) valueObject).doubleValue());
                        }
                    }
                    else if (category.equals("dot")) {
                        value = ".";
                    }
                    else if (category.length() <= 3){
                        value = category; // in case of operator
                    }
                    if (category.equals("atom") || category.equals(value) || value.isEmpty()) {
                        OtpErlangAtom isReservedWord = (OtpErlangAtom) vm.run("erl_scan", "reserved_word", new OtpErlangList(new OtpErlangObject[]{
                                itemObject
                        }));
                        if (isReservedWord.booleanValue()) {
                            category = "reserved_word";
                        }
                    }
                    ErlToken erlToken = new ErlToken(category, line, col, value);
                    tokens.add(erlToken);
                }
                return new SplitResult(tokens, null);
            }
            else {

            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (OtpErlangExit otpErlangExit) {
            otpErlangExit.printStackTrace();
        } catch (OtpAuthException e) {
            e.printStackTrace();
        } catch (OtpErlangDecodeException e) {
            e.printStackTrace();
        } catch (OtpErlangRangeException e) {
            e.printStackTrace();
        }
        return result;
    }

    private SplitResult splitLineIntoLexemsFallback(final String line, int startLine) {

        ErlToken.Category mode = ErlToken.Category.UNDEFINED;

        if (0 == line.length())
            return new SplitResult(new LinkedList<>(), null);

        int cur = 0;
        int prev = -1;

        Matcher m = lexerPattern.matcher(line);

        LinkedList<ErlToken> tokens = new LinkedList<>();
        ErlErrorInfo error = null;

        for (;;) {
            if (m.find(Integer.max(0, prev))) {
                cur = m.start();
                if ( (cur-prev>1 && -1==prev) || (cur-prev>=0 && prev>=0) ) {
                    if (ErlToken.Category.ATOM==mode || ErlToken.Category.STRING==mode) {
                        // Append text to previous lexem
                        final String toAppend = line.substring(prev, cur+m.group().length());
                        tokens.getLast().text += toAppend;
                        tokens.getLast().type = mode;
                    }
                    else {
                        int start = Integer.max(prev, 0);
                        final String text = prev>0 ? line.substring(prev, cur) : line.substring(0, cur);
                        if (text.length() > 0) {
                            tokens.add(new ErlToken(mode, startLine, start + 1, text));
                        }
                    }
                }
                final String symb = m.group();
                if (ErlToken.Category.STRING==mode && symb.equals("\"")) {
                    mode = ErlToken.Category.UNDEFINED;
                }
                else if (ErlToken.Category.ATOM==mode && symb.equals("'")) {
                    mode = ErlToken.Category.UNDEFINED;
                }
                else if (ErlToken.Category.UNDEFINED==mode && symb.equals("%")) {
                    final String text = line.substring(cur);
                    tokens.add(new ErlToken(ErlToken.Category.COMMENT, startLine, cur+1, text));
                    break;
                }
                else if (ErlToken.Category.UNDEFINED==mode) {
                    tokens.add(new ErlToken(mode, startLine, cur+1, symb));
                    if (symb.equals("\""))
                        mode = ErlToken.Category.STRING;
                    else if (symb.equals("'"))
                        mode = ErlToken.Category.ATOM;
                }
                prev = cur + symb.length();
            }
            else {
                // Nothing to search anymore - add tail and break loop
                if (ErlToken.Category.ATOM==mode || ErlToken.Category.STRING==mode) {
                    // Error: unpaired quote
                    final String toAppend = line.substring(prev, line.length());
                    tokens.getLast().text += toAppend;
                    tokens.getLast().type = ErlToken.Category.ATOM==mode? ErlToken.Category.ERROR_ATOM : ErlToken.Category.ERROR_STRING;
                    error = new ErlErrorInfo(startLine, ErlErrorInfo.ERROR, null, "Unpaired quote", null);
                }
                else {
                    if (line.length() > prev) {
                        int start = Integer.max(prev, 0);
                        final String text = line.substring(start);
                        tokens.add(new ErlToken(ErlToken.Category.UNDEFINED, startLine, start+1, text));
                    }
                }
                break;
            }
        }
        return new SplitResult(tokens, error);
    }

    private SplitResult detectSpecialTypes(SplitResult splitResult, String lineText) {
        for (int i=0; i<splitResult.tokens.size(); ++i) {
            ErlToken token = splitResult.tokens.get(i);
            final String textBefore = lineText.substring(0, token.column-1).trim();
            if (ErlToken.Category.ATOM == token.type && i>0) {
                ErlToken previousToken = splitResult.tokens.get(i-1);
                final String previousLexem = previousToken.text;
                if (textBefore.equals("-")) {
                    previousToken.type = token.type = ErlToken.Category.PREPROCESSOR;
                }
                else if (textBefore.equals("-record(")) {
                    token.type = ErlToken.Category.RECORD;
                }
                else if (textBefore.equals("-module(")) {
                    token.type = ErlToken.Category.MODULE;
                }
                else if (previousLexem.equals(":")) {
                    token.type = ErlToken.Category.FUNCTION;
                }
                else if (previousLexem.equals("#")) {
                    previousToken.type = token.type = ErlToken.Category.RECORD;
                }
            }
            else if (ErlToken.Category.VAR == token.type && i>0) {
                ErlToken previousToken = splitResult.tokens.get(i-1);
                final String previousLexem = previousToken.text;
                if (previousLexem.equals("?") || textBefore.equals("-define(")) {
                    previousToken.type = token.type = ErlToken.Category.MACRO;
                }
            }
            else if (ErlToken.Category.OPERATOR == token.type && i>0) {
                ErlToken previousToken = splitResult.tokens.get(i-1);
                final String textBeforePrevToken = lineText.substring(0, previousToken.column-1).trim();
                if (token.text.equals(":") && previousToken.type== ErlToken.Category.ATOM) {
                    previousToken.type = ErlToken.Category.MODULE;
                }
                else if (token.text.equals("(") && previousToken.type== ErlToken.Category.ATOM) {
                    if (textBeforePrevToken.equals("-spec") || textBeforePrevToken.equals("-callback"))
                        previousToken.type = ErlToken.Category.SPEC_FUNCTION;
                    else if (textBefore.startsWith("-spec") || textBefore.startsWith("-type")  || textBefore.startsWith("-callback"))
                        previousToken.type = ErlToken.Category.SPEC_TYPE;
                    else
                        previousToken.type = ErlToken.Category.FUNCTION;
                }
            }
            if (token.type== ErlToken.Category.DOT || token.type==ErlToken.Category.OPERATOR) {
                if (textBefore.startsWith("-spec") || textBefore.startsWith("-type") || textBefore.startsWith("-callback"))
                    token.type = ErlToken.Category.SPEC_OPERATOR;
            }
        }
        for (int i=0; i<splitResult.tokens.size(); ++i) {
            ErlToken token = splitResult.tokens.get(i);
            final String textBefore = lineText.substring(0, token.column-1).trim();
            if (token.type== ErlToken.Category.ATOM) {
                if (textBefore.startsWith("-spec") || textBefore.startsWith("-type") || textBefore.startsWith("-callback"))
                    token.type = ErlToken.Category.SPEC_ATOM;
            }
        }
        return splitResult;
    }


    public CompileResult compile(final String sourceFileName) {
        CompileResult result = null;
        try {
            File file = new File(sourceFileName);
            final String dir = file.getParent();
            final String outDir = Paths.get(dir + "/../ebin").normalize().toAbsolutePath().toString();

            OtpErlangList options = new OtpErlangList(new OtpErlangObject [] {
                    new OtpErlangAtom("return_errors"),
                    new OtpErlangAtom("return_warnings"),
                    new OtpErlangAtom("debug_info"),
                    new OtpErlangTuple(new OtpErlangObject[]{ new OtpErlangAtom("outdir"), new OtpErlangString(outDir) })
            });
            OtpErlangList arguments = new OtpErlangList(new OtpErlangObject[] {
                    new OtpErlangString(sourceFileName), options
            } );
            OtpErlangTuple erlResult = (OtpErlangTuple) vm.run("compile", "file", arguments);
            OtpErlangAtom erlStatus = (OtpErlangAtom) erlResult.elementAt(0);
            final String status = erlStatus.atomValue();
            OtpErlangList erlErrors = null;
            OtpErlangList erlWarnings = null;

            String targetModuleName = null;
            String targetModuleFile = null;

            if (status.equals("ok")) {
                erlWarnings = (OtpErlangList) erlResult.elementAt(2);
                targetModuleName = ((OtpErlangAtom) erlResult.elementAt(1)).atomValue();
                targetModuleFile = outDir + "/" +targetModuleName +".beam";
            }
            else if (status.equals("error")) {
                erlErrors = (OtpErlangList) erlResult.elementAt(1);
                erlWarnings = (OtpErlangList) erlResult.elementAt(2);
            }

            LinkedList<ErlErrorInfo> messages = new LinkedList<>();

            if (erlErrors!=null) {
                messages.addAll(parseCompilerErrorsOrWarnings(erlErrors, ErlErrorInfo.ERROR));
            }
            if (erlWarnings!=null) {
                messages.addAll(parseCompilerErrorsOrWarnings(erlWarnings, ErlErrorInfo.WARNNING));
            }

            result = new CompileResult(targetModuleFile, messages);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (OtpAuthException e) {
            e.printStackTrace();
        } catch (OtpErlangDecodeException e) {
            e.printStackTrace();
        } catch (OtpErlangExit otpErlangExit) {
            otpErlangExit.printStackTrace();
        }
        return result;
    }

    private LinkedList<ErlErrorInfo> parseCompilerErrorsOrWarnings(OtpErlangList rootList, int type) {
        LinkedList<ErlErrorInfo> result = new LinkedList<>();
        try {
            for (OtpErlangObject obj : rootList) {
                OtpErlangTuple erlModule = (OtpErlangTuple) obj;
                final String errorFileName = ((OtpErlangString) erlModule.elementAt(0)).stringValue();
                OtpErlangList errorsList = (OtpErlangList) erlModule.elementAt(1);
                for (OtpErlangObject errorObject : errorsList) {
                    OtpErlangTuple errorItem = (OtpErlangTuple) errorObject;
                    OtpErlangObject location = errorItem.elementAt(0);
                    OtpErlangAtom messageModule = (OtpErlangAtom) errorItem.elementAt(1);
                    OtpErlangObject unformattedMessage = errorItem.elementAt(2);
                    int lineNumber = 0;
                    if (location instanceof OtpErlangLong) {
                        lineNumber = ((OtpErlangLong) location).intValue();
                    } else if (location instanceof OtpErlangTuple) {
                        OtpErlangObject locationLine = ((OtpErlangTuple) location).elementAt(0);
                        lineNumber = ((OtpErlangLong) locationLine).intValue();
                    }
                    final String messageModuleName = messageModule.atomValue();
                    OtpErlangObject formattedUnflattenMessage = vm.run(messageModuleName, "format_error", new OtpErlangList(unformattedMessage));
                    OtpErlangObject formattedMessage = vm.run("lists", "flatten", new OtpErlangList(new OtpErlangObject[]{formattedUnflattenMessage}));
                    final String message = ((OtpErlangString) formattedMessage).stringValue();
                    result.add(new ErlErrorInfo(lineNumber, type, errorFileName, message, null));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (OtpErlangExit otpErlangExit) {
            otpErlangExit.printStackTrace();
        } catch (OtpErlangDecodeException e) {
            e.printStackTrace();
        } catch (OtpAuthException e) {
            e.printStackTrace();
        } catch (OtpErlangRangeException e) {
            e.printStackTrace();
        }
        return result;
    }

    public boolean isReservedWord(String text) {
        for (final String s : KEYWORDS) {
            if (s.equals(text))
                return true;
        }
        return false;
    }

    public boolean isOperator(String text) {
        for (final String s : OPERATORS) {
            if (s.equals(text))
                return true;
        }
        return false;
    }

    public static boolean isValidModuleName(String moduleName) {
        if (moduleName.isEmpty())
            return false;
        final char firstChar = moduleName.charAt(0);
        if (firstChar<'a' || firstChar>'z')
            return false;
        for (int i=1; i<moduleName.length(); ++i) {
            final char ch = moduleName.charAt(i);
            boolean isLetter = 'a' <= ch && ch <= 'z';
            boolean isDigit = '0' <= ch && ch <= '9';
            boolean isUnderscore = '_' == ch;
            if (!isLetter && !isDigit && !isUnderscore)
                return false;
        }
        return true;
    }
}
