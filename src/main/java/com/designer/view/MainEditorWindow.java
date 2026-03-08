package com.designer.view;

import com.designer.model.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.FontPosture;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Scale;

public class MainEditorWindow extends BorderPane {

    private DiagramModel model;
    private Pane canvasArea;
    private ScrollPane scrollPane;
    private Scale zoomScale;

    // --- BUTOANE UNELTE (Stânga) ---
    private Button btnSelect, btnRect, btnDiam;
    private ToggleButton btnGrid;
    private boolean showGrid = false;

    // --- ELEMENTE INSPECTOR (Dreapta) ---
    private VBox rightPanel;
    private VBox nodePropsPanel;
    private VBox connPropsPanel;
    private VBox defaultPropsPanel;

    // Setări Forme (Nodes)
    private ColorPicker fillColorPicker;
    private ColorPicker strokeColorPicker;
    private Spinner<Double> strokeWidthSpinner;
    private Spinner<Integer> fontSizeSpinner;
    private ToggleButton btnBold, btnItalic;
    private ColorPicker textColorPicker;

    // Setări Linii
    private ComboBox<Connection.EndPointStyle> srcEndpointCombo;
    private ComboBox<Connection.EndPointStyle> tgtEndpointCombo;
    private ComboBox<Connection.LineStyle> lineStyleCombo;
    private TextField srcMulField;
    private TextField tgtMulField;
    private ColorPicker lineColorPicker;
    private Spinner<Double> lineWidthSpinner;

    // Stări de desenare
    private FlowNode hoveredNode = null;
    private FlowNode editingNode = null;
    private Connection editingConnection = null;
    public boolean isConnecting = false;
    private double tempLineStartX, tempLineStartY, tempLineEndX, tempLineEndY;

    // Referință la obiectele curente selectate (ca să le modificăm din panel)
    private FlowNode selectedNodeForProps = null;
    private Connection selectedConnectionForProps = null;
    private boolean isUpdatingPropsUI = false;

    public MainEditorWindow(DiagramModel model) {
        this.model = model;

        this.setLeft(createLeftPanel());
        this.setCenter(createCenterPanel());
        this.setRight(createRightPanel());
        this.setTop(createTopToolbar());

        showDefaultProperties();
    }

    // ==========================================
    // 1. PANOUL DIN STÂNGA (Doar Iconițe)
    // ==========================================
    private Node createLeftPanel() {
        VBox leftBox = new VBox();
        leftBox.setPrefWidth(60);
        leftBox.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #dcdcdc; -fx-border-width: 0 1 0 0; -fx-padding: 10; -fx-spacing: 10; -fx-alignment: top-center;");

        btnSelect = new Button();
        Text arrow = new Text("↖");
        arrow.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        btnSelect.setGraphic(arrow);
        btnSelect.setPrefSize(40, 40);
        btnSelect.setTooltip(new Tooltip("Select Tool"));

        btnRect = new Button();
        Rectangle rIcon = new Rectangle(18, 14, Color.WHITE);
        rIcon.setStroke(Color.BLACK);
        rIcon.setStrokeWidth(1.5);
        btnRect.setGraphic(rIcon);
        btnRect.setPrefSize(40, 40);
        btnRect.setTooltip(new Tooltip("Rectangle"));

        btnDiam = new Button();
        Polygon dIcon = new Polygon(10, 0, 20, 10, 10, 20, 0, 10);
        dIcon.setFill(Color.WHITE);
        dIcon.setStroke(Color.BLACK);
        dIcon.setStrokeWidth(1.5);
        btnDiam.setGraphic(dIcon);
        btnDiam.setPrefSize(40, 40);
        btnDiam.setTooltip(new Tooltip("Diamond"));

        leftBox.getChildren().addAll(btnSelect, new Separator(), btnRect, btnDiam);
        return leftBox;
    }

    // ==========================================
    // 2. PANOUL CENTRAL (Canvas + Zoom)
    // ==========================================
    // ==========================================
    // 2. PANOUL CENTRAL (Canvas + Zoom)
    // ==========================================
    private Node createCenterPanel() {
        canvasArea = new Pane();
        canvasArea.setStyle("-fx-background-color: #e0e0e0;");
        canvasArea.setPrefSize(2000, 2000);
        canvasArea.setFocusTraversable(true);

        zoomScale = new Scale(1.0, 1.0, 0, 0);
        canvasArea.getTransforms().add(zoomScale);

        scrollPane = new ScrollPane(canvasArea);
        scrollPane.setPannable(false); // OPRIM conflictul cu Click-Stânga!
        scrollPane.setStyle("-fx-background-color: #c0c0c0;");

        // Zoom din rotița mouse-ului (Ctrl + Scroll)
        scrollPane.setOnScroll(event -> {
            if (event.isControlDown()) {
                event.consume();
                double zoomFactor = event.getDeltaY() > 0 ? 1.1 : 0.9;
                applyZoom(zoomFactor);
            }
        });

        // --- NAVIGARE CUSTOM (PAN) CU CLICK-DREAPTA SAU ROTIȚĂ APĂSATĂ ---
        final double[] dragContext = new double[2];

        scrollPane.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            if (e.isMiddleButtonDown() || e.isSecondaryButtonDown()) {
                dragContext[0] = e.getScreenX();
                dragContext[1] = e.getScreenY();
                canvasArea.setCursor(javafx.scene.Cursor.CLOSED_HAND);
                e.consume(); // Blocăm evenimentul ca să nu ajungă la unelte
            }
        });

        scrollPane.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, e -> {
            if (e.isMiddleButtonDown() || e.isSecondaryButtonDown()) {
                double deltaX = dragContext[0] - e.getScreenX();
                double deltaY = dragContext[1] - e.getScreenY();

                scrollPane.setHvalue(scrollPane.getHvalue() + deltaX * 1.5 / canvasArea.getWidth());
                scrollPane.setVvalue(scrollPane.getVvalue() + deltaY * 1.5 / canvasArea.getHeight());

                dragContext[0] = e.getScreenX();
                dragContext[1] = e.getScreenY();
                e.consume();
            }
        });

        scrollPane.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_RELEASED, e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.MIDDLE || e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                canvasArea.setCursor(javafx.scene.Cursor.DEFAULT);
                e.consume();
            }
        });

        return scrollPane;
    }
    private ToolBar createTopToolbar() {
        Button btnZoomIn = new Button("➕");
        Button btnZoomOut = new Button("➖");
        Button btnResetZoom = new Button("100%");
        btnGrid = new ToggleButton("▦ Grid");

        btnZoomIn.setOnAction(e -> applyZoom(1.1));
        btnZoomOut.setOnAction(e -> applyZoom(0.9));
        btnResetZoom.setOnAction(e -> {
            zoomScale.setX(1.0);
            zoomScale.setY(1.0);
        });

        btnGrid.setOnAction(e -> {
            this.showGrid = btnGrid.isSelected();
            drawDiagram();
        });

        return new ToolBar(btnZoomIn, btnZoomOut, btnResetZoom, new Separator(), btnGrid);
    }

    private void applyZoom(double factor) {
        double newScale = zoomScale.getX() * factor;
        if (newScale >= 0.2 && newScale <= 3.0) {
            zoomScale.setX(newScale);
            zoomScale.setY(newScale);
        }
    }

    // ==========================================
    // 3. PANOUL DIN DREAPTA (Properties Inspector)
    // ==========================================
    private Node createRightPanel() {
        rightPanel = new VBox();
        rightPanel.setPrefWidth(250);
        rightPanel.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #dcdcdc; -fx-border-width: 0 0 0 1;");

        createNodePropertiesPanel();
        createConnectionPropertiesPanel();
        createDefaultPropertiesPanel();

        return rightPanel;
    }

    private void createNodePropertiesPanel() {
        nodePropsPanel = new VBox(15);
        nodePropsPanel.setPadding(new Insets(15));

        Label title = new Label("Proprietăți Formă");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        fillColorPicker = new ColorPicker(Color.WHITE);
        strokeColorPicker = new ColorPicker(Color.BLACK);
        strokeWidthSpinner = new Spinner<>(0.5, 10.0, 1.0, 0.5);

        // Conectăm UI-ul la Model
        fillColorPicker.setOnAction(e -> { if (!isUpdatingPropsUI && selectedNodeForProps != null) { selectedNodeForProps.setFillColor(fillColorPicker.getValue()); drawDiagram(); } });
        strokeColorPicker.setOnAction(e -> { if (!isUpdatingPropsUI && selectedNodeForProps != null) { selectedNodeForProps.setStrokeColor(strokeColorPicker.getValue()); drawDiagram(); } });
        strokeWidthSpinner.valueProperty().addListener((obs, oldVal, newVal) -> { if (!isUpdatingPropsUI && selectedNodeForProps != null) { selectedNodeForProps.setStrokeWidth(newVal); drawDiagram(); } });

        VBox styleBox = new VBox(5,
                new Label("Background:"), fillColorPicker,
                new Label("Contur:"), new HBox(5, strokeColorPicker, strokeWidthSpinner)
        );

        fontSizeSpinner = new Spinner<>(8, 72, 12, 1);
        btnBold = new ToggleButton("B"); btnBold.setStyle("-fx-font-weight: bold;");
        btnItalic = new ToggleButton("I"); btnItalic.setStyle("-fx-font-style: italic;");
        textColorPicker = new ColorPicker(Color.BLACK);

        fontSizeSpinner.valueProperty().addListener((obs, oldVal, newVal) -> { if (!isUpdatingPropsUI && selectedNodeForProps != null) { selectedNodeForProps.setFontSize(newVal); drawDiagram(); } });
        btnBold.setOnAction(e -> { if (!isUpdatingPropsUI && selectedNodeForProps != null) { selectedNodeForProps.setBold(btnBold.isSelected()); drawDiagram(); } });
        btnItalic.setOnAction(e -> { if (!isUpdatingPropsUI && selectedNodeForProps != null) { selectedNodeForProps.setItalic(btnItalic.isSelected()); drawDiagram(); } });
        textColorPicker.setOnAction(e -> { if (!isUpdatingPropsUI && selectedNodeForProps != null) { selectedNodeForProps.setTextColor(textColorPicker.getValue()); drawDiagram(); } });


        HBox fontStyleBox = new HBox(5, fontSizeSpinner, btnBold, btnItalic);
        VBox textBox = new VBox(5,
                new Label("Setări Text:"), fontStyleBox, textColorPicker
        );

        nodePropsPanel.getChildren().addAll(title, new Separator(), styleBox, new Separator(), textBox);
    }

    private void createConnectionPropertiesPanel() {
        connPropsPanel = new VBox(15);
        connPropsPanel.setPadding(new Insets(15));

        Label title = new Label("Proprietăți Conexiune");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        lineStyleCombo = new ComboBox<>(); lineStyleCombo.getItems().addAll(Connection.LineStyle.values());
        srcEndpointCombo = new ComboBox<>(); srcEndpointCombo.getItems().addAll(Connection.EndPointStyle.values());
        tgtEndpointCombo = new ComboBox<>(); tgtEndpointCombo.getItems().addAll(Connection.EndPointStyle.values());

        lineColorPicker = new ColorPicker(Color.BLACK);
        lineWidthSpinner = new Spinner<>(0.5, 10.0, 2.0, 0.5);

        // Conectăm setările noi de linie
        lineColorPicker.setOnAction(e -> { if (!isUpdatingPropsUI && selectedConnectionForProps != null) { selectedConnectionForProps.setLineColor(lineColorPicker.getValue()); drawDiagram(); }});
        lineWidthSpinner.valueProperty().addListener((obs, oldVal, newVal) -> { if (!isUpdatingPropsUI && selectedConnectionForProps != null) { selectedConnectionForProps.setLineWidth(newVal); drawDiagram(); }});

        // Evenimente capete linie
        lineStyleCombo.setOnAction(e -> { if (!isUpdatingPropsUI && selectedConnectionForProps != null) { selectedConnectionForProps.setLineStyle(lineStyleCombo.getValue()); drawDiagram(); }});
        srcEndpointCombo.setOnAction(e -> { if (!isUpdatingPropsUI && selectedConnectionForProps != null) { selectedConnectionForProps.setSrcEndpointStyle(srcEndpointCombo.getValue()); drawDiagram(); }});
        tgtEndpointCombo.setOnAction(e -> { if (!isUpdatingPropsUI && selectedConnectionForProps != null) { selectedConnectionForProps.setTgtEndpointStyle(tgtEndpointCombo.getValue()); drawDiagram(); }});

        VBox styleBox = new VBox(5,
                new Label("Stil Linie:"), lineStyleCombo,
                new Label("Culoare:"), lineColorPicker,
                new Label("Grosime:"), lineWidthSpinner,
                new Label("Săgeți:"), new HBox(5, srcEndpointCombo, tgtEndpointCombo)
        );

        srcMulField = new TextField(); tgtMulField = new TextField();

        VBox textProps = new VBox(5,
                new Label("Texte Capete (Sursă/Destinație):"), new HBox(5, srcMulField, tgtMulField)
        );

        srcMulField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!isUpdatingPropsUI && selectedConnectionForProps != null) {
                selectedConnectionForProps.setSrcText(newVal);
                drawDiagram();
            }
        });
        tgtMulField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!isUpdatingPropsUI && selectedConnectionForProps != null) {
                selectedConnectionForProps.setTgtText(newVal);
                drawDiagram();
            }
        });

        connPropsPanel.getChildren().addAll(title, new Separator(), styleBox, new Separator(), textProps);
    }

    private void createDefaultPropertiesPanel() {
        defaultPropsPanel = new VBox(15);
        defaultPropsPanel.setPadding(new Insets(15));
        defaultPropsPanel.setAlignment(Pos.TOP_CENTER);

        Label lblEmpty = new Label("Selectează o formă de pe planșă.");
        lblEmpty.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");

        ColorPicker pageBgColor = new ColorPicker(Color.web("#e0e0e0"));
        pageBgColor.setOnAction(e -> canvasArea.setStyle("-fx-background-color: #" + pageBgColor.getValue().toString().substring(2, 8) + ";"));

        VBox pageBox = new VBox(5, new Label("Culoare Planșă:"), pageBgColor);
        pageBox.setAlignment(Pos.CENTER);

        defaultPropsPanel.getChildren().addAll(lblEmpty, new Separator(), pageBox);
    }

    // ==========================================
    // METODE DE SCHIMBARE A PANOULUI DIN DREAPTA
    // ==========================================
    public void showNodeProperties(FlowNode node) {
        this.selectedNodeForProps = node;
        this.selectedConnectionForProps = null;
        isUpdatingPropsUI = true;

        fillColorPicker.setValue(node.getFillColor());
        strokeColorPicker.setValue(node.getStrokeColor());
        strokeWidthSpinner.getValueFactory().setValue(node.getStrokeWidth());
        fontSizeSpinner.getValueFactory().setValue(node.getFontSize());
        btnBold.setSelected(node.isBold());
        btnItalic.setSelected(node.isItalic());
        textColorPicker.setValue(node.getTextColor());

        isUpdatingPropsUI = false;

        rightPanel.getChildren().clear();
        rightPanel.getChildren().add(nodePropsPanel);
    }

    public void showConnectionProperties(Connection conn) {
        this.selectedConnectionForProps = conn;
        this.selectedNodeForProps = null;
        isUpdatingPropsUI = true;

        srcEndpointCombo.setValue(conn.getSrcEndpointStyle());
        tgtEndpointCombo.setValue(conn.getTgtEndpointStyle());
        lineStyleCombo.setValue(conn.getLineStyle());
        srcMulField.setText(conn.getSrcText());
        tgtMulField.setText(conn.getTgtText());
        lineColorPicker.setValue(conn.getLineColor());
        lineWidthSpinner.getValueFactory().setValue(conn.getLineWidth());

        isUpdatingPropsUI = false;

        rightPanel.getChildren().clear();
        rightPanel.getChildren().add(connPropsPanel);
    }

    public void showDefaultProperties() {
        this.selectedNodeForProps = null;
        this.selectedConnectionForProps = null;
        rightPanel.getChildren().clear();
        rightPanel.getChildren().add(defaultPropsPanel);
    }

    // ==========================================
    // GETTERI PENTRU CONTROLLER
    // ==========================================
    public Pane getCanvasArea() { return this.canvasArea; }
    public Button getBtnSelect() { return btnSelect; }
    public Button getBtnRect() { return btnRect; }
    public Button getBtnDiam() { return btnDiam; }

    public FlowNode getHoveredNode() { return this.hoveredNode; }
    public FlowNode getEditingNode() { return editingNode; }

    public void setHoveredNode(FlowNode node) { this.hoveredNode = node; }
    public void setTempLine(boolean isConnecting, double sx, double sy, double ex, double ey) {
        this.isConnecting = isConnecting;
        this.tempLineStartX = sx;
        this.tempLineStartY = sy;
        this.tempLineEndX = ex;
        this.tempLineEndY = ey;
    }

    // ==========================================
    // LOGICA DE DESENARE
    // ==========================================

    public void drawDiagram() {
        // --- 1. MĂRIRE AUTOMATĂ A PLANȘEI ---
        double requiredWidth = 2000;
        double requiredHeight = 2000;
        for (FlowNode n : model.getNodes()) {
            if (n.getX() + n.getWidth() + 300 > requiredWidth) requiredWidth = n.getX() + n.getWidth() + 300;
            if (n.getY() + n.getHeight() + 300 > requiredHeight) requiredHeight = n.getY() + n.getHeight() + 300;
        }
        canvasArea.setPrefSize(requiredWidth, requiredHeight);

        canvasArea.getChildren().clear();

        // --- 2. DESENARE GRID ---
        if (showGrid) {
            for (int i = 0; i < requiredWidth; i += 20) {
                Line l = new Line(i, 0, i, requiredHeight); l.setStroke(Color.LIGHTGRAY); l.setStrokeWidth(0.5);
                canvasArea.getChildren().add(l);
            }
            for (int i = 0; i < requiredHeight; i += 20) {
                Line l = new Line(0, i, requiredWidth, i); l.setStroke(Color.LIGHTGRAY); l.setStrokeWidth(0.5);
                canvasArea.getChildren().add(l);
            }
        }

        // --- 3. DESENARE LINII ---
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

        // --- 4. DESENARE FORME ---
        for( FlowNode node : model.getNodes() ) {
            double centerX = node.getX() + node.getWidth() / 2;
            double centerY = node.getY() + node.getHeight() / 2;
            javafx.scene.transform.Rotate pivot = new javafx.scene.transform.Rotate(node.getRotation(), centerX, centerY);

            if (node instanceof RectangleNode) {
                Rectangle rect = new Rectangle(node.getX(), node.getY(), node.getWidth(), node.getHeight());

                // Aplicăm culorile din model
                rect.setFill(node.getFillColor());
                rect.setMouseTransparent(true);
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
                    rect.setStroke(node.getStrokeColor());
                    rect.setStrokeWidth(node.getStrokeWidth());
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

                        for (int i = 0; i < 4; i++) {
                            double startX = vertices[i][0];
                            double startY = vertices[i][1];
                            double endX = vertices[(i + 1) % 4][0];
                            double endY = vertices[(i + 1) % 4][1];

                            for (int j = 0; j < segments; j++) {
                                double t = (double) j / segments;
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
            }

            if (node instanceof DiamondNode) {
                Polygon diamond = new Polygon();
                diamond.getPoints().addAll(new Double[]{
                        node.getX() + node.getWidth() / 2, node.getY(),
                        node.getX() + node.getWidth(), node.getY() + node.getHeight() / 2,
                        node.getX() + node.getWidth() / 2, node.getY() + node.getHeight(),
                        node.getX(), node.getY() + node.getHeight() / 2
                });

                diamond.setFill(node.getFillColor());
                diamond.setMouseTransparent(true);

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

                } else {
                    diamond.setStroke(node.getStrokeColor());
                    diamond.setStrokeWidth(node.getStrokeWidth());
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
                        hoverDiamond.getTransforms().add(pivot);
                        hoverDiamond.setMouseTransparent(true);
                        canvasArea.getChildren().add(hoverDiamond);

                        double pSize = 5;
                        int segments = 4;

                        double[][] vertices = {
                                {node.getX() + node.getWidth() / 2, node.getY()},
                                {node.getX() + node.getWidth(), node.getY() + node.getHeight() / 2},
                                {node.getX() + node.getWidth() / 2, node.getY() + node.getHeight()},
                                {node.getX(), node.getY() + node.getHeight() / 2}
                        };

                        for (int i = 0; i < 4; i++) {
                            double startX = vertices[i][0];
                            double startY = vertices[i][1];
                            double endX = vertices[(i + 1) % 4][0];
                            double endY = vertices[(i + 1) % 4][1];

                            for (int j = 0; j < segments; j++) {
                                double t = (double) j / segments;
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
            }

            // --- APLICARE SETĂRI TEXT ---
            if (node != editingNode) {
                Text textNode = new Text(node.getText() != null ? node.getText() : "UML Node");

                FontWeight fw = node.isBold() ? FontWeight.BOLD : FontWeight.NORMAL;
                FontPosture fp = node.isItalic() ? FontPosture.ITALIC : FontPosture.REGULAR;
                textNode.setFont(Font.font("Consolas", fw, fp, node.getFontSize()));
                textNode.setFill(node.getTextColor());

                textNode.setTextAlignment(TextAlignment.CENTER);

                double textW = textNode.getLayoutBounds().getWidth();
                double textH = textNode.getLayoutBounds().getHeight();
                textNode.setX(centerX - textW / 2);

                // Micul offset pentru a centra vertical
                textNode.setY(centerY - textH / 2 + (node.getFontSize() * 0.8));

                textNode.getTransforms().add(pivot);
                textNode.setMouseTransparent(true);
                canvasArea.getChildren().add(textNode);
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

    private void drawConnection(com.designer.model.Connection c) {
        double[] start = getGlobalAnchor(c.getSource(), c.getSrcPctX(), c.getSrcPctY());
        double[] end = getGlobalAnchor(c.getTarget(), c.getTgtPctX(), c.getTgtPctY());

        double sx = start[0], sy = start[1];
        double ex = end[0], ey = end[1];

        // Aplicăm culoarea și grosimea din modelul Connection
        Color color = c.isSelected() ? Color.DODGERBLUE : c.getLineColor();

        Line line = new Line(sx, sy, ex, ey);
        line.setStroke(color);
        line.setStrokeWidth(c.isSelected() ? c.getLineWidth() + 1 : c.getLineWidth());
        if (c.getLineStyle() == Connection.LineStyle.DASHED) {
            line.getStrokeDashArray().addAll(6.0, 6.0);
        }
        canvasArea.getChildren().add(line);

        double angle = Math.atan2(ey - sy, ex - sx);
        drawEndPoint(ex, ey, angle, c.getTgtEndpointStyle(), color);
        drawEndPoint(sx, sy, angle + Math.PI, c.getSrcEndpointStyle(), color);

        double angleDeg = Math.toDegrees(angle);
        if (angleDeg > 90) angleDeg -= 180;
        else if (angleDeg < -90) angleDeg += 180;

        double midX = (sx + ex) / 2;
        double midY = (sy + ey) / 2;

        drawConnectionLabel(c.getName(), midX + c.getNameOffX(), midY + c.getNameOffY(), angleDeg, c.isSelected(), true);

        double srcPosX = sx + (ex - sx) * 0.15;
        double srcPosY = sy + (ey - sy) * 0.15;
        drawConnectionLabel(c.getSrcText(), srcPosX, srcPosY, angleDeg, c.isSelected(), false);

        double tgtPosX = sx + (ex - sx) * 0.85;
        double tgtPosY = sy + (ey - sy) * 0.85;
        drawConnectionLabel(c.getTgtText(), tgtPosX, tgtPosY, angleDeg, c.isSelected(), false);
    }

    private void drawConnectionLabel(String text, double x, double y, double angleDeg, boolean isSelected, boolean hasInteractiveDot) {
        if (text == null || text.trim().isEmpty()) return;

        Text textMeasurer = new Text(text);
        textMeasurer.setFont(javafx.scene.text.Font.font("Consolas", 12));
        double exactTextW = textMeasurer.getLayoutBounds().getWidth();
        double exactTextH = textMeasurer.getLayoutBounds().getHeight();

        double finalW = exactTextW + 8;
        double finalH = exactTextH + 4;

        javafx.scene.control.Label label = new javafx.scene.control.Label(text);
        label.setFont(javafx.scene.text.Font.font("Consolas", 12));
        label.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: black; -fx-padding: 2 4 2 4;");

        label.setMinWidth(finalW);
        label.setPrefWidth(finalW);
        label.setMinHeight(finalH);
        label.setPrefHeight(finalH);
        label.setAlignment(javafx.geometry.Pos.CENTER);

        label.setLayoutX(x - finalW / 2);
        label.setLayoutY(y - finalH / 2);

        label.getTransforms().add(new javafx.scene.transform.Rotate(angleDeg, finalW / 2, finalH / 2));
        label.setMouseTransparent(true);

        canvasArea.getChildren().add(label);

        if (isSelected && hasInteractiveDot) {
            double dotOffset = finalH / 2 + 6;

            javafx.scene.shape.Circle orangeDot = new javafx.scene.shape.Circle(x, y - dotOffset, 4.5);
            orangeDot.setFill(javafx.scene.paint.Color.ORANGE);
            orangeDot.setStroke(javafx.scene.paint.Color.BLACK);

            orangeDot.getTransforms().add(new javafx.scene.transform.Rotate(angleDeg, x, y));
            orangeDot.setMouseTransparent(true);

            canvasArea.getChildren().add(orangeDot);
        }
    }

    private void drawEndPoint(double x, double y, double angle, Connection.EndPointStyle style, Color color) {
        if (style == Connection.EndPointStyle.NONE) return;

        if (style == Connection.EndPointStyle.ARROW) {
            double arrowSize = 12;
            Polygon arrow = new Polygon();
            arrow.getPoints().addAll(new Double[]{
                    x, y,
                    x - arrowSize * Math.cos(angle - Math.PI / 6), y - arrowSize * Math.sin(angle - Math.PI / 6),
                    x - arrowSize * Math.cos(angle + Math.PI / 6), y - arrowSize * Math.sin(angle + Math.PI / 6)
            });
            arrow.setFill(color);
            canvasArea.getChildren().add(arrow);
        } else if (style == com.designer.model.Connection.EndPointStyle.AGGREGATION || style == com.designer.model.Connection.EndPointStyle.COMPOSITION) {
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
        } else if (style == com.designer.model.Connection.EndPointStyle.CROW_FOOT) {
            double size = 15;
            double spread = 8;

            double px = x - size * Math.cos(angle);
            double py = y - size * Math.sin(angle);

            Line l1 = new Line(px, py, x, y);

            double topX = x + spread * Math.cos(angle - Math.PI / 2);
            double topY = y + spread * Math.sin(angle - Math.PI / 2);
            Line l2 = new Line(topX, topY, px, py);

            double bottomX = x + spread * Math.cos(angle + Math.PI / 2);
            double bottomY = y + spread * Math.sin(angle + Math.PI / 2);
            Line l3 = new Line(bottomX, bottomY, px, py);

            l1.setStroke(color); l1.setStrokeWidth(2);
            l2.setStroke(color); l2.setStrokeWidth(2);
            l3.setStroke(color); l3.setStrokeWidth(2);
            canvasArea.getChildren().addAll(l1, l2, l3);
        }
    }

    public void showInlineEditor(FlowNode node) {
        this.editingNode = node;
        drawDiagram();

        TextArea editor = new TextArea(node.getText() != null ? node.getText() : "");

        FontWeight fw = node.isBold() ? FontWeight.BOLD : FontWeight.NORMAL;
        FontPosture fp = node.isItalic() ? FontPosture.ITALIC : FontPosture.REGULAR;
        editor.setFont(Font.font("Consolas", fw, fp, node.getFontSize()));

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
                        "-fx-padding: 2;"
        );

        double margin = 5;
        final int MAX_CHARS = 200;
        editor.setLayoutX(node.getX() + margin);
        editor.setLayoutY(node.getY() + margin);
        editor.setPrefWidth(node.getWidth() - margin * 2);
        editor.setPrefHeight(node.getHeight() - margin * 2);

        double localPivotX = editor.getPrefWidth() / 2;
        double localPivotY = editor.getPrefHeight() / 2;
        editor.getTransforms().add(new javafx.scene.transform.Rotate(node.getRotation(), localPivotX, localPivotY));

        editor.textProperty().addListener((obs, oldText, newText) -> {
            if (newText.length() > MAX_CHARS) {
                editor.setText(oldText);
                return;
            }

            node.setText(newText);

            javafx.scene.Node sp = editor.lookup(".scroll-pane");
            if (sp instanceof javafx.scene.control.ScrollPane) {
                ((javafx.scene.control.ScrollPane) sp).setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
                ((javafx.scene.control.ScrollPane) sp).setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
            }
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

    public void showConnectionInlineEditor(com.designer.model.Connection c) {
        this.editingConnection = c;

        long editorOpenTime = System.currentTimeMillis();
        drawDiagram();

        javafx.scene.control.TextArea editor = new javafx.scene.control.TextArea(c.getName() != null ? c.getName() : "");
        editor.setFont(javafx.scene.text.Font.font("Consolas", 12));
        editor.setWrapText(true);

        editor.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-control-inner-background: white; " +
                        "-fx-text-fill: black; " +
                        "-fx-focus-color: transparent; " +
                        "-fx-faint-focus-color: transparent; " +
                        "-fx-border-color: #0078D7; " +
                        "-fx-border-width: 1.5; " +
                        "-fx-border-style: dashed; " +
                        "-fx-padding: 2;"
        );

        double[] start = getGlobalAnchor(c.getSource(), c.getSrcPctX(), c.getSrcPctY());
        double[] end = getGlobalAnchor(c.getTarget(), c.getTgtPctX(), c.getTgtPctY());
        double midX = (start[0] + end[0]) / 2;
        double midY = (start[1] + end[1]) / 2;

        double finalX = midX + c.getNameOffX();
        double finalY = midY + c.getNameOffY();

        double width = 100;
        double height = 40;

        editor.setLayoutX(finalX - width / 2);
        editor.setLayoutY(finalY - height / 2);
        editor.setPrefWidth(width);
        editor.setPrefHeight(height);

        double angle = Math.atan2(end[1] - start[1], end[0] - start[0]);
        double angleDeg = Math.toDegrees(angle);
        if (angleDeg > 90) angleDeg -= 180;
        else if (angleDeg < -90) angleDeg += 180;

        editor.getTransforms().add(new javafx.scene.transform.Rotate(angleDeg, width / 2, height / 2));

        final int MAX_CHARS = 30;

        editor.textProperty().addListener((obs, oldText, newText) -> {
            if (newText.length() > MAX_CHARS) {
                editor.setText(oldText);
                return;
            }

            c.setName(newText);

            javafx.scene.Node sp = editor.lookup(".scroll-pane");
            if (sp instanceof javafx.scene.control.ScrollPane) {
                ((javafx.scene.control.ScrollPane) sp).setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
                ((javafx.scene.control.ScrollPane) sp).setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
            }
        });

        editor.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                if (System.currentTimeMillis() - editorOpenTime < 300) {
                    javafx.application.Platform.runLater(editor::requestFocus);
                    return;
                }
                this.editingConnection = null;
                canvasArea.getChildren().remove(editor);
                drawDiagram();
            }
        });

        canvasArea.getChildren().add(editor);

        javafx.application.Platform.runLater(() -> {
            editor.requestFocus();
            editor.positionCaret(editor.getText().length());
        });
    }
}