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

    /**
     * This method is called when the user clicks the "Explain Method" button
     * @param anActionEvent The event that is triggered when the user clicks the "Explain Method" button
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        try {
            Editor editor = anActionEvent.getRequiredData(CommonDataKeys.EDITOR);
            explainMethod(editor);
        }catch (Exception e){
           LOG.warn("An error occurred during action execution: ",e);
        }
    }

    /**
     * Extracts the method that the user has selected and displays an explanation for it using ChatGPT
     * @param editor The editor that the user is currently using
     */
    private void explainMethod(Editor editor){
        try{
            // Get the method that the user has selected
            PyFunction method = extractMethod(editor);

            // Get the explanation for the method from ChatGPT
            String explanation = getExplanation(method);

            // Display the explanation to the user
            Messages.showMessageDialog(explanation, "Method Explanation", Messages.getInformationIcon());

        }catch (Exception e){
            LOG.warn("An error occurred during method extraction: ",e);
        }

    }

    /**
     * Extracts the method that the user has selected
     * @param editor The editor that the user is currently using
     * @return The method that the user has selected as a PyFunction
     */
    private PyFunction extractMethod(Editor editor) {
        // Get the current file
        PsiFile psiFile = getPsiFile(editor);

        // The selected element is chosen to be the start of the selection or the caret position
        SelectionModel selectionModel = editor.getSelectionModel();
        int start = selectionModel.getSelectionStart();
        PsiElement selectedElement = psiFile.findElementAt(start);

        // Get the method that the selected element is in
        return PsiTreeUtil.getParentOfType(selectedElement, PyFunction.class);
    }

    /**
     * Gets the explanation for the method from ChatGPT
     * @param method The method to get the explanation for as a PyFunction
     * @return The explanation for the method as a String
     */
    @NotNull
    private  String getExplanation(PyFunction method) {
        String explanation = "No method found";

        if(method != null) {
            String methodText = compressAndSummarizeFunction(method);

            // Send the following prompt to ChatGPT: "Explain the method <method text> in plain English."
            // explanation = ChatGPT.infer("Explain the method " + methodText + " in plain English.");
            if (explanation.equals("Error")) {
                explanation = "An error occurred during explanation generation";
                LOG.warn(explanation);
            }
        }
        return explanation;
    }

    /**
     * Gets the current file that the user is working on
     * @param editor The editor that the user is currently using
     * @return The current file that the user is working on as a PsiFile
     */
    private PsiFile getPsiFile(Editor editor) {
        Project project = Objects.requireNonNull(editor.getProject(), "Project cannot be null");
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        Objects.requireNonNull(psiFile, "PsiFile cannot be null");
        return psiFile;
    }

    private String compressAndSummarizeFunction(@NotNull PyFunction method) {
        String compressedContent = compressing(method);

        return compressedContent;
    }

    private String compressing(@NotNull PyFunction method) {

        // Remove comments and docstrings
        String compressedContent = method.getText().replaceAll("#.*", "")
                .replaceAll("('''[\\s\\S]*?'''|\"\"\"[\\s\\S]*?\"\"\")", "");

        // Remove duplicate spaces between characters
        compressedContent = compressedContent.replaceAll("(?<=\\S) +(?=\\S)", " ");

        // Remove duplicate newlines
        compressedContent = compressedContent.replaceAll("(\\n\\s*\\n)+", "\n");

        // Remove spaces at the beginning and end of the string
        compressedContent = compressedContent.trim();

        System.out.println(compressedContent);

        return compressedContent;
    }
}
