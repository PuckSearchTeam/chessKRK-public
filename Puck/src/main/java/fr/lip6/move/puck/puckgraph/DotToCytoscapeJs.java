package fr.lip6.move.puck.puckgraph;
import java.io.*;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
// class DotToCytoscapeJs is a utility class that converts DOT files to Cytoscape.js JSON format and generates a web page to visualize the graphs.
public class DotToCytoscapeJs {

    record JsonGraph(JsonArray nodes, JsonArray edges) {
        JsonArray getNodes() {
            return nodes;
        }
        JsonArray getEdges() {
            return edges;
        }
    }

    static BufferedWriter graphsList;
    static String lastJsonLine = "";

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java DotToCytoscapeJs projectBasePath");
            return;
        }

        String projectBasePath = args[0];
        String cyDirPath = projectBasePath + "/cytoscapejs";
        String jsonDirPath = cyDirPath + "/data";
        String graphsListFile = jsonDirPath + "/" +  "graphs-list.json";

        try {
            Files.createDirectories(Paths.get(jsonDirPath));
            populateCyDir(cyDirPath);
            graphsList = new BufferedWriter(new FileWriter(graphsListFile));
            graphsList.write("{");
            graphsList.newLine();
            graphsList.write("  \"graphs\": [");
            graphsList.newLine();
            Files.walkFileTree(Paths.get(projectBasePath), EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE,
                    new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            if (file.toString().endsWith(".dot")) {
                                System.out.println("Processing file: " + file);
                                processDotFile(file, jsonDirPath);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
            graphsList.write("  ]");
            graphsList.newLine();
            graphsList.write("}");
            graphsList.newLine();
            graphsList.close();
            String newJsonLastLine = lastJsonLine.replace(',', ' ');
            List<String> jsonlines = Files.readAllLines(Paths.get(graphsListFile));
            for (int i = 0; i < jsonlines.size(); i++) {
                if (jsonlines.get(i).contains(lastJsonLine)) {
                    jsonlines.set(i, newJsonLastLine);
                    break;  // only one line to change
                }
            }
            BufferedWriter gwriter = new BufferedWriter(new FileWriter(graphsListFile));
            for (String line : jsonlines) {
                gwriter.write(line);
                gwriter.newLine();
            }
            gwriter.close();
            createServerScripts(projectBasePath, cyDirPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void populateCyDir(String cyDirPath) throws IOException {
        String jsDirPath = cyDirPath + "/js";
        String cssDirPath = cyDirPath + "/css";
        String htmlFile = cyDirPath + "/index.html";

        // Create directories if they do not exist
        Files.createDirectories(Paths.get(jsDirPath));
        Files.createDirectories(Paths.get(cssDirPath));

        try {
            BufferedWriter indexhtml = new BufferedWriter(new FileWriter(htmlFile));
            String indexcontent = "<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                    "    <title>Graphes avec Cytoscape.js</title>\n" +
                    "    <link rel=\"stylesheet\" href=\"css/styles.css\">\n" +
                    "    <link rel=\"icon\" href=\"data:,\">\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <div class=\"container\">\n" +
                    "        <div class=\"controls\">\n" +
                    "            <label for=\"graph-selector\">Select Graph:</label>\n" +
                    "            <select id=\"graph-selector\">\n" +
                    "                <!-- Options will be populated by JavaScript -->\n" +
                    "            </select>\n" +
                    "            <label for=\"layout-selector\">Select Layout:</label>\n" +
                    "            <select id=\"layout-selector\">\n" +
                    "                <option value=\"dagre\">Dagre</option>\n" +
                    "                <option value=\"breadthfirst\">Breadthfirst</option>\n" +
                    "                <option value=\"circle\">Circle</option>\n" +
                    "                <option value=\"cose\">Cose</option>\n" +
                    "                <option value=\"grid\">Grid</option>\n" +
                    "                <option value=\"concentric\">Concentric</option>\n" +
                    "                <option value=\"random\">Random</option>\n" +
                    "            </select>\n" +
                    "        </div>\n" +
                    "        <div id=\"cy\"></div>\n" +
                    "    </div>\n" +
                    "    <script src=\"https://cdnjs.cloudflare.com/ajax/libs/cytoscape/3.19.1/cytoscape.min.js\"></script>\n" +
                    "    <script src=\"https://unpkg.com/dagre@0.8.5/dist/dagre.min.js\"></script>\n" +
                    "    <script src=\"https://unpkg.com/cytoscape-dagre@2.2.2/cytoscape-dagre.js\"></script>\n" +
                    "    <script src=\"js/cytoscape-styles.js\"></script>\n" +
                    "    <script src=\"js/main.js\"></script>\n" +
                    "</body>\n" +
                    "</html>";
            indexhtml.write(indexcontent);
            indexhtml.close();

            BufferedWriter mainjs = new BufferedWriter(new FileWriter(jsDirPath + "/main.js"));
            String mainjscontent = "document.addEventListener(\"DOMContentLoaded\", function() {\n" +
                    "    cytoscape.use(cytoscapeDagre); // Utiliser le layout dagre\n\n" +
                    "    const selector = document.getElementById('graph-selector');\n" +
                    "    const layoutSelector = document.getElementById('layout-selector');\n" +
                    "    let cy; // Store the cytoscape instance\n\n" +
                    "    // Charger la liste des fichiers graphiques\n" +
                    "    fetch('data/graphs-list.json')\n" +
                    "        .then(response => response.json())\n" +
                    "        .then(data => {\n" +
                    "            const graphFiles = data.graphs;\n\n" +
                    "            // Populer le menu déroulant\n" +
                    "            graphFiles.forEach(file => {\n" +
                    "                const option = document.createElement('option');\n" +
                    "                option.value = file;\n" +
                    "                option.text = file;\n" +
                    "                selector.appendChild(option);\n" +
                    "            });\n\n" +
                    "            // Charger le premier fichier par défaut\n" +
                    "            loadGraph(graphFiles[0]);\n\n" +
                    "            // Ajouter un écouteur d'événement pour le changement de sélection\n" +
                    "            selector.addEventListener('change', function() {\n" +
                    "                const selectedFile = this.value;\n" +
                    "                loadGraph(selectedFile);\n" +
                    "            });\n" +
                    "        })\n" +
                    "        .catch(error => console.error('Error loading graph list:', error));\n\n" +
                    "    // Fonction pour charger le graphe\n" +
                    "    function loadGraph(fileName) {\n" +
                    "        const uniqueParam = new Date().getTime();\n" +
                    "        fetch(`data/${fileName}?_=${uniqueParam}`)\n" +
                    "            .then(response => response.json())\n" +
                    "            .then(data => {\n" +
                    "                cy = cytoscape({\n" +
                    "                    container: document.getElementById('cy'),\n" +
                    "                    elements: data.elements,\n" +
                    "                    style: cytoscapeStyles,\n" +
                    "                    layout: {\n" +
                    "                        name: layoutSelector.value,\n" +
                    "                        rankDir: 'LR',\n" +
                    "                        nodeSep: 80,\n" +
                    "                        edgeSep: 10,\n" +
                    "                        rankSep: 200,\n" +
                    "                    }\n" +
                    "                });\n" +
                    "            })\n" +
                    "            .catch(error => console.error('Error loading graph data:', error));\n" +
                    "    }\n\n" +
                    "    // Ajouter un écouteur d'événement pour le changement de layout\n" +
                    "    layoutSelector.addEventListener('change', function() {\n" +
                    "        if (cy) {\n" +
                    "            cy.layout({\n" +
                    "                name: layoutSelector.value,\n" +
                    "                rankDir: 'LR',\n" +
                    "                nodeSep: 80,\n" +
                    "                edgeSep: 10,\n" +
                    "                rankSep: 200,\n" +
                    "            }).run();\n" +
                    "        }\n" +
                    "    });\n" +
                    "});";

            mainjs.write(mainjscontent);
            mainjs.close();

            BufferedWriter cytostylesjs = new BufferedWriter(new FileWriter(jsDirPath + "/cytoscape-styles.js"));
            String cytostylecontent = "const cytoscapeStyles = [\n" +
                    "    {\n" +
                    "        selector: 'node',\n" +
                    "        style: {\n" +
                    "            'label': 'data(label)',\n" +
                    "            'shape': ele => {\n" +
                    "                const shape = ele.data('shape');\n" +
                    "                switch (shape) {\n" +
                    "                    case 'folder':\n" +
                    "                        return 'rectangle';\n" +
                    "                    case 'box':\n" +
                    "                        return 'roundrectangle';\n" +
                    "                    case 'doubleoctagon':\n" +
                    "                        return 'octagon';\n" +
                    "                    default:\n" +
                    "                        return shape; // Utilise la forme si elle est supportée\n" +
                    "                }\n" +
                    "            },\n" +
                    "            'background-color': '#666',\n" +
                    "            'text-valign': 'center',\n" +
                    "            'text-halign': 'center',\n" +
                    "            'color': '#fff',\n" +
                    "            'width': 'label',\n" +
                    "            'height': 'label',\n" +
                    "            'padding': '5px',\n" +
                    "            'border-width': 1,\n" +
                    "            'border-color': '#000'\n" +
                    "        }\n" +
                    "    },\n" +
                    "    {\n" +
                    "        selector: 'edge',\n" +
                    "        style: {\n" +
                    "            'width': 3,\n" +
                    "            'line-color': ele => {\n" +
                    "                return ele.data('color') === 'red' ? 'red' : '#ccc';\n" +
                    "            },\n" +
                    "            'target-arrow-color': ele => {\n" +
                    "                return ele.data('color') === 'red' ? 'red' : '#ccc';\n" +
                    "            },\n" +
                    "            'target-arrow-shape': 'triangle',\n" +
                    "            'arrow-scale': 4, // Ajustez cette valeur pour agrandir ou rétrécir les pointes de flèche\n" +
                    "            'line-style': ele => {\n" +
                    "                return ele.data('style') === 'dotted' ? 'dotted' : 'solid';\n" +
                    "            },\n" +
                    "            'curve-style': 'bezier'\n" +
                    "        }\n" +
                    "    }\n" +
                    "];";
            cytostylesjs.write(cytostylecontent);
            cytostylesjs.close();

            BufferedWriter cssstyles = new BufferedWriter(new FileWriter(cssDirPath + "/styles.css"));
            String cssstylescontent = "html, body {\n" +
                    "    height: 100%;\n" +
                    "    margin: 0;\n" +
                    "    display: flex;\n" +
                    "    flex-direction: column;\n" +
                    "}\n\n" +
                    ".container {\n" +
                    "    display: flex;\n" +
                    "    flex-direction: column;\n" +
                    "    height: 100%;\n" +
                    "}\n\n" +
                    ".controls {\n" +
                    "    padding: 10px;\n" +
                    "    background-color: #f4f4f4;\n" +
                    "    border-bottom: 1px solid #ccc;\n" +
                    "}\n\n" +
                    "#cy {\n" +
                    "    flex: 1;\n" +
                    "    height: calc(100% - 50px); /* Adjust based on the height of the controls */\n" +
                    "    border: 1px solid #ccc;\n" +
                    "}";
            cssstyles.write(cssstylescontent);
            cssstyles.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void processDotFile(Path dotFilePath, String jsonDirPath) throws IOException {
        String dotContent = readFile(dotFilePath);
        JsonGraph cytoscapeData = parseDotToCytoscape(dotContent);
        String baseFileName = dotFilePath.getFileName().toString().replaceAll("\\.dot$", "");
        String outputJsonFile = jsonDirPath + "/" + baseFileName + ".json";
        lastJsonLine = "      " + "\"" + baseFileName + ".json\",";
        graphsList.write(lastJsonLine);
        graphsList.newLine();
        BufferedWriter fileWriter = new BufferedWriter(new FileWriter(outputJsonFile));
        fileWriter.write("{");
        fileWriter.newLine();
        fileWriter.write("  \"elements\": {");
        fileWriter.newLine();
        fileWriter.write("      \"nodes\": [");
        fileWriter.newLine();
        JsonArray nodes = cytoscapeData.getNodes();
        int nodesSize = nodes.size();
        for ( int i = 0; i < nodesSize; i++) {
            JsonElement node = nodes.get(i);
            fileWriter.write("         " + node.toString());
            if (i != nodes.size() - 1) {
                fileWriter.write(",");
            }
            fileWriter.newLine();
        }
        fileWriter.write("],");
        fileWriter.newLine();
        fileWriter.write("      \"edges\": [");
        fileWriter.newLine();
        JsonArray edges = cytoscapeData.getEdges();
        int edgesSize = edges.size();
        for ( int i = 0; i < edgesSize; i++) {
            JsonElement edge = edges.get(i);
            fileWriter.write("          " + edge.toString());
            if (i != edges.size() - 1) {
                fileWriter.write(",");
            }
            fileWriter.newLine();
        }
        fileWriter.write("      ]");
        fileWriter.newLine();
        fileWriter.write("   }");
        fileWriter.newLine();
        fileWriter.write("}");
        fileWriter.newLine();
        fileWriter.close();
    }

    private static String readFile(Path filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath.toFile()))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    private static void writeFile(String filePath, String content) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            bw.write(content);
        }
    }

    private static JsonGraph parseDotToCytoscape(String dotContent) {
        Pattern nodePattern = Pattern.compile("\\s*n(\\d+)\\s*\\[shape=(\\w+),label=\"([^\"]+)\"\\];");
        Pattern edgePattern = Pattern.compile("\\s*n(\\d+)\\s*->\\s*n(\\d+)\\s*(\\[\\s*(color=(\\w+))?(\\s*,?\\s*style=(\\w+))?\\s*\\])?\\s*?;");

        JsonGraph jsongraph  = new JsonGraph(new JsonArray(), new JsonArray());

        Matcher nodeMatcher = nodePattern.matcher(dotContent);
        while (nodeMatcher.find()) {
            String nodeId = nodeMatcher.group(1);
            String shape = nodeMatcher.group(2);
            String label = nodeMatcher.group(3);

            JsonObject node = new JsonObject();
            JsonObject data = new JsonObject();
            data.addProperty("id", label);
            data.addProperty("label", label);
            data.addProperty("shape", shape);

            node.add("data", data);
            jsongraph.getNodes().add(node);
        }

        Matcher edgeMatcher = edgePattern.matcher(dotContent);
        while (edgeMatcher.find()) {
            String sourceId = edgeMatcher.group(1);
            String targetId = edgeMatcher.group(2);
            String color = edgeMatcher.group(5); // color is in group 5 if present
            String style = edgeMatcher.group(7); // style is in group 7 if present

            String sourceLabel = getNodeLabel(dotContent, sourceId);
            String targetLabel = getNodeLabel(dotContent, targetId);

            System.out.println("Parsing edge from " + sourceLabel + " to " + targetLabel +
                    (color != null ? " with color " + color : "") +
                    (style != null ? " and style " + style : ""));

            JsonObject edge = new JsonObject();
            JsonObject data = new JsonObject();
            data.addProperty("source", sourceLabel);
            data.addProperty("target", targetLabel);
            if (color != null) {
                data.addProperty("color", color);
            }
            if (style != null) {
                data.addProperty("style", style);
            }
            edge.add("data", data);
            jsongraph.getEdges().add(edge);
        }

        return jsongraph;
    }

    private static String getNodeLabel(String dotContent, String nodeId) {
        Pattern nodeLabelPattern = Pattern.compile("\\s*n" + nodeId + "\\s*\\[shape=\\w+,label=\"([^\"]+)\"\\];");
        Matcher matcher = nodeLabelPattern.matcher(dotContent);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null; // or throw an exception if a label is expected to always be found
    }

    private static void createServerScripts(String projectBasePath, String cyDirPath) throws IOException {
        // Shell script for Linux/MacOS to start the server and open the browser
        String shellScriptContent = "#!/bin/bash\n" +
                "cd " + cyDirPath + "\n" +
                "python -m http.server 8000 &\n" +
                "sleep 1\n" +
                "(xdg-open http://localhost:8000 || open http://localhost:8000) &\n";
        String shellScriptPath = projectBasePath + "/start_server.sh";
        writeFile(shellScriptPath, shellScriptContent);

        // Shell script for Linux/MacOS to stop the server
        String stopShellScriptContent = "#!/bin/bash\n" +
                "cd " + cyDirPath + "\n" +
                "pkill -f 'python -m http.server'\n" + // Kill all local HTTP servers
                "echo 'All local HTTP servers stopped.'\n";
        String stopShellScriptPath = projectBasePath + "/stop_server.sh";
        writeFile(stopShellScriptPath, stopShellScriptContent);
        Path stopShellPath = Paths.get(stopShellScriptPath);
        stopShellPath.toFile().setExecutable(true);

        // Make the shell script executable
        Path shellPath = Paths.get(shellScriptPath);
        shellPath.toFile().setExecutable(true);

        // Batch script for Windows to start the server and open the browser
        String batchScriptContent = "@echo off\n" +
                "cd " + cyDirPath + "\n" +
                "start /B python -m http.server 8000 > NUL\n" +
                "timeout /t 2\n" +
                "start http://localhost:8000\n";
        String batchScriptPath = projectBasePath + "/start_server.bat";
        writeFile(batchScriptPath, batchScriptContent);

        // Batch script for Windows to stop the server
        String stopBatchScriptContent = "@echo off\n" +
                "cd " + cyDirPath + "\n" +
                "taskkill /F /FI \"WINDOWTITLE eq Python*\" /T\n" + // Kill all local HTTP servers
                "echo All local HTTP servers stopped.\n";
        String stopBatchScriptPath = projectBasePath + "/stop_server.bat";
        writeFile(stopBatchScriptPath, stopBatchScriptContent);
        Path stopBatchPath = Paths.get(stopBatchScriptPath);
        stopBatchPath.toFile().setExecutable(true);
    }
}
