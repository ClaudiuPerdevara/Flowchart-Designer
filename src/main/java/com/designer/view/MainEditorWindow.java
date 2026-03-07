package com.designer.view;

import com.designer.model.*;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Polygon;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.concurrent.Flow;


public class MainEditorWindow extends BorderPane{

    private DiagramModel model;
    private Pane canvasArea;
    private Button btnSelect,btnRect,btnDiam,btnConnection;

    private ComboBox<Connection.EndPointStyle> srcEndpointCombo;
    private ComboBox<Connection.EndPointStyle> tgtEndpointCombo;
    private ComboBox<Connection.LineStyle> lineStyleCombo;

    private ToolBar toolbar;
    private FlowNode hoveredNode=null;
    private FlowNode editingNode=null;
    public boolean isConnecting=false;
    private double tempLineStartX,tempLineStartY, tempLineEndX, tempLineEndY;

    public void setTempLine(boolean isConnecting, double sx, double sy, double ex, double ey) {
        this.isConnecting = isConnecting;
        this.tempLineStartX = sx;
        this.tempLineStartY = sy;
        this.tempLineEndX = ex;
        this.tempLineEndY = ey;
    }

    public void setHoveredNode(FlowNode node) { this.hoveredNode=node; }
    public FlowNode getHoveredNode() { return this.hoveredNode; }

    public MainEditorWindow(DiagramModel model)
    {
        this.model=model;
        this.canvasArea=new Pane();
        this.toolbar=new ToolBar();
        this.btnSelect=new Button("Select");
        this.btnRect=new Button("Rectangle");
        this.btnDiam=new Button("Diamond");

        this.srcEndpointCombo=new ComboBox<>();
        this.srcEndpointCombo.getItems().addAll(Connection.EndPointStyle.values());
        this.srcEndpointCombo.setPromptText("Source");

        this.tgtEndpointCombo=new ComboBox<>();
        this.tgtEndpointCombo.getItems().addAll(Connection.EndPointStyle.values());
        this.tgtEndpointCombo.setPromptText("Target");

        this.lineStyleCombo=new ComboBox<>();
        this.lineStyleCombo.getItems().addAll(Connection.LineStyle.values());
        this.lineStyleCombo.setPromptText("Style");

        this.lineStyleCombo.setDisable(true);
        this.tgtEndpointCombo.setDisable(true);
        this.srcEndpointCombo.setDisable(true);

        this.srcEndpointCombo.setOnAction(e -> {
            if (srcEndpointCombo.getValue() != null) {
                for (com.designer.model.Connection c : this.model.getConnections()) {
                    if (c.isSelected()) c.setSrcEndpointStyle(srcEndpointCombo.getValue());
                }
                drawDiagram();
            }
        });

        this.tgtEndpointCombo.setOnAction(e -> {
            if (tgtEndpointCombo.getValue() != null) {
                for (com.designer.model.Connection c : this.model.getConnections()) {
                    if (c.isSelected()) c.setTgtEndpointStyle(tgtEndpointCombo.getValue());
                }
                drawDiagram();
            }
        });

        this.lineStyleCombo.setOnAction(e -> {
            if (lineStyleCombo.getValue() != null) {
                for (com.designer.model.Connection c : this.model.getConnections()) {
                    if (c.isSelected()) c.setLineStyle(lineStyleCombo.getValue());
                }
                drawDiagram();
            }
        });

        HBox toolGroup=new HBox(5,btnSelect,btnRect,btnDiam);
        toolGroup.setAlignment(Pos.CENTER_LEFT);

        HBox propertiesGroup=new HBox(5,new Label("Source:"),srcEndpointCombo,new Label("Target:"),tgtEndpointCombo,new Label("Line:"),lineStyleCombo);
        propertiesGroup.setAlignment(Pos.CENTER_LEFT);

        srcEndpointCombo.setOnAction(e -> {
            if (srcEndpointCombo.getValue() != null) {
                for (com.designer.model.Connection c : model.getConnections()) {
                    if (c.isSelected()) c.setSrcEndpointStyle(srcEndpointCombo.getValue());
                }
                drawDiagram();
            }
        });

        tgtEndpointCombo.setOnAction(e -> {
            if (tgtEndpointCombo.getValue() != null) {
                for (com.designer.model.Connection c : model.getConnections()) {
                    if (c.isSelected()) c.setTgtEndpointStyle(tgtEndpointCombo.getValue());
                }
                drawDiagram();
            }
        });

        lineStyleCombo.setOnAction(e -> {
            if (lineStyleCombo.getValue() != null) {
                for (com.designer.model.Connection c : model.getConnections()) {
                    if (c.isSelected()) c.setLineStyle(lineStyleCombo.getValue());
                }
                drawDiagram();
            }
        });

        toolbar.getItems().addAll(toolGroup,new Separator(),propertiesGroup);

        this.canvasArea.setStyle("-fx-background-color: #e0e0e0;");

        this.setTop(toolbar);
        this.setCenter(canvasArea);
    }

    public void drawDiagram()
    {
        canvasArea.getChildren().clear();
        for (com.designer.model.Connection c : model.getConnections()) {
            drawConnection(c);
        }

        if (isConnecting) {
            Line tempLine = new Line(tempLineStartX, tempLineStartY, tempLineEndX, tempLineEndY);
            tempLine.setStroke(Color.DODGERBLUE);
            tempLine.setStrokeWidth(2);
            tempLine.getStrokeDashArray().addAll(5.0, 5.0);
            canvasArea.getChildren().add(tempLine);
        }

        for( FlowNode node : model.getNodes() )
        {
            if (node instanceof RectangleNode) {
                Rectangle rect = new Rectangle(node.getX(), node.getY(), node.getWidth(), node.getHeight());
                rect.setFill(Color.WHITE);
                rect.setMouseTransparent(true);

                double centerX = node.getX() + node.getWidth() / 2;
                double centerY = node.getY() + node.getHeight() / 2;

                javafx.scene.transform.Rotate pivot = new javafx.scene.transform.Rotate(node.getRotation(), centerX, centerY);

                rect.getTransforms().add(pivot);

                if (node.isSelected()) {
                    rect.setStroke(Color.DODGERBLUE);
                    rect.setStrokeWidth(3);
                    rect.getStrokeDashArray().addAll(5.0, 5.0);
                    canvasArea.getChildren().addAll(rect);

                    double size = 6;
                    Rectangle nw = new Rectangle(node.getX() - size/2, node.getY() - size/2, size, size);
                    Rectangle ne = new Rectangle(node.getX() + node.getWidth() - size/2, node.getY() - size/2, size, size);
                    Rectangle sw = new Rectangle(node.getX() - size/2, node.getY() + node.getHeight() - size/2, size, size);
                    Rectangle se = new Rectangle(node.getX() + node.getWidth() - size/2, node.getY() + node.getHeight() - size/2, size, size);

                    nw.setFill(Color.DODGERBLUE); ne.setFill(Color.DODGERBLUE);
                    sw.setFill(Color.DODGERBLUE); se.setFill(Color.DODGERBLUE);

                    nw.getTransforms().add(pivot); ne.getTransforms().add(pivot);
                    sw.getTransforms().add(pivot); se.getTransforms().add(pivot);

                    Line antenaLine = new Line(centerX, node.getY(), centerX, node.getY() - 30);
                    antenaLine.setStroke(Color.GRAY); antenaLine.setStrokeWidth(2);

                    Circle antenaCircle = new Circle(centerX, node.getY() - 30, 5);
                    antenaCircle.setFill(Color.LIMEGREEN); antenaCircle.setStroke(Color.BLACK);

                    antenaLine.getTransforms().add(pivot); antenaCircle.getTransforms().add(pivot);

                    nw.setMouseTransparent(true); ne.setMouseTransparent(true);
                    sw.setMouseTransparent(true); se.setMouseTransparent(true);
                    antenaLine.setMouseTransparent(true); antenaCircle.setMouseTransparent(true);

                    canvasArea.getChildren().addAll(antenaLine, antenaCircle, sw, se, ne, nw);

                } else {
                    rect.setStroke(Color.BLACK);
                    rect.setStrokeWidth(1);
                    canvasArea.getChildren().addAll(rect);

                    if (node == hoveredNode) {
                        Rectangle hoverBox = new Rectangle(node.getX(), node.getY(), node.getWidth(), node.getHeight());
                        hoverBox.setFill(Color.TRANSPARENT);
                        hoverBox.setStroke(Color.LIMEGREEN);
                        hoverBox.setStrokeWidth(2);
                        hoverBox.getStrokeDashArray().addAll(5.0, 5.0);
                        hoverBox.getTransforms().add(pivot);
                        hoverBox.setMouseTransparent(true);

                        double pSize = 5;
                        int segments=4;

                        double[][] vertices={
                                {node.getX(),node.getY()},
                                {node.getX()+node.getWidth(), node.getY()},
                                {node.getX()+node.getWidth(), node.getY()+node.getHeight()},
                                {node.getX(),node.getY()+node.getHeight()}
                        };

                        for (int i = 0; i < 4; i++)
                        {
                            double startX = vertices[i][0];
                            double startY = vertices[i][1];
                            double endX = vertices[(i + 1) % 4][0];
                            double endY = vertices[(i + 1) % 4][1];

                            for (int j = 0; j < segments; j++)
                            {
                                double t = (double) j / segments; // Procentul distanței (0%, 25%, 50%, 75%)
                                double px = startX + (endX - startX) * t;
                                double py = startY + (endY - startY) * t;

                                Rectangle anchor = new Rectangle(px - pSize / 2, py - pSize / 2, pSize, pSize);
                                anchor.setFill(Color.LIMEGREEN);
                                anchor.setStroke(Color.BLACK);
                                anchor.setStrokeWidth(1);

                                anchor.getTransforms().add(pivot);
                                anchor.setMouseTransparent(true);

                                canvasArea.getChildren().add(anchor);
                            }
                        }
                    }
                }

                if (node != editingNode)
                {
                    Text textNode = new Text(node.getText() != null ? node.getText() : "UML Node");
                    textNode.setFont(Font.font("Consolas", 12));
                    textNode.setTextAlignment(TextAlignment.CENTER);

                    double textW = textNode.getLayoutBounds().getWidth();
                    double textH = textNode.getLayoutBounds().getHeight();
                    textNode.setX(centerX - textW / 2);
                    textNode.setY(centerY - textH / 2 + 10);

                    textNode.getTransforms().add(pivot);
                    textNode.setMouseTransparent(true);
                    canvasArea.getChildren().add(textNode);
                }
            }

            if (node instanceof DiamondNode) {
                Polygon diamond = new Polygon();
                diamond.getPoints().addAll(new Double[]{
                        node.getX() + node.getWidth() / 2, node.getY(),
                        node.getX() + node.getWidth(), node.getY() + node.getHeight() / 2,
                        node.getX() + node.getWidth() / 2, node.getY() + node.getHeight(),
                        node.getX(), node.getY() + node.getHeight() / 2
                });

                diamond.setFill(Color.WHITE);
                diamond.setMouseTransparent(true);

                double centerX = node.getX() + node.getWidth() / 2;
                double centerY = node.getY() + node.getHeight() / 2;

                javafx.scene.transform.Rotate pivot = new javafx.scene.transform.Rotate(node.getRotation(), centerX, centerY);

                diamond.getTransforms().add(pivot);

                if (node.isSelected()) {
                    diamond.setStroke(Color.DODGERBLUE);
                    diamond.setStrokeWidth(3);
                    diamond.getStrokeDashArray().addAll(5.0, 5.0);
                    canvasArea.getChildren().addAll(diamond);

                    double size = 6;
                    Rectangle nw = new Rectangle(node.getX() - size/2, node.getY() - size/2, size, size);
                    Rectangle ne = new Rectangle(node.getX() + node.getWidth() - size/2, node.getY() - size/2, size, size);
                    Rectangle sw = new Rectangle(node.getX() - size/2, node.getY() + node.getHeight() - size/2, size, size);
                    Rectangle se = new Rectangle(node.getX() + node.getWidth() - size/2, node.getY() + node.getHeight() - size/2, size, size);

                    nw.setFill(Color.DODGERBLUE); ne.setFill(Color.DODGERBLUE);
                    sw.setFill(Color.DODGERBLUE); se.setFill(Color.DODGERBLUE);

                    nw.getTransforms().add(pivot); ne.getTransforms().add(pivot);
                    sw.getTransforms().add(pivot); se.getTransforms().add(pivot);

                    Rectangle boundingBox = new Rectangle(node.getX(), node.getY(), node.getWidth(), node.getHeight());
                    boundingBox.setMouseTransparent(true);
                    boundingBox.setFill(Color.TRANSPARENT);
                    boundingBox.setStroke(Color.DODGERBLUE);
                    boundingBox.setStrokeWidth(1);
                    boundingBox.getStrokeDashArray().addAll(5.0, 5.0);
                    boundingBox.getTransforms().add(pivot);

                    Line antenaLine = new Line(centerX, node.getY(), centerX, node.getY() - 30);
                    antenaLine.setStroke(Color.GRAY); antenaLine.setStrokeWidth(2);

                    Circle antenaCircle = new Circle(centerX, node.getY() - 30, 5);
                    antenaCircle.setFill(Color.LIMEGREEN); antenaCircle.setStroke(Color.BLACK);

                    antenaLine.getTransforms().add(pivot); antenaCircle.getTransforms().add(pivot);

                    nw.setMouseTransparent(true); ne.setMouseTransparent(true);
                    sw.setMouseTransparent(true); se.setMouseTransparent(true);
                    antenaLine.setMouseTransparent(true); antenaCircle.setMouseTransparent(true);

                    canvasArea.getChildren().addAll(boundingBox, antenaCircle, antenaLine, ne, nw, se, sw);

                }
                else {
                    diamond.setStroke(Color.BLACK);
                    diamond.setStrokeWidth(2);
                    canvasArea.getChildren().addAll(diamond);

                    if (node == hoveredNode) {
                        Polygon hoverDiamond = new Polygon();
                        hoverDiamond.getPoints().addAll(new Double[]{
                                node.getX() + node.getWidth() / 2, node.getY(),
                                node.getX() + node.getWidth(), node.getY() + node.getHeight() / 2,
                                node.getX() + node.getWidth() / 2, node.getY() + node.getHeight(),
                                node.getX(), node.getY() + node.getHeight() / 2
                        });
                        hoverDiamond.setFill(Color.TRANSPARENT);
                        hoverDiamond.setStroke(Color.LIMEGREEN);
                        hoverDiamond.setStrokeWidth(2);
                        hoverDiamond.getStrokeDashArray().addAll(5.0, 5.0);
                        hoverDiamond.getTransforms().add(pivot); // Rotație perfectă
                        hoverDiamond.setMouseTransparent(true);
                        canvasArea.getChildren().add(hoverDiamond);


                        double pSize = 5;
                        int segments = 4;

                        double[][] vertices = {
                                {node.getX() + node.getWidth() / 2, node.getY()}, // Sus
                                {node.getX() + node.getWidth(), node.getY() + node.getHeight() / 2}, // Dreapta
                                {node.getX() + node.getWidth() / 2, node.getY() + node.getHeight()}, // Jos
                                {node.getX(), node.getY() + node.getHeight() / 2} // Stânga
                        };

                        for (int i = 0; i < 4; i++) {
                            double startX = vertices[i][0];
                            double startY = vertices[i][1];
                            double endX = vertices[(i + 1) % 4][0];
                            double endY = vertices[(i + 1) % 4][1];

                            for (int j = 0; j < segments; j++) {
                                double t = (double) j / segments; // Procentul distanței (0%, 25%, 50%, 75%)
                                double px = startX + (endX - startX) * t;
                                double py = startY + (endY - startY) * t;

                                Rectangle anchor = new Rectangle(px - pSize / 2, py - pSize / 2, pSize, pSize);
                                anchor.setFill(Color.LIMEGREEN);
                                anchor.setStroke(Color.BLACK);
                                anchor.setStrokeWidth(1);

                                anchor.getTransforms().add(pivot);
                                anchor.setMouseTransparent(true);

                                canvasArea.getChildren().add(anchor);
                            }
                        }
                    }
                }

                if (node != editingNode)
                {
                    Text textNode = new Text(node.getText() != null ? node.getText() : "UML Node");
                    textNode.setFont(Font.font("Consolas", 12));
                    textNode.setTextAlignment(TextAlignment.CENTER);

                    double textW = textNode.getLayoutBounds().getWidth();
                    double textH = textNode.getLayoutBounds().getHeight();
                    textNode.setX(centerX - textW / 2);
                    textNode.setY(centerY - textH / 2 + 10);

                    textNode.getTransforms().add(pivot);
                    textNode.setMouseTransparent(true);
                    canvasArea.getChildren().add(textNode);
                }
            }

        }
    }

    private double[] getGlobalAnchor(FlowNode node, double pctX, double pctY) {

        double localX = node.getX() + pctX * node.getWidth();
        double localY = node.getY() + pctY * node.getHeight();

        double cx = node.getX() + node.getWidth() / 2;
        double cy = node.getY() + node.getHeight() / 2;
        double rad = Math.toRadians(node.getRotation());

        double dx = localX - cx;
        double dy = localY - cy;

        double globalX = cx + (dx * Math.cos(rad) - dy * Math.sin(rad));
        double globalY = cy + (dx * Math.sin(rad) + dy * Math.cos(rad));

        return new double[]{globalX, globalY};
    }
    public Pane getCanvasArea()
    {
        return this.canvasArea;
    }
    public Button getBtnSelect() { return btnSelect; }
    public Button getBtnRect() { return btnRect; }
    public Button getBtnDiam() { return btnDiam; }
    public FlowNode getEditingNode() { return editingNode; }

    private void drawConnection(com.designer.model.Connection c)
    {
        double[] start=getGlobalAnchor(c.getSource(),c.getSrcPctX(),c.getSrcPctY());
        double[] end=getGlobalAnchor(c.getTarget(),c.getTgtPctX(),c.getTgtPctY());

        double sx = start[0], sy = start[1];
        double ex = end[0], ey = end[1];

        Color color=c.isSelected() ? Color.DODGERBLUE : Color.BLACK;

        Line line=new Line(sx,sy,ex,ey);
        line.setStroke(color);
        line.setStrokeWidth(c.isSelected() ? 3 : 2);
        if(c.getLineStyle()== Connection.LineStyle.DASHED)
        {
            line.getStrokeDashArray().addAll(6.0,6.0);
        }
        canvasArea.getChildren().add(line);

        double angle=Math.atan2(ey-sy,ex-sx);
        drawEndPoint(ex,ey,angle,c.getTgtEndpointStyle(), color);
        drawEndPoint(sx,sy,angle+Math.PI,c.getSrcEndpointStyle(), color);
    }

    private void drawEndPoint(double x, double y, double angle, Connection.EndPointStyle style, Color color)
    {
        if(style==Connection.EndPointStyle.NONE)
            return;

        if (style == Connection.EndPointStyle.ARROW)
        {
            double arrowSize = 12;
            Polygon arrow = new Polygon();
            arrow.getPoints().addAll(new Double[]{
                    x, y,
                    x - arrowSize * Math.cos(angle - Math.PI / 6), y - arrowSize * Math.sin(angle - Math.PI / 6),
                    x - arrowSize * Math.cos(angle + Math.PI / 6), y - arrowSize * Math.sin(angle + Math.PI / 6)
            });
            arrow.setFill(color);
            canvasArea.getChildren().add(arrow);
        }
        else if (style == com.designer.model.Connection.EndPointStyle.AGGREGATION || style == com.designer.model.Connection.EndPointStyle.COMPOSITION)
        {

            double d = 10;
            Polygon diamond = new Polygon();
            diamond.getPoints().addAll(new Double[]{
                    x, y,
                    x - d * Math.cos(angle - Math.PI / 8), y - d * Math.sin(angle - Math.PI / 8),
                    x - 2 * d * Math.cos(angle), y - 2 * d * Math.sin(angle),
                    x - d * Math.cos(angle + Math.PI / 8), y - d * Math.sin(angle + Math.PI / 8)
            });
            diamond.setStroke(color);
            diamond.setStrokeWidth(2);
            diamond.setFill(style == com.designer.model.Connection.EndPointStyle.COMPOSITION ? color : Color.WHITE);
            canvasArea.getChildren().add(diamond);
        }
        else if (style == com.designer.model.Connection.EndPointStyle.CROW_FOOT)
        {
            double size = 15;
            double spread=8;

            double px=x-size*Math.cos(angle);
            double py=y-size*Math.sin(angle);

            Line l1 = new Line(px,py,x,y);

            double topX=x+spread*Math.cos(angle-Math.PI/2);
            double topY=y+spread*Math.sin(angle-Math.PI/2);
            Line l2 = new Line(topX,topY,px,py);

            double bottomX=x+spread*Math.cos(angle+Math.PI/2);
            double bottomY=y+spread*Math.sin(angle+Math.PI/2);
            Line l3 = new Line(bottomX,bottomY,px,py);

            l1.setStroke(color); l1.setStrokeWidth(2);
            l2.setStroke(color); l2.setStrokeWidth(2);
            l3.setStroke(color); l3.setStrokeWidth(2);
            canvasArea.getChildren().addAll(l1, l2, l3);
        }
    }

    public ComboBox<Connection.EndPointStyle> getSrcEndpointCombo() { return srcEndpointCombo; }
    public ComboBox<Connection.EndPointStyle> getTgtEndpointCombo() { return tgtEndpointCombo; }
    public ComboBox<Connection.LineStyle> getLineStyleCombo() { return lineStyleCombo; }

    public void showInlineEditor(FlowNode node) {
        this.editingNode = node;
        drawDiagram();

        TextArea editor = new TextArea(node.getText() != null ? node.getText() : "");
        editor.setFont(javafx.scene.text.Font.font("Consolas", 12));
        editor.setWrapText(true);

        editor.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-control-inner-background: transparent; " +
                        "-fx-text-fill: black; " +
                        "-fx-focus-color: transparent; " +
                        "-fx-faint-focus-color: transparent; " +
                        "-fx-border-color: #0078D7; " +
                        "-fx-border-width: 1.5; " +
                        "-fx-border-style: dashed; " +
                        "-fx-padding: 2;" // Padding mai mic ca să lase loc textului, evitând scroll-ul
        );

        double margin = 5;
        editor.setLayoutX(node.getX() + margin);
        editor.setLayoutY(node.getY() + margin);
        editor.setPrefWidth(node.getWidth() - margin * 2);
        editor.setPrefHeight(node.getHeight() - margin * 2);

        double localPivotX = editor.getPrefWidth() / 2;
        double localPivotY = editor.getPrefHeight() / 2;
        editor.getTransforms().add(new javafx.scene.transform.Rotate(node.getRotation(), localPivotX, localPivotY));

        editor.textProperty().addListener((obs, oldText, newText) -> {
            node.setText(newText);
        });

        editor.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                this.editingNode = null;
                canvasArea.getChildren().remove(editor);
                drawDiagram();
            }
        });

        canvasArea.getChildren().add(editor);

        javafx.application.Platform.runLater(() -> {
            javafx.scene.Node scrollPane = editor.lookup(".scroll-pane");
            if (scrollPane != null) {
                scrollPane.setStyle("-fx-hbar-policy: NEVER; -fx-vbar-policy: NEVER; -fx-background-color: transparent;");
            }
            editor.requestFocus();
            editor.positionCaret(editor.getText().length());
        });
    }

}