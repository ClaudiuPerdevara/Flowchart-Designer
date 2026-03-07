package com.designer.model;

public abstract class FlowNode {

    private double x;
    private double y;
    private double width;
    private double height;
    private double rotation=0;
    private String text="UML node";

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

}
