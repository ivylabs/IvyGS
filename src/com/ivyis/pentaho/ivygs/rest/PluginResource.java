package com.ivyis.pentaho.ivygs.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.StreamingOutput;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.webdetails.cpf.messaging.JsonGeneratorSerializable;
import pt.webdetails.cpf.messaging.JsonResult;
import pt.webdetails.cpf.utils.JsonHelper;
import pt.webdetails.cpf.utils.MimeTypes;
import pt.webdetails.cpk.CpkApi;

import com.ivyis.pentaho.ivygs.manager.GitOperationsManager;
import com.ivyis.pentaho.ivygs.manager.SettingsManager;
import com.ivyis.pentaho.ivygs.util.PluginConfig;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

/**
 * @author <a href="mailto:joel.latino@ivy-is.co.uk">Joel Latino</a>
 * @since 1.0.0
 */
@Path("/{plugin}/api")
public class PluginResource extends CpkApi {

  private static final Logger log = LoggerFactory
      .getLogger(PluginResource.class);

  public PluginResource() {
    super();
  }

  @GET
  @Path("/checkLicense")
  @Produces(MimeTypes.JSON)
  public StreamingOutput checkLicense(@PathParam("pluginId") String pluginId)
      throws WebApplicationException {
    try {
    	SettingsManager.getInstance().saveSettings(SettingsManager.getInstance().getSettings());
      return toStreamingOutput(new JsonResult(true,
          String.valueOf(PluginConfig.getInstance().checkLicense())));
    } catch (Exception e) {
      return toErrorResult(e);
    }
  }

  @GET
  @Path("/checkNumNewCommits")
  @Produces(MimeTypes.JSON)
  public StreamingOutput checkNumNewCommits(
      @PathParam("pluginId") String pluginId)
          throws WebApplicationException {
    try {
      return toStreamingOutput(new JsonResult(true,
          String.valueOf(GitOperationsManager.checkNumNewCommits())));
    } catch (Exception e) {
      return toErrorResult(e);
    }
  }

  @POST
  @Path("/updateGitRepo")
  @Produces(MimeTypes.JSON)
  public StreamingOutput updateGitRepo() throws WebApplicationException {
    try {
      return toStreamingOutput(new JsonResult(true,
          String.valueOf(GitOperationsManager.updateGitRepo())));
    } catch (Exception e) {
      return toErrorResult(e);
    }
  }

  @POST
  @Path("/submitLicense")
  @Produces(MimeTypes.JSON)
  public StreamingOutput submitLicense(
      @PathParam("pluginId") String pluginId,
      @QueryParam("licenseKey") String licenseKey)
          throws WebApplicationException {
    try {
      final Map<String, String> map = PluginConfig.getInstance()
          .writeLicenseKeyFile(licenseKey);
      if (map == null) {
        return toErrorResult("License invalid.");
      }
      return toStreamingOutput(new JsonResult(true,
          new JSONObject(map).toString(2)));
    } catch (Exception e) {
      return toErrorResult(e);
    }
  }

  @POST
  @Path("/configSettings")
  @Consumes("multipart/form-data")
  public StreamingOutput configSettings(
      @FormDataParam("gitCloneURL") String gitURL,
      @FormDataParam("gitUsername") String gitUserName,
      @FormDataParam("gitPassword") String gitPassword,
      @FormDataParam("etlFile") InputStream uploadedInputStream,
      @FormDataParam("etlFile") FormDataContentDisposition fileDetail,
      @Context HttpServletRequest request,
      @Context HttpServletResponse response) {

    return toStreamingOutput(new JsonResult(true,
        String.valueOf(SettingsManager.getInstance().setSettings(
            gitURL, gitUserName, gitPassword, uploadedInputStream,
            fileDetail))));

  }

  private StreamingOutput toStreamingOutput(
      final JsonGeneratorSerializable json) {
    return new StreamingOutput() {
      public void write(OutputStream out) throws IOException,
      WebApplicationException {
        JsonHelper.writeJson(json, out);
      }
    };
  }

  private StreamingOutput toErrorResult(final Exception e) {
    log.error(e.getLocalizedMessage(), e);
    return new StreamingOutput() {
      public void write(OutputStream out) throws IOException,
      WebApplicationException {
        JsonHelper.writeJson(
            new JsonResult(false, e.getLocalizedMessage()), out);
      }
    };
  }

  private StreamingOutput toErrorResult(final String msg) {
    return new StreamingOutput() {
      public void write(OutputStream out) throws IOException,
      WebApplicationException {
        JsonHelper.writeJson(new JsonResult(false, msg), out);
      }
    };
  }

}
