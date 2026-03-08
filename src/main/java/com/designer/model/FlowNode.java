package com.designer.model;

public abstract class FlowNode {

    private double x;
    private double y;
    private double width;
    private double height;
    private double rotation=0;
    private String text="UML node";

    private javafx.scene.paint.Color fillColor = javafx.scene.paint.Color.WHITE;
    private javafx.scene.paint.Color strokeColor = javafx.scene.paint.Color.BLACK;
    private double strokeWidth = 1.0;

    private int fontSize = 12;
    private boolean isBold = false;
    private boolean isItalic = false;
    private javafx.scene.paint.Color textColor = javafx.scene.paint.Color.BLACK;

    private boolean selected=false;

    public FlowNode(double x, double y, double width, double height, String text)
    {
        this.x=x;
        this.y=y;
        this.width=width;
        this.height=height;
        this.text=text;
    }

    public void setX(double x) { this.x=x;}
    public void setY(double y) { this.y=y;}
    public void setWidth(double width) { this.width=width;}
    public void setHeight(double height) { this.height=height;}
    public void setRotation(double rotation) { this.rotation=rotation;}
    public void setText(String text) { this.text=text;}

    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public double getRotation() { return rotation; }
    public String getText() { return text; }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected=selected; }

    public javafx.scene.paint.Color getFillColor() { return fillColor; }
    public void setFillColor(javafx.scene.paint.Color c) { this.fillColor = c; }

    public javafx.scene.paint.Color getStrokeColor() { return strokeColor; }
    public void setStrokeColor(javafx.scene.paint.Color c) { this.strokeColor = c; }

    public double getStrokeWidth() { return strokeWidth; }
    public void setStrokeWidth(double w) { this.strokeWidth = w; }

    public int getFontSize() { return fontSize; }
    public void setFontSize(int s) { this.fontSize = s; }

    public boolean isBold() { return isBold; }
    public void setBold(boolean b) { this.isBold = b; }

    public boolean isItalic() { return isItalic; }
    public void setItalic(boolean i) { this.isItalic = i; }

    public javafx.scene.paint.Color getTextColor() { return textColor; }
    public void setTextColor(javafx.scene.paint.Color c) { this.textColor = c; }

}
