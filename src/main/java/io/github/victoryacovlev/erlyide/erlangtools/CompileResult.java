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

import java.util.LinkedList;
import java.util.List;

public class CompileResult {
    private List<String> outputFileNames;
    private List<String> upToDateFileNames;
    private List<ErlErrorInfo> errorsAndWarnings;

    public CompileResult(String outputFileName, List<ErlErrorInfo> errorsAndWarnings) {
        if (outputFileName!=null) {
            outputFileNames = new LinkedList<>();
            outputFileNames.add(outputFileName);
        }
        else {
            outputFileNames = null;
        }
        this.errorsAndWarnings = errorsAndWarnings;
    }

    public CompileResult(List<String> outputFileNames, List<ErlErrorInfo> errorsAndWarnings) {
        this.outputFileNames = outputFileNames;
        this.errorsAndWarnings = errorsAndWarnings;
    }

    public List<String> getOutputFileNames() {
        if (null==outputFileNames) return new LinkedList<>();
        else return outputFileNames;
    }

    public List<ErlErrorInfo> getErrorsAndWarnings() {
        return errorsAndWarnings;
    }

    public boolean isSuccess() {
        boolean result = true;
        for (ErlErrorInfo msg : errorsAndWarnings) {
            if (ErlErrorInfo.ERROR == msg.type) {
                result = false;
                break;
            }
        }
        return result;
    }


    public List<String> getUpToDateFileNames() {
        return upToDateFileNames;
    }

    public void setUpToDateFileNames(List<String> upToDateFileNames) {
        this.upToDateFileNames = upToDateFileNames;
    }
}
