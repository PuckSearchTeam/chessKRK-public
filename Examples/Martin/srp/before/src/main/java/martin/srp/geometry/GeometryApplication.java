package martin.srp.geometry;

/** The computational geometry application: needs the area only. */
public class GeometryApplication {
    public double totalArea(Rectangle... rectangles) {
        double total = 0;
        for (Rectangle r : rectangles) {
            total += r.area();
        }
        return total;
    }
}
