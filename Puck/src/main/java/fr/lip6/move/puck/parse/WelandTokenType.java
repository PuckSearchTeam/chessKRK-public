package fr.lip6.move.puck.parse;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class WelandTokenType extends IElementType {

    public WelandTokenType(@NotNull @NonNls String debugName) {
        super(debugName, Weland.INSTANCE);
    }

    @Override
    public String toString() {
        return "WelandTokenType." + super.toString();
    }

}