package martin.srp.geometry;

/** After the repair: the rectangle is geometry only. */
public class Rectangle {
    private final double width;
    private final double height;

    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }

    public double area() {
        return width * height;
    }
}
