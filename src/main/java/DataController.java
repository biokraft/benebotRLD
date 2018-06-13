import static java.lang.System.getProperty;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import javafx.scene.chart.PieChart.Data;
import javassist.NotFoundException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class DataController {
  private static String     configFilePath = getProperty("user.dir") + File.separator + "Bot_data";
  private ArrayList<File>   configFiles;
  private ArrayList<String> fileNames;

  public DataController () {
    configFiles = new ArrayList<File>();
    fileNames = new ArrayList<String>();
  }

  public int addFile (String fileName) {
    // If its already part of the DataController exit this method
    if (fileNames.contains(fileName)) {
      System.out.println(">>> " + fileName + " already exists, continuing...");
      return -1;
    }

    // Create a new file
    File configFile = new File(configFilePath, fileName);

    // If the file already exists just add it to the DataController
    if (configFile.exists()) {
      System.out.println(">>> " + fileName + " already exists, continuing...");
      configFiles.add(configFile);
      fileNames.add(fileName);
    } else {
      if (!configFile.getParentFile().isDirectory()) {
        configFile.getParentFile().mkdirs();
      }
      try {
        // Create a new File
        configFile.createNewFile();
        System.out.println(">>> " + configFile + " succesfully created");

        // Add file and name into the DataController
        configFiles.add(configFile);
        fileNames.add(fileName);
      } catch (IOException ex) {
        System.out.println("<<< Error creating config file");
        return -1;
      }
    }
    return configFiles.size()-1;
  }

  public File getFile (int fileID) {
    return configFiles.get(fileID);
  }

  public File getFile (String fileName) {
    for (int i = 0; i < fileNames.size(); i++) {
      if (fileNames.get(i).equals(fileName)) {
        return configFiles.get(i);
      }
    }
    return null;
  }

  public boolean setFile (File file, int fileID) {
    if (fileID >= configFiles.size()) throw new IndexOutOfBoundsException();

    ArrayList<File> tmp = new ArrayList<File>();
    boolean success = false;

    for (int i = 0; i < configFiles.size(); i++) {
      if (i == fileID) {
        tmp.add(file);
        success = true;
      } else {
        tmp.add(configFiles.get(i));
      }
    }

    if (success) {
      configFiles = tmp;
    }

    return success;
  }

  public ArrayList<Command> parseCommands (String fileName) {
    ArrayList<Command> result = new ArrayList<Command>();
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = null; Document doc = null;
    try {
      builder = factory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    }
    try {
      doc = builder.parse(this.getFile(fileName));
    } catch (SAXException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    NodeList commandList = doc.getElementsByTagName("command");

    for (int i = 0; i < commandList.getLength(); i++) {
      Node c = commandList.item(i);
      if (c.getNodeType() == Node.ELEMENT_NODE) {
        Element command = (Element) c;
        String id = command.getAttribute("id");
        NodeList nodeList = command.getChildNodes();

        String name = null;
        String response = null;
        for (int j = 0; j < nodeList.getLength(); j++) {
          Node n = nodeList.item(j);
          if (n.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) n;
            if (element.getTagName().equals("name")) name = element.getTextContent();
            else if (element.getTagName().equals("response")) response = element.getTextContent();
          }
        }
        if (name != null && response != null) {
          result.add(new Command(name, response));
        }
      }
    }
    return result;
  }

  public static void main (String args[]) throws IOException, SAXException {
    DataController dataController = new DataController();
    dataController.addFile("commands.xml");

  }

}
