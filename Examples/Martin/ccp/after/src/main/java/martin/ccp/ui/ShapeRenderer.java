package martin.ccp.ui;

import martin.ccp.shapes.Circle;
import martin.ccp.shapes.Square;

/** After the repair (Move Class): the renderer in a package of its own axis. */
public class ShapeRenderer {
    public void render(Circle circle) {
        System.out.println("circle of area " + circle.area());
    }

    public void render(Square square) {
        System.out.println("square of area " + square.area());
    }
}
