package com.ivyis.pentaho.ivygs.manager;

import java.util.Date;

/**
 * @author <a href="mailto:joel.latino@ivy-is.co.uk">Joel Latino</a>
 * @since 1.0.0
 */
public class Settings {
  private String repoName;
  private String gitURL;
  private String gitUserName;
  private String gitPassword;
  private String pointCheckout;
  private String etlFolderPath;
  private String gitRepoFolderPath;
  private Date creationDate;

  public Settings() {
    this.pointCheckout = "master";
    this.creationDate = new Date();
    this.gitURL = "";
    this.gitUserName = "";
    this.gitPassword = "";
  }

  public Settings(String gitURL, String gitUserName, String gitPassword,
      String pointCheckout, Date creationDate) {
    this.gitURL = gitURL;
    this.gitUserName = gitUserName;
    this.gitPassword = gitPassword;
    this.pointCheckout = pointCheckout;
    this.creationDate = creationDate;
  }

  public Settings(String gitURL, String gitUserName, String gitPassword,
      String etlFolderPath) {
    this.gitURL = gitURL;
    this.gitUserName = gitUserName;
    this.gitPassword = gitPassword;
    this.pointCheckout = "master";
    this.creationDate = new Date();
    this.etlFolderPath = etlFolderPath;
  }

  public Settings(String gitURL, String gitUserName, String gitPassword) {
    this.gitURL = gitURL;
    this.gitUserName = gitUserName;
    this.gitPassword = gitPassword;
    this.pointCheckout = "master";
    this.creationDate = new Date();
  }

  public String getRepoName() {
    return repoName;
  }

  public void setRepoName(String repoName) {
    this.repoName = repoName;
  }

  public String getGitURL() {
    return gitURL;
  }

  public void setGitURL(String gitURL) {
    this.gitURL = gitURL;
  }

  public String getGitUserName() {
    return gitUserName;
  }

  public void setGitUserName(String gitUserName) {
    this.gitUserName = gitUserName;
  }

  public String getGitPassword() {
    return gitPassword;
  }

  public void setGitPassword(String gitPassword) {
    this.gitPassword = gitPassword;
  }

  public String getPointCheckout() {
    return pointCheckout;
  }

  public void setPointCheckout(String pointCheckout) {
    this.pointCheckout = pointCheckout;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }

  public String getEtlFolderPath() {
    return etlFolderPath;
  }

  public void setEtlFolderPath(String etlFolderPath) {
    this.etlFolderPath = etlFolderPath;
  }

  public String getGitRepoFolderPath() {
    return gitRepoFolderPath;
  }

  public void setGitRepoFolderPath(String gitRepoFolderPath) {
    this.gitRepoFolderPath = gitRepoFolderPath;
  }

}
