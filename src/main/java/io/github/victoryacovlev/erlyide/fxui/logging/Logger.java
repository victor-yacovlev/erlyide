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

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Labeled;
import javafx.scene.control.TableView;

import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import org.joda.time.DateTime;

import java.util.*;

public class Logger {

    private static Logger instance = null;

    private ObservableList<IssuesLogEntry> issues = FXCollections.observableArrayList();
    private ObservableList<EventLogEntry> events = FXCollections.observableArrayList();
    private List<Labeled> lastEntryListeners = new LinkedList<>();

    public static Logger getInstance() {
        if (null==instance) {
            instance = new Logger();
        }
        return instance;
    }

    private Logger() {
        new Thread(() -> {
            while (true) {
                String message = "No events";
                if (events.size() > 0) {
                    EventLogEntry lastEvent = events.get(events.size()-1);
                    message = lastEvent.getMessage();
                    long when = lastEvent.getDateTime().getMillis();
                    long now = DateTime.now().getMillis();
                    long agoMills = now - when;
                    long agoSeconds = agoMills / 1000;
                    long agoMinutes = agoSeconds / 60;
                    long agoHours = agoMinutes / 60;
                    long agoDays = agoHours / 24;
                    long what = 0;
                    String suffix = "";
                    if (agoDays != 0) {
                        what = agoDays;
                        suffix = "day";
                    }
                    else if (agoHours != 0) {
                        what = agoHours;
                        suffix = "hour";
                    }
                    else if (agoMinutes != 0) {
                        what = agoMinutes;
                        suffix = "minute";
                    }
                    if (what == 0) {
                        message += " (moments ago)";
                    }
                    else if (what == 1) {
                        message += " (a " + suffix + " ago)";
                    }
                    else {
                        message += " (" + Long.toString(what) + " " + suffix + "s ago)";
                    }
                }
                String finalMessage = message;
                Platform.runLater(() -> {
                    for (Labeled l : lastEntryListeners) {
                        l.setText(finalMessage);
                    }
                });
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    public void addIssueEntry(IssuesLogEntry entry) {
        issues.add(entry);
    }

    public void addEventEntry(EventLogEntry entry) {
        events.add(entry);
    }

    public void clearIssues() {
        issues.clear();
    }

    public void clearEvents() {
        events.clear();
    }

    public boolean hasIssues() {
        return issues.size() > 0;
    }

    public void addIssuesTableView(TableView view) {
        TableColumn kindColumn = (TableColumn) view.getColumns().get(0);
        TableColumn messageColumn = (TableColumn) view.getColumns().get(1);
        TableColumn locationColumn = (TableColumn) view.getColumns().get(2);
        kindColumn.setCellValueFactory(new PropertyValueFactory<IssuesLogEntry,String>("kind"));
        messageColumn.setCellValueFactory(new PropertyValueFactory<IssuesLogEntry,String>("message"));
        locationColumn.setCellValueFactory(new PropertyValueFactory<IssuesLogEntry,String>("location"));
        view.setItems(issues);
    }

    public void addEventsTableView(TableView view) {
        TableColumn timeColumn = (TableColumn) view.getColumns().get(0);
        TableColumn messageColumn = (TableColumn) view.getColumns().get(1);
        timeColumn.setCellValueFactory(new PropertyValueFactory<EventLogEntry,String>("posted"));
        messageColumn.setCellValueFactory(new PropertyValueFactory<EventLogEntry,String>("message"));
        view.setItems(events);
    }

    public void addLastEventView(Labeled label) {
        lastEntryListeners.add(label);
    }

    public void addIssueEntries(List<IssuesLogEntry> compilerLogEntries) {
        issues.addAll(compilerLogEntries);
    }

}
