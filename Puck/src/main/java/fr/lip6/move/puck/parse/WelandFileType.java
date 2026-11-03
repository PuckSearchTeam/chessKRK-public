package fr.lip6.move.puck.parse;
import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class WelandFileType extends LanguageFileType {
    public static final WelandFileType INSTANCE = new WelandFileType();

    private WelandFileType() {
        super(Weland.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "Weland File";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Weland language file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "wld";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return WelandIcons.FILE;
    }
}
