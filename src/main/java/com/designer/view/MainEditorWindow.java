package com.designer.view;

import com.designer.model.DiagramModel;
import com.designer.model.DiamondNode;
import com.designer.model.FlowNode;
import com.designer.model.RectangleNode;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Polygon;


import java.awt.*;

public class MainEditorWindow extends BorderPane{

    private DiagramModel model;
    private Pane canvasArea;
    private Button btnSelect,btnRect,btnDiam;
    private ToolBar toolbar;
    public MainEditorWindow(DiagramModel model)
    {
        this.model=model;
        this.canvasArea=new Pane();
        this.toolbar=new ToolBar();
        this.btnSelect=new Button("Select");
        this.btnRect=new Button("Rectangle");
        this.btnDiam=new Button("Diamond");

        toolbar.getItems().addAll(btnSelect,btnRect,btnDiam);

        this.canvasArea.setStyle("-fx-background-color: #e0e0e0;");

        this.setTop(toolbar);
        this.setCenter(canvasArea);
    }

    public void drawDiagram()
    {
        canvasArea.getChildren().clear();
        for( FlowNode node : model.getNodes() )
        {
            if(node instanceof RectangleNode)
            {
                Rectangle rect=new Rectangle(node.getX(), node.getY(), node.getWidth(), node.getHeight());
                rect.setFill(Color.LIGHTBLUE);
                rect.setStroke(Color.BLACK);
                canvasArea.getChildren().add(rect);
            }
            if(node instanceof DiamondNode)
            {
                Polygon diamond = new Polygon();
                diamond.getPoints().addAll(new Double[]{
                        node.getX() + node.getWidth() / 2, node.getY(),                 // Punctul Sus
                        node.getX() + node.getWidth(), node.getY() + node.getHeight() / 2,  // Punctul Dreapta
                        node.getX() + node.getWidth() / 2, node.getY() + node.getHeight(),  // Punctul Jos
                        node.getX(), node.getY() + node.getHeight() / 2                  // Punctul Stânga
                });

                diamond.setFill(Color.WHITE);
                diamond.setStroke(Color.BLACK);
                diamond.setStrokeWidth(2);
                canvasArea.getChildren().add(diamond);
            }




        }
    }

    public Pane getCanvasArea()
    {
        return this.canvasArea;
    }

    public Button getBtnSelect() { return btnSelect; }
    public Button getBtnRect() { return btnRect; }
    public Button getBtnDiam() { return btnDiam; }
}
