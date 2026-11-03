package martin.ccp.shapes;

/** CCP, the violating version: a user-interface class in the package of the shapes. */
public class ShapeRenderer {
    public void render(Circle circle) {
        System.out.println("circle of area " + circle.area());
    }

    public void render(Square square) {
        System.out.println("square of area " + square.area());
    }
}
