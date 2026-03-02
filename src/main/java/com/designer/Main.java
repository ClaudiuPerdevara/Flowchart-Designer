package com.designer;

import com.designer.controller.EditorController;
import com.designer.model.DiagramModel;
import com.designer.model.DiamondNode;
import com.designer.model.FlowNode;
import com.designer.view.MainEditorWindow;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {

        DiagramModel model = new DiagramModel();
        FlowNode node=new DiamondNode(100,100,100,60,"yes");
        node.setRotation(45);
        model.addNode(node);


        MainEditorWindow window = new MainEditorWindow(model);
        window.drawDiagram();

        new EditorController(model,window);

        Scene scene = new Scene(window, 1500, 900);
        primaryStage.setTitle("Flowchart Designer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}