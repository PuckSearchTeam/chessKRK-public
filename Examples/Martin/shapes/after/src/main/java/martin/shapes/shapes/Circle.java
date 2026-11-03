package martin.shapes.shapes;

import martin.shapes.rendering.Shape;

public class Circle implements Shape {
    public final double radius;

    public Circle(double radius) {
        this.radius = radius;
    }

    @Override
    public void draw() {
        System.out.println("circle " + radius);
    }
}
