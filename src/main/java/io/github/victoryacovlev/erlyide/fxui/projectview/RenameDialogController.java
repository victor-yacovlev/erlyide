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

package io.github.victoryacovlev.erlyide.fxui.projectview;

import io.github.victoryacovlev.erlyide.project.ErlangSourceFile;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.File;

public class RenameDialogController {

    private ProjectFileItem projectFileItem;
    private String oldName;
    private Stage stage;
    private boolean accepted;

    @FXML private Label suffix;
    @FXML private TextField newName;
    @FXML private Button btnOK;
    @FXML private Button btnCancel;
    @FXML private CheckBox preprocessItself;
    @FXML private CheckBox findUsages;
    @FXML private Label error;


    void initializeWithItem(ProjectFileItem projectFileItem) {
        this.projectFileItem = projectFileItem;
        oldName = projectFileItem.getValue();
        accepted = false;
        int dotPos = oldName.indexOf('.');
        String name = oldName.substring(0, dotPos);
        String suff = oldName.substring(dotPos);
        suffix.setText(suff);
        newName.setText(name);
        error.setText("");
    }

    void initialize() {
        newName.textProperty().addListener(((observable, oldValue, newValue) -> checkName()));
    }

    @FXML void okPressed() {
        this.accepted = true;
        stage.close();
    }

    @FXML void cancelPressed() {
        this.accepted = false;
        stage.close();
    }

    @FXML void checkName() {
        String name = newName.getText().trim();
        if (name.isEmpty()) {
            error.setText("Name can not be empty");
            btnOK.setDisable(true);
            return;
        }
        if (this.projectFileItem.getProjectFile() instanceof ErlangSourceFile) {
            boolean validErlangName = true;
            char first = name.charAt(0);
            if (first != '_' && (first < 'a' || first > 'z')) {
                validErlangName = false;
            }
            for (int i=1; i<name.length(); ++i) {
                char c = name.charAt(i);
                boolean valid = ('0' <= c && c <= '9') || ('a' <= c && c <= 'z') || (c == '_');
                if (!valid)
                    validErlangName = false;
            }
            if (!validErlangName) {
                error.setText("File name must be a valid Erlang module name");
                btnOK.setDisable(true);
                return;
            }
        }
        File parentDir = new File(projectFileItem.getProjectFile().getFile().getParent());
        File newFile = new File(parentDir.getAbsolutePath() + "/" + name + suffix.getText());
        if (newFile.exists()) {
            if (!newFile.getName().equals(oldName)) {
                error.setText("File with this name already exists");
                btnOK.setDisable(true);
                return;
            }
        }
        btnOK.setDisable(false);
        error.setText("");
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }
}
