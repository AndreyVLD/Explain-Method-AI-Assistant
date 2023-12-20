package org.example.demo;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.python.psi.PyFunction;

import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class ExplainMethodAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Editor editor = anActionEvent.getRequiredData(CommonDataKeys.EDITOR);
        extractMethod(editor);
    }

    private void extractMethod(Editor editor){
        PsiFile psiFile = PsiDocumentManager.getInstance(
                Objects.requireNonNull(editor.getProject(),"Project cannot be null"))
                .getPsiFile(editor.getDocument());

        SelectionModel selectionModel = editor.getSelectionModel();
        int start = selectionModel.getSelectionStart();

        assert psiFile != null;
        PsiElement element = psiFile.findElementAt(start);


        assert element != null;
        PsiElement method = getMethod(element);

        if(method != null)
            System.out.println(method.getText());
        else
            System.out.println("No method found");
    }

    private PsiElement getMethod(PsiElement element){
        if(element == null){
            return null;
        }

        if(element instanceof PyFunction){
            return element;
        }else{
            return getMethod(element.getParent());
        }
    }
}
