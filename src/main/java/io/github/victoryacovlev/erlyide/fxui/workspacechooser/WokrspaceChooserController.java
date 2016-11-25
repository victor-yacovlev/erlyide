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

package io.github.victoryacovlev.erlyide.fxui.workspacechooser;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class WokrspaceChooserController implements Initializable {

    @FXML private ComboBox<String> comboBoxHistory;
    @FXML private Button buttonBrowse;
    @FXML private Button buttonOK;
    @FXML private Button buttonCancel;
    @FXML private CheckBox checkBoxShowNextLaunch;
    private Stage stage;
    private boolean accepted = false;
    private final Preferences preferences = Preferences.userNodeForPackage(WokrspaceChooserController.class);
    private static final String PREFS_WINDOW_GEOMETRY = "WorkspaceChooser/Geometry";
    private static final String PREFS_SHOW_ON_STARTUP = "WorkspaceChooser/ShowOnStartup";
    private static final String PREFS_HISTORY = "WorkspaceChooser/History";
    private DirectoryChooser directoryChooser = new DirectoryChooser();

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    @FXML
    private void accept() {
        accepted = true;
        saveSettings(false);
        saveHistory();
        stage.close();
    }

    @FXML
    private void reject() {
        saveSettings(!accepted);
        stage.close();
    }

    @FXML
    private void browseLocation() {
        directoryChooser.setTitle("Browse workspace location");
        directoryChooser.setInitialDirectory(new File(getSelectedWorkspacePath()));
        File entry = directoryChooser.showDialog(stage);
        if (null != entry) {
            boolean exists = false;
            String fullPath = entry.getAbsolutePath();
            for (int i=0; i<comboBoxHistory.getItems().size(); ++i) {
                String itemPath = comboBoxHistory.getItems().get(i);
                if (itemPath.trim().equals(fullPath)) {
                    exists = true;
                    comboBoxHistory.getSelectionModel().select(i);
                    break;
                }
            }
            if (!exists) {
                comboBoxHistory.getItems().add(0, fullPath);
                comboBoxHistory.getSelectionModel().select(0);
            }
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        this.stage.setOnCloseRequest(event -> {saveSettings(!accepted);});

    }

    public void restoreSettings() {
        double CENTER_ON_SCREEN_X_FRACTION = 1.0f / 2;
        double CENTER_ON_SCREEN_Y_FRACTION = 1.0f / 3;
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double centerX = bounds.getMinX() + (bounds.getWidth() - stage.getMinWidth())
                * CENTER_ON_SCREEN_X_FRACTION;
        double centerY = bounds.getMinY() + (bounds.getHeight() - stage.getMinHeight())
                * CENTER_ON_SCREEN_Y_FRACTION;


        int x = preferences.getInt(PREFS_WINDOW_GEOMETRY + "/x", (int) centerX);
        int y = preferences.getInt(PREFS_WINDOW_GEOMETRY + "/y", (int) centerY);
        stage.setX(x);
        stage.setY(y);
        checkBoxShowNextLaunch.setSelected(isShowOnStartup());
    }

    public void saveSettings(boolean positionOnly) {
        preferences.putInt(PREFS_WINDOW_GEOMETRY + "/x", (int) stage.getX());
        preferences.putInt(PREFS_WINDOW_GEOMETRY + "/y", (int) stage.getY());
        if (positionOnly)
            return;
        preferences.putBoolean(PREFS_SHOW_ON_STARTUP, checkBoxShowNextLaunch.isSelected());
    }


    public boolean execDialog(Stage modalStage) {
        accepted = false;
        restoreSettings();
        loadHistory();
        comboBoxHistory.getEditor().selectAll();
        if (null != modalStage && stage.getOwner() != modalStage) {
            stage.initOwner(modalStage);
            stage.initModality(Modality.WINDOW_MODAL);
        }
        stage.showAndWait();
        return accepted;
    }

    private void loadHistory() {
        String rawHistory = preferences.get(PREFS_HISTORY, "[]");
        JSONArray jsonHistory = new JSONArray(rawHistory);
        comboBoxHistory.getItems().clear();
        for (int i=0; i<jsonHistory.length(); ++i) {
            JSONObject jsonObject = (JSONObject) jsonHistory.get(i);
            String path = jsonObject.getString("path");
            boolean selected = jsonObject.getBoolean("selected");
            File f = new File(path);
            if (f.exists() && f.isDirectory()) {
                comboBoxHistory.getItems().add(path);
                if (selected) {
                    comboBoxHistory.getSelectionModel().select(comboBoxHistory.getItems().size()-1);
                }
            }
        }
        if (comboBoxHistory.getItems().isEmpty()) {
            comboBoxHistory.getItems().add(getDefaultWorkspacePath());
            comboBoxHistory.getSelectionModel().select(0);
        }
    }

    private String getDefaultWorkspacePath() {
        String defaultPath = System.getenv("HOME")+"/ErlangWorkspace";
        return defaultPath;
    }

    private void saveHistory() {
        JSONArray resut = new JSONArray();
        for (int i=0; i<comboBoxHistory.getItems().size(); ++i) {
            String path = comboBoxHistory.getItems().get(i);
            boolean selected = comboBoxHistory.getSelectionModel().isSelected(i);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("path", path);
            jsonObject.put("selected", selected);
            resut.put(jsonObject);
        }
        String raw = resut.toString();
        preferences.put(PREFS_HISTORY, raw);
    }

    public boolean isShowOnStartup() {
        boolean showOnStartup = preferences.getBoolean(PREFS_SHOW_ON_STARTUP, true);
        return showOnStartup;
    }

    public String getSelectedWorkspacePath() {
        if (comboBoxHistory.getSelectionModel().isEmpty())
            loadHistory(); // Might be dialog not shown, so it's empty
        if (comboBoxHistory.getSelectionModel().isEmpty())
            return getDefaultWorkspacePath();
        else
            return comboBoxHistory.getSelectionModel().getSelectedItem();
    }
}
