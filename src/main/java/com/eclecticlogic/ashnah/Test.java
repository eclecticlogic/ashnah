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
package com.eclecticlogic.ashnah;

import com.eclecticlogic.ashnah.git.EGit;
import com.eclecticlogic.ashnah.git.GitConfigurationSource;


/**
 * @author kabram.
 *
 */
public class Test {

    
    public static void main(String[] args) {
        EGit git = new EGit().setUri("git@github.com:eclecticlogic/whisper.git");
        git.initialize();
        GitConfigurationSource source = new GitConfigurationSource();
        source.setGit(git);
        source.setLabel("master");
        source.init();
    }
}
