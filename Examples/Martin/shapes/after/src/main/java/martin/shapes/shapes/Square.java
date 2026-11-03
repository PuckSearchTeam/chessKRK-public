package martin.shapes.shapes;

import martin.shapes.rendering.Shape;

public class Square implements Shape {
    public final double side;

    public Square(double side) {
        this.side = side;
    }

    @Override
    public void draw() {
        System.out.println("square " + side);
    }
}
