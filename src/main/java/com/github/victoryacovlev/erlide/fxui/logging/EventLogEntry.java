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

package com.github.victoryacovlev.erlide.fxui.logging;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.joda.time.DateTime;

public class EventLogEntry {
    private final SimpleStringProperty message;
    private final SimpleStringProperty posted;
    private final DateTime dateTime;
    private long timeout;

    public EventLogEntry(String message, long timeout) {
        this.message = new SimpleStringProperty(message);
        this.dateTime = DateTime.now();
        this.timeout = timeout;
        this.posted = new SimpleStringProperty(dateTime.toLocalTime().toString());
    }

    public EventLogEntry(String message) {
        this.message = new SimpleStringProperty(message);
        this.dateTime = DateTime.now();
        this.timeout = 0;
        this.posted = new SimpleStringProperty(dateTime.toLocalTime().toString());
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
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
}
