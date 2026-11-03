package martin.shapes.rendering;

import java.util.List;

import martin.shapes.shapes.Circle;
import martin.shapes.shapes.Square;

/** Martin's Shape example, the procedural version: a new kind of shape requires modifying drawAllShapes. */
public class Renderer {
    public void drawAllShapes(List<Object> shapes) {
        for (Object shape : shapes) {
            if (shape instanceof Circle) {
                drawCircle((Circle) shape);
            } else if (shape instanceof Square) {
                drawSquare((Square) shape);
            }
        }
    }

    private void drawCircle(Circle circle) {
        System.out.println("circle " + circle.radius);
    }

    private void drawSquare(Square square) {
        System.out.println("square " + square.side);
    }
}
