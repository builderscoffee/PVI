package io.playpen.visual.controller;

import io.playpen.core.networking.TransactionInfo;
import io.playpen.visual.PVIApplication;
import io.playpen.visual.PVIClient;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class ProvisionDialogController implements Initializable {
    @FXML
    private TextField packageField;

    @FXML
    private TextField versionField;

    @FXML
    private TextField serverNameField;

    @FXML
    private ChoiceBox coordinatorBox;

    @FXML
    private TableView<PropertyValue> propertyTable;

    @FXML
    private TableColumn<PropertyValue, String> nameColumn;

    @FXML
    private TableColumn<PropertyValue, String> valueColumn;

    @Getter
    @Setter
    private Stage stage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        nameColumn.setOnEditCommit(event -> {
            event.getTableView().getItems().get(event.getTablePosition().getRow()).setName(event.getNewValue());
        });

        valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        valueColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        valueColumn.setOnEditCommit(event -> {
            event.getTableView().getItems().get(event.getTablePosition().getRow()).setValue(event.getNewValue());
        });

        propertyTable.getItems().addAll(
                new PropertyValue("id", ""),
                new PropertyValue("port", ""),
                new PropertyValue("managed_by", "user")
        );

        coordinatorBox.getItems().addAll(new ArrayList<>(WorkspaceController.rootNode.getChildren().stream().map(TreeItem::getValue).collect(Collectors.toList())));
        if(!coordinatorBox.getItems().isEmpty()) { coordinatorBox.getSelectionModel().select(0); }
    }

    public void setPackage(String name, String version) {
        packageField.setText(name);
        versionField.setText(version);
    }

    @FXML
    protected void handleCancelButtonPressed(ActionEvent event) {
        stage.close();
    }

    @FXML
    protected void handlePropertyAddButtonPressed(ActionEvent event) {
        propertyTable.getItems().add(new PropertyValue("", ""));
        int idx = propertyTable.getItems().size() - 1;
        propertyTable.getSelectionModel().select(idx);
    }

    @FXML
    protected void handlePropertyRemoveButtonPressed(ActionEvent event) {
        if (propertyTable.getSelectionModel().getSelectedItem() != null)
            propertyTable.getItems().remove(propertyTable.getSelectionModel().getSelectedIndex());
    }

    @FXML
    protected void handleProvisionButtonPressed(ActionEvent event) {
        String id = packageField.getText();
        String version = versionField.getText();
        Map<String, String> properties = new HashMap<>();

        String serverName = serverNameField.getText().trim();
        if (serverName.isEmpty())
            serverName = null;


        String coordinatorName = coordinatorBox.getSelectionModel().getSelectedItem().toString();
        if (coordinatorName.isEmpty())
            coordinatorName = null;

        for (PropertyValue prop : propertyTable.getItems()) {
            if (prop.getName().trim().isEmpty() || prop.getValue().trim().isEmpty())
                continue;

            properties.put(prop.getName(), prop.getValue());
        }

        log.info("Attempting provision of " + id + " @ " + version + " with the following properties:");
        for (Map.Entry<String, String> prop : properties.entrySet()) {
            log.info("  " + prop.getKey() + " = " + prop.getValue());
        }

        TransactionInfo info = PVIClient.get().sendProvision(id, version, coordinatorName, serverName, properties);
        if (info == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Unable to send provision request.");
            alert.showAndWait();
        } else {
            PVIApplication.get().showTransactionDialog("Provision Server", info, null);
            stage.close();
        }
    }

    public static class PropertyValue {
        private final SimpleStringProperty name;
        private final SimpleStringProperty value;

        PropertyValue(String name, String value) {
            this.name = new SimpleStringProperty(name);
            this.value = new SimpleStringProperty(value);
        }

        public String getName() {
            return name.get();
        }

        public void setName(String name) {
            this.name.set(name);
        }

        String getValue() {
            return value.get();
        }

        void setValue(String value) {
            this.value.set(value);
        }
    }
}
