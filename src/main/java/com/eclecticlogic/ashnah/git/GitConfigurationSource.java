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

import org.eclipse.jgit.lib.Ref;

import com.netflix.config.PollResult;
import com.netflix.config.PolledConfigurationSource;

/**
 * @author kabram.
 *
 */
public class GitConfigurationSource implements PolledConfigurationSource {

    private EGit git;
    private String label;


    public EGit getGit() {
        return git;
    }


    public void setGit(EGit git) {
        this.git = git;
    }


    public String getLabel() {
        return label;
    }


    public void setLabel(String label) {
        this.label = label;
    }


    public void init() {
        git.getRepository().getConfig().setString("branch", label, "merge", label);
        Ref ref = git.checkout(label);
        if (git.shouldPull(ref)) {
            git.pull(label, ref);
        }
    }


    public PollResult poll(boolean initial, Object checkPoint) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

}
