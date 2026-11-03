package fr.lip6.move.puck.parse;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class WelandElementType extends IElementType {

    public WelandElementType(@NotNull @NonNls String debugName) {
        super(debugName, Weland.INSTANCE);
    }
}