package martin.ccp.shapes;

public class Square {
    public final double side;

    public Square(double side) {
        this.side = side;
    }

    public double area() {
        return side * side;
    }
}
