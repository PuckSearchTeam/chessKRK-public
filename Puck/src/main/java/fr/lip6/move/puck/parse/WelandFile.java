package fr.lip6.move.puck.parse;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;

public class WelandFile extends PsiFileBase {

    public WelandFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, Weland.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return WelandFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "Weland File";
    }

}