package martin.shapes.rendering;

import java.util.List;

/** After the repair (Replace Conditional with Polymorphism): a new kind of shape leaves the renderer untouched. */
public class Renderer {
    public void drawAllShapes(List<Shape> shapes) {
        for (Shape shape : shapes) {
            shape.draw();
        }
    }
}
