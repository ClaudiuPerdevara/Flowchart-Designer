package com.designer.model;

public class FlowNode {

    private double x;
    private double y;
    private double width;
    private double height;
    private String text;

    public FlowNode(double x, double y, double width, double height, String text)
    {
        this.x=x;
        this.y=y;
        this.width=width;
        this.height=height;
        this.text=text;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public String getText() { return text; }

}
