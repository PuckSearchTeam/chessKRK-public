package fr.lip6.move.puck.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class DecouplePermutationsAction extends AnAction {
    private static Project project = null;

    public void actionPerformed(AnActionEvent e) {
        project = e.getProject();
        if (project != null) {
            Set<String> sets = new HashSet<>();
            String projectPath = project.getBasePath();
            String filename = projectPath + "/decouple.wld";
            Boolean fileEndsWithNewLine = false;
            String lastLine = "";
            //collect sets
           try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
                String line;
               while ((line = reader.readLine()) != null) {
                   lastLine = line;
                   if (line.startsWith("set")) {
                       // get set name
                       int startIndex = line.indexOf(' ') + 1; // after "set"
                       int endIndex = line.indexOf('='); // before "="
                       if (startIndex < endIndex) {
                           String setName = line.substring(startIndex, endIndex).trim();
                           sets.add(setName);
                       }
                   }
               }            } catch (IOException ex) {
                ex.printStackTrace();
            }
           if (lastLine.endsWith("\n")) {
               fileEndsWithNewLine = true;
           }
            // generate permutations and add them at the end of the file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true))) {
                if (!fileEndsWithNewLine) {
                    writer.newLine();
                }
                for (String x : sets) {
                    for (String y : sets) {
                        if (!x.equals(y)) {
                            writer.write(String.format("hide %s from %s;", x, y));
                            writer.newLine();
                        }
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
       }
    }
}
