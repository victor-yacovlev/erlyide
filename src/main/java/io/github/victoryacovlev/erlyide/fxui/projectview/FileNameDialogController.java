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

import io.github.victoryacovlev.erlyide.project.ErlangFileType;
import io.github.victoryacovlev.erlyide.project.ErlangSourceFile;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;

public class FileNameDialogController {


    public boolean isAccepted() {
        return accepted;
    }

    public enum Mode {
        CreateNewFile, RenameExisitingFile
    }

    private ProjectFileItem projectFileItem;
    private ErlangFileType fileType;
    private String oldName;
    private Stage stage;
    private boolean accepted;
    private Mode mode;
    private File root;

    @FXML private Label suffix;
    @FXML private TextField newName;
    @FXML private Button btnOK;
    @FXML private Button btnCancel;
    @FXML private CheckBox preprocessItself;
//    @FXML private CheckBox findUsages;
    @FXML private Label error;


    public void initializeWithItem(ProjectFileItem projectFileItem) {
        this.projectFileItem = projectFileItem;
        oldName = projectFileItem.getValue();
        accepted = false;
        int dotPos = oldName.indexOf('.');
        String name = oldName.substring(0, dotPos);
        String suff = oldName.substring(dotPos);
        suffix.setText(suff);
        newName.setText(name);
        error.setText("");
        setMode(Mode.RenameExisitingFile);
    }

    public void initializeWithSuggestedName(String fileName, File root, ErlangFileType fileType) {
        setMode(Mode.CreateNewFile);
        this.root = root;
        this.fileType = fileType;

        oldName = fileName;
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

    public void setMode(Mode mode) {
        this.mode = mode;
        if (Mode.CreateNewFile == mode) {
            preprocessItself.setVisible(false);
//            findUsages.setVisible(false);
            stage.setTitle("New file");
            stage.setMinWidth(470);
            stage.setMinHeight(160);
            stage.setMaxWidth(470);
            stage.setMaxHeight(160);
        }
        else {
            preprocessItself.setVisible(true);
//            findUsages.setVisible(false);  // Not implemented yet
            stage.setTitle("Rename file");
            stage.setMinWidth(470);
            stage.setMinHeight(160);
            stage.setMaxWidth(470);
            stage.setMaxHeight(160);
        }
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
        ErlangFileType ft;
        if (Mode.CreateNewFile == mode) {
            ft = fileType;
        }
        else {
            if (projectFileItem.getProjectFile() instanceof ErlangSourceFile) {
                ft = ErlangFileType.SourceFile;
            }
            else {
                ft = ErlangFileType.OtherFile;
            }
        }
        if (ErlangFileType.SourceFile==ft) {
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
        File parentDir = Mode.CreateNewFile==mode ? root : new File(projectFileItem.getProjectFile().getFile().getParent());
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

    public String getEnteredName() {
        return newName.getText().trim();
    }

    public String getSuffix() {
        return suffix.getText();
    }

    public boolean isPreprocessItselfSelected() {
        return preprocessItself.isSelected();
    }

    public boolean isUpdateUsagesSelected() {
//        return findUsages.isSelected();
        return false;
    }
}
