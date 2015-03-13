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
import java.nio.file.Files;

import org.eclipse.jgit.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic utility methods that are "git" independent are here to reduce "noise" in egit implementation. 
 * 
 * @author kabram.
 *
 */
public abstract class AbstractEGit<T extends AbstractEGit<?>> {

    protected File basedir;

    private String uri;
    private String username;
    private String password;

    private static final Logger logger = LoggerFactory.getLogger(AbstractEGit.class);


    public String getUri() {
        return uri;
    }


    @SuppressWarnings("unchecked")
    public T setUri(String uri) {
        this.uri = uri;
        return (T) this;
    }


    public String getUsername() {
        return username;
    }


    @SuppressWarnings("unchecked")
    public T setUsername(String username) {
        this.username = username;
        return (T) this;
    }


    public String getPassword() {
        return password;
    }


    @SuppressWarnings("unchecked")
    public T setPassword(String password) {
        this.password = password;
        return (T) this;
    }


    protected File createBaseDir() {
        try {
            final File basedir = Files.createTempDirectory("config-repo-").toFile();
            Runtime.getRuntime().addShutdownHook(new Thread() {

                @Override
                public void run() {
                    try {
                        FileUtils.delete(basedir, FileUtils.RECURSIVE);
                    } catch (IOException e) {
                        logger.warn("Failed to delete temporary directory on exit: " + e);
                    }
                }
            });
            return basedir;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create temp dir", e);
        }
    }


    protected File getWorkingDirectory() {
        if (uri.startsWith("file:")) {
            try {
                URL url = new URL(PathUtil.cleanPath(uri));
                URI uri = new URI(url.toString().replaceAll(" ", "%20"));
                return new File(uri.getSchemeSpecificPart());
            } catch (Exception e) {
                throw new GitRuntimeException("Cannot convert uri to file: " + uri);
            }
        }
        return basedir;
    }


    protected void deleteBaseDirIfExists() {
        if (basedir.exists()) {
            try {
                FileUtils.delete(basedir, FileUtils.RECURSIVE);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to initialize base directory", e);
            }
        }
    }


    protected void verify(boolean result, String message) {
        if (!result) {
            throw new GitRuntimeException(message);
        }
    }
}
