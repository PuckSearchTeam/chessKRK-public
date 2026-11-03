package martin.srp.geometry;

import martin.srp.ui.Canvas;

/** Martin's SRP example: the rectangle both computes its area and draws itself. */
public class Rectangle {
    private final double width;
    private final double height;

    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public double area() {
        return width * height;
    }

    public void draw(Canvas canvas) {
        canvas.drawRectangle(width, height);
    }
}
