package org.example.demo;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.jetbrains.python.psi.PyFunction;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.openapi.ui.Messages;

import com.jetbrains.python.psi.PyReferenceExpression;
import com.jetbrains.python.psi.impl.PyTargetExpressionImpl;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;


public class ExplainMethodAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ExplainMethodAction.class);

    /**
     * This method is called when the user clicks the "Explain Method" button
     * @param anActionEvent The event that is triggered when the user clicks the "Explain Method" button
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Editor editor = anActionEvent.getRequiredData(CommonDataKeys.EDITOR);
        try {
            // Run the action asynchronously to avoid blocking the UI thread
            CompletableFuture.runAsync(() -> explainMethod(editor))
                .exceptionally(e -> {
                    LOG.warn("An error occurred during action execution: ",e);
                    return null;
                 });
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
            getExplanationAsync(method).thenAccept(explanation -> {

                // Display the explanation to the user
                ApplicationManager.getApplication().invokeLater(() -> Messages
                        .showMessageDialog(explanation, "Method Explanation", Messages.getInformationIcon()));

            })
            .exceptionally(e -> {
                LOG.warn("An error occurred during explanation generation: ",e);
                return null;
            });

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
        // Create a mutable object to store the method in
        MutableObject<PyFunction> method = new MutableObject<>();

        // Run the method extraction in a read action to avoid threading issues
        ApplicationManager.getApplication().runReadAction(() -> {
            // Get the current file
            PsiFile psiFile = getPsiFile(editor);

            // The selected element is chosen to be the start of the selection or the caret position
            SelectionModel selectionModel = editor.getSelectionModel();
            int start = selectionModel.getSelectionStart();
            PsiElement selectedElement = psiFile.findElementAt(start);

            // Get the method that the selected element is in
            method.setValue(PsiTreeUtil.getParentOfType(selectedElement, PyFunction.class));
        });

        return method.getValue();
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


    /**
     * Gets the explanation for the method from ChatGPT asynchronously
     * @param method The method to get the explanation for as a PyFunction
     * @return The explanation for the method as a Future String
     */
    private CompletableFuture<String> getExplanationAsync(PyFunction method) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getExplanation(method);
            } catch (Exception e) {
                LOG.warn("An error occurred during explanation generation: ", e);
                return "An error occurred during explanation generation";
            }
        });
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
            String prompt = "Explain the method " + methodText + " in plain English. " +
                    "Ignore built-in Python methods from the external references.";
            LOG.warn("Prompt: \n" + prompt);

            // Send the following prompt to ChatGPT: "Explain the method <method text> in plain English."
            explanation = ChatGPT.infer(prompt);

            if (explanation.equals("Error")) {
                explanation = "An error occurred during explanation generation";
                LOG.warn(explanation);
            }
        }
        return explanation;
    }

    /**
     * Compresses and summarizes the method
     * @param method The method to compress and summarize as a PyFunction
     * @return The compressed and summarized method as a String
     */
    private String compressAndSummarizeFunction(@NotNull PyFunction method) {

        // Create a mutable object to store the compressed method in
        MutableObject<String> improvedContent = new MutableObject<>();
        MutableObject<Collection<String>> externalReferences = new MutableObject<>();

        // Run the compression and summarization in a read action to avoid threading issues
        ApplicationManager.getApplication().runReadAction(() -> {
            improvedContent.setValue(compressing(method.getText()));
            externalReferences.setValue(findExternalReferences(method).values());
        });
        return improvedContent.getValue() + "\nWe have the following external references:\n" + externalReferences.getValue();
    }

    /**
     * Compresses the method
     * @param method The method to compress as a PyFunction
     * @return The compressed method as a String
     */
    private String compressing(@NotNull String method) {

        // Remove comments and docstrings
        String compressedContent = method.replaceAll("#.*", "")
                .replaceAll("('''[\\s\\S]*?'''|\"\"\"[\\s\\S]*?\"\"\")", "");

        // Remove duplicate spaces between characters
        compressedContent = compressedContent.replaceAll("(?<=\\S) +(?=\\S)", " ");

        // Remove duplicate newlines
        compressedContent = compressedContent.replaceAll("(\\n\\s*\\n)+", "\n");

        // Remove spaces at the beginning and end of the string
        compressedContent = compressedContent.trim();

        return compressedContent;
    }

    /**
     * Finds the external references in the method
     * @param method The method to find the external references in as a PyFunction
     * @return The external references in the method as a Map of Strings,
     * where the key is the name of the reference and the value is the references text
     */
    public  Map<String,String> findExternalReferences(PyFunction method) {
        Map<String,String> externalReferences = new HashMap<>();

        // Find all references in the method
        method.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                // If the reference is not qualified, it is an external reference
                if (element instanceof PyReferenceExpression referenceExpression) {
                    if (referenceExpression.getQualifier() == null) {
                        // Get the name of the reference and the reference itself
                        String name = referenceExpression.getName();
                        PsiElement resolved = referenceExpression.getReference().resolve();

                        if (resolved != null && !PsiTreeUtil.isAncestor(method, resolved, false)
                                && !externalReferences.containsKey(name)) {

                            // For external variables, get the name , and value
                            if (resolved instanceof PyTargetExpressionImpl assignmentExpression) {
                                // Extract Value
                                String value = compressing(Objects
                                .requireNonNull(assignmentExpression.findAssignedValue()).getText());
                                externalReferences.put(name,  name + "=" + value);
                            } else {
                                String info = compressing(resolved.getText());
                                externalReferences.put(name, info);
                            }

                        }
                    }
                }
                super.visitElement(element);
            }
        });
        return externalReferences;
    }
}
