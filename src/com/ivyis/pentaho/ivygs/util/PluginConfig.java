package com.ivyis.pentaho.ivygs.util;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ivyis.opensource.license.manager.LicenseDetails;
import com.ivyis.opensource.license.manager.LicenseManagerFactory;

/**
 * @author <a href="mailto:joel.latino@ivy-is.co.uk">Joel Latino</a>
 * @since 1.0.0
 */
public class PluginConfig {
  public static final String PLUGIN_LICENSE_KEY_FILE = "LicenseKey.xml";
  public static final String SYSTEM_FOLDER = "system";

  public static final String LICENSE_KEY = "UserLicense";
  public static final String LICENSE_PLUGIN_NAME = "UserPluginName";
  public static final String LICENSE_PLUGIN_TYPE = "UserPluginType";
  public static final String LICENSE_USERNAME = "UserName";
  public static final String LICENSE_USER_EMAIL = "UserEmail";
  public static final String LICENSE_LICENSE_DATE = "UserLicenseDate";
  public static final String PLUGIN_NAME = "IvyGS";

  private static final Logger log = LoggerFactory
      .getLogger(PluginConfig.class);
  private static final SimpleDateFormat DF = new SimpleDateFormat(
      "yyyy-mm-dd hh:mm:ss");
  private static volatile PluginConfig instance = null;

  private Map<String, String> licenseProps = new HashMap<String, String>();

  private PluginConfig() {
    final File licenseKeyFile = new File(PentahoSystem
        .getApplicationContext().getSolutionRootPath()
        + File.separator
        + SYSTEM_FOLDER
        + File.separator
        + PLUGIN_NAME
        + File.separator
        + PLUGIN_LICENSE_KEY_FILE);
    if (licenseKeyFile.exists()) {
      this.licenseProps = readLicenseKeyFile(licenseKeyFile
          .getAbsolutePath());
    }
  }

  public static PluginConfig getInstance() {
    if (instance == null) {
      synchronized (PluginConfig.class) {
        if (instance == null) {
          instance = new PluginConfig();
        }
      }
    }
    return instance;
  }

  public boolean checkLicense() {
    if (licenseProps.get(LICENSE_KEY) == null) {
      return false;
    }
    final LicenseDetails ld = getLicenseDetails(licenseProps
        .get(LICENSE_KEY));
    return ld.getPlugin().equalsIgnoreCase(PLUGIN_NAME)
        && ld.getCustomerName().equalsIgnoreCase(
            licenseProps.get(LICENSE_USERNAME))
            && ld.getEmail().equalsIgnoreCase(
                licenseProps.get(LICENSE_USER_EMAIL));
  }

  public Map<String, String> writeLicenseKeyFile(String licenseKey)
      throws TransformerException, ParserConfigurationException {
    final LicenseDetails ld = getLicenseDetails(licenseKey);
    if (ld.getPlugin().equalsIgnoreCase(PLUGIN_NAME)) {

      final DocumentBuilderFactory docFactory = DocumentBuilderFactory
          .newInstance();
      final DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

      // root elements
      final Document doc = docBuilder.newDocument();
      final Element rootElement = doc.createElement("license");
      doc.appendChild(rootElement);

      final Element userEl = doc.createElement("user");
      rootElement.appendChild(userEl);

      final Element licenseKeyEl = doc.createElement(LICENSE_KEY);
      licenseKeyEl.appendChild(doc.createTextNode(licenseKey));
      userEl.appendChild(licenseKeyEl);

      final Element licensePluginNameEl = doc
          .createElement(LICENSE_PLUGIN_NAME);
      licensePluginNameEl.appendChild(doc.createTextNode(ld.getPlugin()));
      userEl.appendChild(licensePluginNameEl);

      final Element licensePluginTypeEl = doc
          .createElement(LICENSE_PLUGIN_TYPE);
      licensePluginTypeEl.appendChild(doc.createTextNode(ld.getType()));
      userEl.appendChild(licensePluginTypeEl);

      final Element usernameEl = doc.createElement(LICENSE_USERNAME);
      usernameEl.appendChild(doc.createTextNode(ld.getCustomerName()));
      userEl.appendChild(usernameEl);

      final Element userEmailEL = doc.createElement(LICENSE_USER_EMAIL);
      userEmailEL.appendChild(doc.createTextNode(ld.getEmail()));
      userEl.appendChild(userEmailEL);

      final Element licenseDateEl = doc
          .createElement(LICENSE_LICENSE_DATE);
      licenseDateEl.appendChild(doc.createTextNode(DF.format(Calendar
          .getInstance().getTime())));
      userEl.appendChild(licenseDateEl);

      final TransformerFactory transformerFactory = TransformerFactory
          .newInstance();
      final Transformer transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      final DOMSource source = new DOMSource(doc);
      final File licenseKeyFile = new File(PentahoSystem
          .getApplicationContext().getSolutionRootPath()
          + File.separator
          + SYSTEM_FOLDER
          + File.separator
          + PLUGIN_NAME + File.separator + PLUGIN_LICENSE_KEY_FILE);

      final StreamResult result = new StreamResult(licenseKeyFile);

      transformer.transform(source, result);
      log.debug("License saved.");

      this.licenseProps = readLicenseKeyFile(licenseKeyFile
          .getAbsolutePath());
      return this.licenseProps;

    }
    return null;
  }

  private LicenseDetails getLicenseDetails(String key) {
    return LicenseManagerFactory.getLicenseManager().decodeLicense(key);
  }

  private static Map<String, String> readLicenseKeyFile(String path) {
    final Map<String, String> mapLicenseKey = new HashMap<String, String>();

    try {
      final DocumentBuilderFactory dbFactory = DocumentBuilderFactory
          .newInstance();
      final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      final Document doc = dBuilder.parse(path);
      doc.getDocumentElement().normalize();
      final NodeList nList = doc.getElementsByTagName("user");
      for (int temp = 0; temp < nList.getLength(); temp++) {
        final Node nNode = nList.item(temp);
        if (nNode.getNodeType() == Node.ELEMENT_NODE) {

          final Element eElement = (Element) nNode;

          mapLicenseKey.put(LICENSE_KEY, eElement
              .getElementsByTagName(LICENSE_KEY).item(0)
              .getTextContent());
          mapLicenseKey.put(LICENSE_PLUGIN_NAME, eElement
              .getElementsByTagName(LICENSE_PLUGIN_NAME).item(0)
              .getTextContent());
          mapLicenseKey.put(LICENSE_PLUGIN_TYPE, eElement
              .getElementsByTagName(LICENSE_PLUGIN_TYPE).item(0)
              .getTextContent());
          mapLicenseKey.put(LICENSE_USERNAME, eElement
              .getElementsByTagName(LICENSE_USERNAME).item(0)
              .getTextContent());
          mapLicenseKey.put(LICENSE_USER_EMAIL, eElement
              .getElementsByTagName(LICENSE_USER_EMAIL).item(0)
              .getTextContent());
          mapLicenseKey.put(LICENSE_LICENSE_DATE, eElement
              .getElementsByTagName(LICENSE_LICENSE_DATE).item(0)
              .getTextContent());

        }
      }
    } catch (Exception e) {
      log.warn(e.getMessage(), e);
    }
    return mapLicenseKey;
  }
}
