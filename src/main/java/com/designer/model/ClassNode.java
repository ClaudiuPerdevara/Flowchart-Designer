package com.designer.model;

public class ClassNode extends FlowNode {

    private String attributesText;
    private String methodsText;

    private double minWidth = 100;
    private double minHeight = 100;

    public ClassNode(double x, double y, double w, double h, String t) {
        super(x, y, w, h, "NumeClasa");
        this.attributesText = "+ atribut1 : int\n+ atribut2 : String";
        this.methodsText = "+ metoda1()\n+ metoda2()";
    }

    public String getAttributesText() { return attributesText; }
    public void setAttributesText(String attributesText) { this.attributesText = attributesText; }

    public String getMethodsText() { return methodsText; }
    public void setMethodsText(String methodsText) { this.methodsText = methodsText; }

    public double getMinWidth() { return minWidth; }
    public void setMinWidth(double minWidth) { this.minWidth = minWidth; }

    public double getMinHeight() { return minHeight; }
    public void setMinHeight(double minHeight) { this.minHeight = minHeight; }
}