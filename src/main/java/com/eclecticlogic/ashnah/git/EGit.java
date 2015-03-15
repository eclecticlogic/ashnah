/**
 * Copyright (c) 2014-2015 Eclectic Logic LLC
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package com.eclecticlogic.ashnah.git;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.jcraft.jsch.Session;

/**
 * Wrapper around the git client to make api calls feel more natural.
 * 
 * @author kabram.
 *
 */
public class EGit extends AbstractEGit<EGit> {

    private Git git;

    private static final Logger logger = LoggerFactory.getLogger(EGit.class);


    public EGit initialize() {
        if (getUri().startsWith("file:")) {
            SshSessionFactory.setInstance(new JschConfigSessionFactory() {

                @Override
                protected void configure(Host hc, Session session) {
                    session.setConfig("StrictHostKeyChecking", "no");
                }
            });
        }

        basedir = createBaseDir();
        git = createGitClient();
        if (new File(basedir, ".git").exists()) {
            fetch();
        }
        return this;
    }


    private Git createGitClient() {
        if (new File(basedir, ".git").exists()) {
            return openGitRepository();
        } else {
            return copyRepository();
        }
    }


    private Git copyRepository() {
        deleteBaseDirIfExists();
        verify(basedir.mkdirs(), "Could not create basedir: " + basedir);

        if (getUri().startsWith("file:")) {
            return copyFromLocalRepository();
        } else {
            return cloneToBasedir();
        }
    }


    private Git openGitRepository() {
        try {
            return Git.open(getWorkingDirectory());
        } catch (IOException e) {
            throw new GitRuntimeException(e);
        }
    }


    private Git copyFromLocalRepository() {
        try {
            URL url = new URL(PathUtil.cleanPath(getUri()));
            URI uri = new URI(url.toString().replaceAll(" ", "%20"));
            File remote = new File(uri.getSchemeSpecificPart());
            verify(remote.isDirectory(), "No directory at " + uri);
            File gitDir = new File(remote, ".git");
            verify(gitDir.exists(), "No .git at " + uri);
            verify(gitDir.isDirectory(), "No .git directory at " + uri);
            return Git.open(remote);
        } catch (Exception e) {
            throw new GitRuntimeException(e);
        }
    }


    private Git cloneToBasedir() {
        CloneCommand clone = Git.cloneRepository().setURI(getUri()).setDirectory(basedir);
        setCredentialsProvider(clone);
        try {
            return clone.call();
        } catch (GitAPIException e) {
            throw new GitRuntimeException(e);
        }
    }


    public Ref checkout(String label) {
        CheckoutCommand checkout = git.checkout();
        if (shouldTrack(label)) {
            trackBranch(checkout, label);
        } else {
            // works for tags and local branches
            checkout.setName(label);
        }
        try {
            return checkout.call();
        } catch (GitAPIException e) {
            throw new GitRuntimeException(e);
        }
    }


    public boolean shouldPull(Ref ref) {
        try {
            return git.status().call().isClean() && ref != null
                    && git.getRepository().getConfig().getString("remote", "origin", "url") != null;
        } catch (GitAPIException e) {
            throw new GitRuntimeException(e);
        }
    }


    public boolean shouldTrack(String label) {
        return isBranch(label) && !isLocalBranch(label);
    }


    /**
     * Assumes we are on a tracking branch (should be safe)
     */
    public void pull(String label, Ref ref) {
        PullCommand pull = git.pull();
        try {
            setCredentialsProvider(pull);
            pull.call();
        } catch (Exception e) {
            logger.warn("Could not pull remote for " + label + " (current ref=" + ref + "), remote: "
                    + git.getRepository().getConfig().getString("remote", "origin", "url"));
        }
    }


    public void fetch() {
        try {
            FetchCommand fetch = git.fetch();
            setCredentialsProvider(fetch);
            fetch.call();
        } catch (Exception e) {
            logger.warn("Remote repository not available");
        }
    }


    public void trackBranch(CheckoutCommand checkout, String label) {
        checkout.setCreateBranch(true).setName(label).setUpstreamMode(SetupUpstreamMode.TRACK)
                .setStartPoint("origin/" + label);
    }


    /**
     * @param label Branch name
     * @return true if this is a branch (including checking remote branch names).
     */
    public boolean isBranch(String label) {
        return containsBranch(label, ListMode.ALL);
    }


    /**
     * @param label Branch name
     * @return true if this is a local branch.
     */
    public boolean isLocalBranch(String label) {
        return containsBranch(label, null);
    }


    /**
     * @param branchName Name of the branch to look for. 
     * @param listMode
     * @return true if branch exists, false otherwise.
     * @throws GitAPIException
     */
    public boolean containsBranch(String branchName, ListMode listMode) {
        ListBranchCommand command = git.branchList();
        if (listMode != null) {
            command.setListMode(listMode);
        }
        List<Ref> branches;
        try {
            branches = command.call();
        } catch (GitAPIException e) {
            throw new GitRuntimeException(e);
        }
        for (Ref ref : branches) {
            if (ref.getName().endsWith("/" + branchName)) {
                return true;
            }
        }
        return false;
    }


    private void setCredentialsProvider(TransportCommand<?, ?> cmd) {
        if (!Strings.isNullOrEmpty(getUsername())) {
            cmd.setCredentialsProvider(new UsernamePasswordCredentialsProvider(getUsername(), getPassword()));
        }
    }


    public Repository getRepository() {
        return git.getRepository();
    }
}
