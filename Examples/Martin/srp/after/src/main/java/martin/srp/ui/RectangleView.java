package martin.srp.ui;

import martin.srp.geometry.Rectangle;

/** Drawing moved to the user-interface axis (Extract Class). */
public class RectangleView {
    public void draw(Rectangle rectangle, Canvas canvas) {
        canvas.drawRectangle(rectangle.width(), rectangle.height());
    }
}
