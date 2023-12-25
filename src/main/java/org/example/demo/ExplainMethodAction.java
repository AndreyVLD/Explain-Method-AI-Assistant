package org.example.demo;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.python.psi.PyFunction;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.openapi.ui.Messages;

import org.jetbrains.annotations.NotNull;
import java.util.Objects;


public class ExplainMethodAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ExplainMethodAction.class);
    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        try {
            Editor editor = anActionEvent.getRequiredData(CommonDataKeys.EDITOR);
            extractMethod(editor);
        }catch (Exception e){
           LOG.warn("An error occurred during action execution: ",e);
        }
    }

    private void extractMethod(Editor editor){
        try{
            PsiFile psiFile = getPsiFile(editor);
            SelectionModel selectionModel = editor.getSelectionModel();
            int start = selectionModel.getSelectionStart();
            PsiElement selectedElement = psiFile.findElementAt(start);

            PyFunction method =  PsiTreeUtil.getParentOfType(selectedElement, PyFunction.class);

            String explanation = getExplanation(method);

            Messages.showMessageDialog(explanation, "Method Explanation", Messages.getInformationIcon());

        }catch (Exception e){
            LOG.warn("An error occurred during method extraction: ",e);
        }

    }

    @NotNull
    private static String getExplanation(PyFunction method) {
        String explanation = "No method found";

        if(method != null) {
            explanation = ChatGPT.infer("Explain the method " + method.getText() + " in plain English.");
            if (explanation.equals("Error")) {
                explanation = "An error occurred during explanation generation";
                LOG.warn(explanation);
            }
        }
        return explanation;
    }

    private PsiFile getPsiFile(Editor editor) {
        Project project = Objects.requireNonNull(editor.getProject(), "Project cannot be null");
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        Objects.requireNonNull(psiFile, "PsiFile cannot be null");
        return psiFile;
    }

}
