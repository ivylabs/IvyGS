package com.ivyis.pentaho.ivygs.manager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.jersey.core.header.FormDataContentDisposition;

/**
 * @author <a href="mailto:joel.latino@ivy-is.co.uk">Joel Latino</a>
 * @since 1.0.0
 */
public final class SettingsManager {
  private static final Logger LOG = LoggerFactory
      .getLogger(SettingsManager.class);
  private static final String SETTINGS_FOLDER = "system/.ivygs";
  private static final String IVYGS_GIT_REPOS_FOLDER = "IvyGS";
  private static final String CONFIG_FILE_NAME = "config.xml";
  private static final String REPO_NAME_FIELD_NAME = "name";
  private static final String GIT_CLONE_URL_FIELD_NAME = "GitCloneURL";
  private static final String GIT_USERNAME_FIELD_NAME = "GitUsername";
  private static final String GIT_PASSWORD_FIELD_NAME = "GitPassword";
  private static final String POINT_CHECKOUT_FIELD_NAME = "PointCheckout";
  private static final String CREATION_DATE_FIELD_NAME = "CreationDate";
  private static final String ETL_FOLDER_PATH_FIELD_NAME = "ETLFolderPath";
  private static final String GIT_REPOS_FOLDER_PATH_FIELD_NAME = "GitReposFolderPath";
  private static final SimpleDateFormat DF = new SimpleDateFormat(
      "yyyy-mm-dd hh:mm:ss");
  private static volatile SettingsManager instance = null;
  private Settings settings;

  private SettingsManager() {
    this.settings = readSettingsFile(new File(PentahoSystem
        .getApplicationContext().getSolutionRootPath()
        + File.separator
        + SETTINGS_FOLDER + File.separator + CONFIG_FILE_NAME)
    .getAbsolutePath());
    if (this.settings == null) {
      this.settings = new Settings();
    }
  }

  public static SettingsManager getInstance() {
    if (instance == null) {
      synchronized (SettingsManager.class) {
        if (instance == null) {
          instance = new SettingsManager();
        }
      }
    }
    return instance;
  }

  public SettingsManager refresh() {
    this.settings = readSettingsFile(new File(PentahoSystem
        .getApplicationContext().getSolutionRootPath()
        + File.separator
        + SETTINGS_FOLDER + File.separator + CONFIG_FILE_NAME)
    .getAbsolutePath());
    return instance;
  }

  public boolean setSettings(String gitURL, String gitUserName,
      String gitPassword, InputStream uploadedInputStream,
      FormDataContentDisposition fileDetail) {
    try {
      if (!"".equalsIgnoreCase(fileDetail.getFileName())) {
        final File etlRepoFile = new File(PentahoSystem
            .getApplicationContext().getSolutionRootPath()
            + File.separator
            + SETTINGS_FOLDER
            + File.separator
            + fileDetail.getFileName());
        final File etlRepoFolder = new File(PentahoSystem
            .getApplicationContext().getSolutionRootPath()
            + File.separator
            + SETTINGS_FOLDER
            + File.separator
            + "repo");
        rmdir(etlRepoFolder);
        etlRepoFolder.delete();
        if (!etlRepoFolder.exists()) {
          etlRepoFolder.mkdirs();
        }
        writeToFile(uploadedInputStream, etlRepoFile.getAbsolutePath());
        unZipIt(etlRepoFile.getAbsolutePath(),
            etlRepoFolder.getAbsolutePath());
        etlRepoFile.delete();

        this.settings.setEtlFolderPath(etlRepoFolder.getAbsolutePath());
      }

      // Set settings
      this.settings.setGitURL(gitURL);
      this.settings.setGitUserName(gitUserName);
      this.settings.setGitPassword(gitPassword);
      this.settings.setGitRepoFolderPath(new File(PentahoSystem.getApplicationContext()
          .getSolutionRootPath()
          + File.separator
          + IVYGS_GIT_REPOS_FOLDER
          + File.separator
          + "repo").getAbsolutePath());
      this.settings.setPointCheckout("master");
      saveSettings(this.settings);

      final File gitRepoFolder = new File(this.settings.getGitRepoFolderPath());
      rmdir(gitRepoFolder);
      gitRepoFolder.delete();

      // Clone project
      Git.cloneRepository()
      .setURI(this.settings.getGitURL())
      .setDirectory(new File(this.settings.getGitRepoFolderPath()))
      .setBranch(
          this.settings.getPointCheckout() == null ? "master" : this.settings
              .getPointCheckout())
              .setCloneAllBranches(false)
              .setCloneSubmodules(true)
              .setCredentialsProvider(
                  new UsernamePasswordCredentialsProvider(this.settings.getGitUserName(), this.settings
                      .getGitPassword())).setBare(false).call();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Empty and delete a folder (and subfolders).
   * 
   * @param folder folder to empty
   */
  public void rmdir(final File folder) {
    // check if folder file is a real folder
    if (folder.isDirectory()) {
      final File[] list = folder.listFiles();
      if (list != null) {
        for (int i = 0; i < list.length; i++) {
          final File tmpF = list[i];
          if (tmpF.isDirectory()) {
            rmdir(tmpF);
          }
          tmpF.delete();
        }
      }
      if (!folder.delete()) {
        LOG.error("can't delete folder : " + folder);
      }
    }
  }

  private void writeToFile(InputStream uploadedInputStream, String uploadedFileLocation) {
    try {
      final OutputStream out = new FileOutputStream(new File(uploadedFileLocation));
      int read = 0;
      final byte[] bytes = new byte[1024];

      while ((read = uploadedInputStream.read(bytes)) != -1) {
        out.write(bytes, 0, read);
      }
      out.flush();
      out.close();
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
    }

  }

  public void unZipIt(String zipFile, String outputFolder) {

    try {
      final File destDir = new File(outputFolder);
      if (!destDir.exists()) {
        destDir.mkdirs();
      }
      final ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFile));
      ZipEntry entry = zipIn.getNextEntry();
      // iterates over entries in the zip file
      while (entry != null) {
        final String filePath = outputFolder + File.separator + entry.getName();
        if (!entry.isDirectory()) {
          // if the entry is a file, extracts it
          extractFile(zipIn, filePath);
        } else {
          // if the entry is a directory, make the directory
          final File dir = new File(filePath);
          dir.mkdir();
        }
        zipIn.closeEntry();
        entry = zipIn.getNextEntry();
      }
      zipIn.close();

      LOG.debug("Done");

    } catch (IOException ex) {
      LOG.error(ex.getMessage(), ex);
    }
  }

  /**
   * Extracts a zip entry (file entry).
   * 
   * @param zipIn
   * @param filePath
   * @throws IOException
   */
  private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
    final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
    final byte[] bytesIn = new byte[4096];
    int read = 0;
    while ((read = zipIn.read(bytesIn)) != -1) {
      bos.write(bytesIn, 0, read);
    }
    bos.close();
  }

  private void saveSettings(Settings settings) throws ParserConfigurationException,
  TransformerException {
    final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    final DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

    // root elements
    final Document doc = docBuilder.newDocument();
    final Element rootElement = doc.createElement("projects");
    doc.appendChild(rootElement);

    final Element projectEl = doc.createElement("project");
    rootElement.appendChild(projectEl);

    final Element licenseKeyEl = doc.createElement(REPO_NAME_FIELD_NAME);
    licenseKeyEl.appendChild(doc.createTextNode("repo"));
    projectEl.appendChild(licenseKeyEl);

    final Element gitURLEl = doc.createElement(GIT_CLONE_URL_FIELD_NAME);
    gitURLEl.appendChild(doc.createTextNode(settings.getGitURL()));
    projectEl.appendChild(gitURLEl);

    final Element gitUserNameEl = doc.createElement(GIT_USERNAME_FIELD_NAME);
    gitUserNameEl.appendChild(doc.createTextNode(settings.getGitUserName()));
    projectEl.appendChild(gitUserNameEl);

    final Element gitPasswordEl = doc.createElement(GIT_PASSWORD_FIELD_NAME);
    gitPasswordEl.appendChild(doc.createTextNode(settings.getGitPassword()));
    projectEl.appendChild(gitPasswordEl);

    final Element pointCheckoutEl = doc.createElement(POINT_CHECKOUT_FIELD_NAME);
    pointCheckoutEl.appendChild(doc.createTextNode(settings.getPointCheckout()));
    projectEl.appendChild(pointCheckoutEl);

    final Element creationDateEl = doc.createElement(CREATION_DATE_FIELD_NAME);
    creationDateEl.appendChild(doc.createTextNode(DF.format(settings.getCreationDate())));
    projectEl.appendChild(creationDateEl);

    final Element gitReposFolderPath = doc.createElement(GIT_REPOS_FOLDER_PATH_FIELD_NAME);
    gitReposFolderPath.appendChild(doc.createTextNode(settings.getGitRepoFolderPath()));
    projectEl.appendChild(gitReposFolderPath);

    final Element etlFolderPathEl = doc.createElement(ETL_FOLDER_PATH_FIELD_NAME);
    etlFolderPathEl.appendChild(doc.createTextNode(settings.getEtlFolderPath()));
    projectEl.appendChild(etlFolderPathEl);

    final TransformerFactory transformerFactory = TransformerFactory.newInstance();
    final Transformer transformer = transformerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    final DOMSource source = new DOMSource(doc);
    if (!new File(PentahoSystem.getApplicationContext().getSolutionRootPath() + File.separator
        + SETTINGS_FOLDER).exists()) {
      new File(PentahoSystem.getApplicationContext().getSolutionRootPath() + File.separator
          + SETTINGS_FOLDER).mkdirs();
    }
    final File licenseKeyFile =
        new File(PentahoSystem.getApplicationContext().getSolutionRootPath() + File.separator
            + SETTINGS_FOLDER + File.separator + CONFIG_FILE_NAME);

    final StreamResult result = new StreamResult(licenseKeyFile);

    transformer.transform(source, result);
    LOG.debug("License saved.");
  }

  private static Settings readSettingsFile(String path) {
    final Settings settings = new Settings();

    if (!new File(path).exists()) {
      return null;
    }

    try {
      final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      final Document doc = dBuilder.parse(path);
      doc.getDocumentElement().normalize();
      final NodeList nList = doc.getElementsByTagName("project");
      for (int temp = 0; temp < nList.getLength(); temp++) {
        final Node nNode = nList.item(temp);
        if (nNode.getNodeType() == Node.ELEMENT_NODE) {

          final Element eElement = (Element) nNode;

          settings.setRepoName(eElement.getElementsByTagName(REPO_NAME_FIELD_NAME).item(0)
              .getTextContent());
          settings.setGitURL(eElement.getElementsByTagName(GIT_CLONE_URL_FIELD_NAME).item(0)
              .getTextContent());
          settings.setGitUserName(eElement.getElementsByTagName(GIT_USERNAME_FIELD_NAME).item(0)
              .getTextContent());
          settings.setGitPassword(eElement.getElementsByTagName(GIT_PASSWORD_FIELD_NAME).item(0)
              .getTextContent());
          settings.setPointCheckout(eElement.getElementsByTagName(POINT_CHECKOUT_FIELD_NAME)
              .item(0).getTextContent());
          settings.setCreationDate(DF.parse(eElement.getElementsByTagName(CREATION_DATE_FIELD_NAME)
              .item(0).getTextContent()));
          settings.setEtlFolderPath(eElement.getElementsByTagName(ETL_FOLDER_PATH_FIELD_NAME)
              .item(0).getTextContent());

        }
      }
    } catch (Exception e) {
      LOG.warn(e.getMessage(), e);
    }
    return settings;
  }

  public Settings getSettings() {
    return settings;
  }
}
