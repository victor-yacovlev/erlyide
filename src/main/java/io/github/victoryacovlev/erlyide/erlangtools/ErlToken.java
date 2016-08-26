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

public class ErlToken {
    public String text;
    public int line;
    public int column;
    public Category type;

    public enum Category {
        COMMENT, OPERATOR, ATOM, STRING, INTEGER, FLOAT, DOT, WHITE_SPACE, VAR, RESERVED_WORD, PREPROCESSOR,
        FUNCTION, MACRO, RECORD, MODULE, SPEC_FUNCTION, SPEC_TYPE, SPEC_OPERATOR, SPEC_ATOM,
        ERROR_ATOM, ERROR_STRING,
        UNDEFINED
    }

    public ErlToken(Category type, int line, int column, final String text) {
        this.text = text;
        this.line = line;
        this.column = column;
        this.type = type;
    }

    public ErlToken(String type, int line, int column, final String text) {
        this.text = text;
        this.line = line;
        this.column = column;
        if (text.equals(type) && !type.equals("string")) {
            if (containsOnlyKeywordsSymbols(type))
                this.type = Category.RESERVED_WORD;
            else
                this.type = Category.OPERATOR;
        }
        else {
            try {
                this.type = Category.valueOf(type.toUpperCase());
            }
            catch (IllegalArgumentException e) {
                if (containsOnlyKeywordsSymbols(type))
                    this.type = Category.RESERVED_WORD;
                else
                    this.type = Category.OPERATOR;
                this.text = type;
            }
        }
    }

    private boolean containsOnlyKeywordsSymbols(String text) {
        boolean result = text.length() > 0;
        for (int i=0; i<text.length(); ++i) {
            char c = text.charAt(i);
            if (c < 'a' || c > 'z') {
                return false;
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return text + " [at " + Integer.toString(line)+":"+Integer.toString(column)+"]";
    }
}
