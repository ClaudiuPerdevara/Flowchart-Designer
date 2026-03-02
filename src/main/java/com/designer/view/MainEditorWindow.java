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
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
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
                rect.setFill(Color.WHITE);
                rect.setMouseTransparent(true);

                double centerX=node.getX()+node.getWidth()/2;
                double centerY=node.getY()+node.getHeight()/2;

                javafx.scene.transform.Rotate pivot = new javafx.scene.transform.Rotate(node.getRotation(), centerX, centerY);
                rect.getTransforms().add(pivot);

                if(node.isSelected())
                {
                    double topY=node.getY();
                    double handleY=node.getY()-30;

                    Line antenaLine=new Line(centerX,topY,centerX,handleY);
                    antenaLine.setStroke(Color.GRAY);
                    antenaLine.setStrokeWidth(2);

                    Circle antenaCircle = new Circle(centerX, handleY, 5);
                    antenaCircle.setFill(Color.LIMEGREEN);
                    antenaCircle.setStroke(Color.BLACK);

                    rect.setStroke(Color.DODGERBLUE);
                    rect.setStrokeWidth(3);
                    rect.getStrokeDashArray().addAll(5.0, 5.0);

                    antenaLine.getTransforms().add(pivot);
                    antenaCircle.getTransforms().add(pivot);

                    canvasArea.getChildren().addAll(rect, antenaLine, antenaCircle);
                }
                else
                {
                    rect.setStroke(Color.BLACK);
                    rect.setStrokeWidth(1);
                    canvasArea.getChildren().add(rect);
                }

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
                diamond.setMouseTransparent(true);
                if(node.isSelected()) {
                    diamond.setStroke(Color.DODGERBLUE);
                    diamond.setStrokeWidth(3);
                    diamond.getStrokeDashArray().addAll(5.0, 5.0);
                } else {
                    diamond.setStroke(Color.BLACK);
                    diamond.setStrokeWidth(2);
                }
                diamond.setRotate(node.getRotation());
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
