package com.designer.view;

import com.designer.model.DiagramModel;
import com.designer.model.FlowNode;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.awt.*;

public class MainEditorWindow extends BorderPane{

    private DiagramModel model;
    private Pane canvasArea;
    public MainEditorWindow(DiagramModel model)
    {
        this.model=model;
        this.canvasArea=new Pane();
        ToolBar toolbar=new ToolBar();
        Button btnSelect=new Button("Select");
        Button btnDraw=new Button("Draw");

        toolbar.getItems().addAll(btnSelect,btnDraw);

        this.canvasArea.setStyle("-fx-background-color: #e0e0e0;");

        this.setTop(toolbar);
        this.setCenter(canvasArea);
    }

    public void drawDiagram()
    {
        canvasArea.getChildren().clear();
        for( FlowNode node : model.getNodes() )
        {
            Rectangle rect=new Rectangle(node.getX(), node.getY(), node.getWidth(), node.getHeight());
            rect.setFill(Color.LIGHTBLUE);
            rect.setStroke(Color.BLACK);

            canvasArea.getChildren().add(rect);
        }
    }

}
