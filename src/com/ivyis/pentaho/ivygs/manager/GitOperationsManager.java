package com.ivyis.pentaho.ivygs.manager;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ivyis.pentaho.ivygs.util.PluginConfig;

/**
 * @author <a href="mailto:joel.latino@ivy-is.co.uk">Joel Latino</a>
 * @since 1.0.0
 */
public class GitOperationsManager {
  private static final Logger LOG = LoggerFactory
      .getLogger(GitOperationsManager.class);

  public static boolean checkNumNewCommits() throws IOException,
  NoHeadException, GitAPIException {
    final Settings settings = SettingsManager.getInstance().refresh()
        .getSettings();
    if(settings==null||settings.getRepoName()==null){
    	return false;
    }
    final Repository repository = FileRepositoryBuilder.create(new File(
        PentahoSystem.getApplicationContext().getSolutionRootPath()
        + File.separator + PluginConfig.PLUGIN_NAME
        + File.separator + settings.getRepoName(), ".git"));

    final Git git = new Git(repository);
    final RevWalk walk = new RevWalk(repository);

    final Iterable<Ref> commits = git
        .lsRemote()
        .setCredentialsProvider(
            new UsernamePasswordCredentialsProvider(settings
                .getGitUserName(), settings.getGitPassword()))
                .setTags(true).setHeads(true).call();

    for (Ref commit : commits) {

      for (Map.Entry<String, Ref> e : repository.getAllRefs().entrySet()) {
        if (e.getKey().startsWith(Constants.R_HEADS)) {
          boolean parsed = true;
          try {
            walk.parseCommit(commit.getObjectId());
          } catch (Exception ex) {
            parsed = false;
          }
          if ((repository.resolve(commit.getName()) == null
              || walk.parseCommit(repository.resolve(commit
                  .getName())) == null || (!parsed || !walk
                      .isMergedInto(walk.parseCommit(walk
                          .parseCommit(commit.getObjectId())), walk
                          .parseCommit(e.getValue().getObjectId()))))
                          && commit.getName().contains(
                              settings.getPointCheckout())
                              && commit
                              .getName()
                              .substring(
                                  commit.getName()
                                  .lastIndexOf(
                                      settings.getPointCheckout()),
                                      commit.getName().length())
                                      .equals(settings.getPointCheckout())) {
            git.close();
            return true;
          }
        }
      }
    }
    git.close();

    return false;
  }

  public static boolean updateGitRepo() {
    try {
      final Settings settings = SettingsManager.getInstance()
          .getSettings();
      final File gitRepoFolder = new File(PentahoSystem
          .getApplicationContext().getSolutionRootPath()
          + File.separator
          + PluginConfig.PLUGIN_NAME
          + File.separator + settings.getRepoName());
      if (gitRepoFolder.exists()) {
        FileUtils.deleteDirectory(gitRepoFolder);
      }

      Git.cloneRepository()
      .setURI(settings.getGitURL())
      .setDirectory(gitRepoFolder)
      .setBranch(settings.getPointCheckout())
      .setCloneAllBranches(true)
      .setCredentialsProvider(
          new UsernamePasswordCredentialsProvider(settings
              .getGitUserName(), settings
              .getGitPassword())).setBare(false).call();
      return true;
    } catch (InvalidRemoteException e) {
      LOG.warn(e.getMessage(), e);
    } catch (TransportException e) {
      LOG.warn(e.getMessage(), e);
    } catch (GitAPIException e) {
      LOG.warn(e.getMessage(), e);
    } catch (IOException e) {
      LOG.warn(e.getMessage(), e);
    }

    return false;
  }
}
