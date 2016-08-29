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

package io.github.victoryacovlev.erlyide.fxui.logging;

import javafx.beans.property.SimpleStringProperty;
import org.joda.time.DateTime;

public class EventLogEntry {
    private SimpleStringProperty message;
    private SimpleStringProperty longMessage = null;
    private final SimpleStringProperty posted;
    private final DateTime dateTime;


    public EventLogEntry(String message) {
        this.message = new SimpleStringProperty(message);
        this.longMessage = new SimpleStringProperty(message);
        this.dateTime = DateTime.now();
        this.posted = new SimpleStringProperty(dateTime.toLocalTime().toString());
    }

    public String getMessage() {
        return message.get();
    }

    public SimpleStringProperty messageProperty() {
        return message;
    }

    public String getPosted() {
        return posted.get();
    }

    public SimpleStringProperty postedProperty() {
        return posted;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    public String getLongMessage() {
        return longMessage.get();
    }

    public SimpleStringProperty longMessageProperty() {
        return longMessage;
    }

    public void setLongMessage(String longMessage) {
        this.longMessage.set(longMessage);
    }

    public void mergeAfter(EventLogEntry previous) {
        // To be implemented in clild classes if possible
    }
}
