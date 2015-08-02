// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2015 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.wizards;

import static com.google.appinventor.client.Ode.MESSAGES;

import com.allen_sauer.gwt.dnd.client.util.StringUtil;
import com.google.appinventor.client.Ode;
import com.google.appinventor.client.OdeAsyncCallback;
import com.google.appinventor.client.editor.youngandroid.YaProjectEditor;
import com.google.appinventor.client.explorer.project.Project;
import com.google.appinventor.common.utils.StringUtils;
import com.google.appinventor.shared.rpc.component.Component;
import com.google.appinventor.shared.rpc.project.ProjectNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidAssetsFolder;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidComponentsFolder;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidProjectNode;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.Command;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;

import java.util.List;

public class ComponentImportWizard extends Wizard {



  private static class ImportComponentCallback extends OdeAsyncCallback<List<ProjectNode>> {
    @Override
    public void onSuccess(List<ProjectNode> compNodes) {
      if (compNodes.isEmpty())  return;
      long projectId = ode.getCurrentYoungAndroidProjectId();
      Project project = ode.getProjectManager().getProject(projectId);
      YoungAndroidComponentsFolder componentsFolder = ((YoungAndroidProjectNode) project.getRootNode()).getComponentsFolder();
      YaProjectEditor projectEditor = (YaProjectEditor) ode.getEditorManager().getOpenProjectEditor(projectId);

      for (ProjectNode node : compNodes) {
        project.addNode(componentsFolder,node);
        if (node.getName().endsWith(".json") && StringUtils.countMatches(node.getFileId(),"/") == 3) {
          projectEditor.addComponent(node, null);
        }
      }

    }
  }

  private static int MY_COMPONENT_TAB = 1;
  private static int URL_TAB = 0;

  private static final Ode ode = Ode.getInstance();

  public ComponentImportWizard() {
    super(MESSAGES.componentImportWizardCaption(), true, false);

    final CellTable compTable = createCompTable();
    final Grid urlGrid = createUrlGrid();
    final TabPanel tabPanel = new TabPanel();
    // tabPanel.add(compTable, "My components");
    tabPanel.add(urlGrid, "URL");
    tabPanel.selectTab(URL_TAB);
    tabPanel.addStyleName("ode-Tabpanel");

    VerticalPanel panel = new VerticalPanel();
    panel.add(tabPanel);

    addPage(panel);

    setPagePanelHeight(150);
    setPixelSize(200, 150);
    setStylePrimaryName("ode-DialogBox");

    ListDataProvider<Component> dataProvider = provideData();
    dataProvider.addDataDisplay(compTable);

    initFinishCommand(new Command() {
      @Override
      public void execute() {
        final long projectId = ode.getCurrentYoungAndroidProjectId();
        final Project project = ode.getProjectManager().getProject(projectId);
        final YoungAndroidAssetsFolder assetsFolderNode =
            ((YoungAndroidProjectNode) project.getRootNode()).getAssetsFolder();

        if (tabPanel.getTabBar().getSelectedTab() == MY_COMPONENT_TAB) {
          SingleSelectionModel<Component> selectionModel =
              (SingleSelectionModel<Component>) compTable.getSelectionModel();
          Component toImport = selectionModel.getSelectedObject();

          if (toImport == null) {
            showAlert(MESSAGES.noComponentSelectedError());
            return;
          }

          ode.getComponentService().importComponentToProject(toImport, projectId,
              assetsFolderNode.getFileId(), new ImportComponentCallback());

        } else if (tabPanel.getTabBar().getSelectedTab() == URL_TAB) {
          TextBox urlTextBox = (TextBox) urlGrid.getWidget(1, 0);
          String url = urlTextBox.getText();

          if (url.trim().isEmpty()) {
            showAlert(MESSAGES.noUrlError());
            return;
          }

          ode.getComponentService().importComponentToProject(url, projectId,
              assetsFolderNode.getFileId(), new ImportComponentCallback());
        }
      }
    });
  }

  private CellTable createCompTable() {
    final SingleSelectionModel<Component> selectionModel =
        new SingleSelectionModel<Component>();

    CellTable<Component> compTable = new CellTable<Component>();
    compTable.setSelectionModel(selectionModel);

    Column<Component, Boolean> checkColumn =
        new Column<Component, Boolean>(new CheckboxCell(true, false)) {
          @Override
          public Boolean getValue(Component comp) {
            return selectionModel.isSelected(comp);
          }
        };
    Column<Component, String> nameColumn =
        new Column<Component, String>(new TextCell()) {
          @Override
          public String getValue(Component comp) {
            return comp.getName();
          }
        };
    Column<Component, Number> versionColumn =
        new Column<Component, Number>(new NumberCell()) {
          @Override
          public Number getValue(Component comp) {
            return comp.getVersion();
          }
        };

    compTable.addColumn(checkColumn);
    compTable.addColumn(nameColumn, "Component");
    compTable.addColumn(versionColumn, "Version");

    return compTable;
  }

  private Grid createUrlGrid() {
    TextBox urlTextBox = new TextBox();
    urlTextBox.setWidth("100%");
    Grid grid = new Grid(2, 1);
    grid.setWidget(0, 0, new Label("Url:"));
    grid.setWidget(1, 0, urlTextBox);
    return grid;
  }

  private ListDataProvider<Component> provideData() {
    ListDataProvider<Component> provider = new ListDataProvider<Component>();
    for (Component comp : ode.getComponentManager().getComponents()) {
      provider.getList().add(comp);
    }
    return provider;
  }

  private void showAlert(String message) {
    Window.alert(message);
    center();
  }
}
