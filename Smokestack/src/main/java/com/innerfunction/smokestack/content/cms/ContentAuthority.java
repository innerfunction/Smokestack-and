// Copyright 2017 InnerFunction Ltd.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License
package com.innerfunction.smokestack.content.cms;

import android.content.Context;

import com.innerfunction.smokestack.content.AbstractAuthority;
import com.innerfunction.smokestack.content.AuthenticationManager;

import java.net.PasswordAuthentication;
import java.net.URL;

/**
 * A class representing a content repository in the Smokestack CMS.
 * TODO: Should this be called Repository? ContentRepository?
 * Created by juliangoacher on 09/03/2017.
 */
public class ContentAuthority extends AbstractAuthority {

    private Settings cms;
    private AuthenticationManager authManager;
    private String logoutAction;
    private FileDB fileDB;

    public ContentAuthority(Context context) {
        super( context );
    }

    public void setCMS(Settings cms) {
        this.cms = cms;
    }

    public Settings getCMS() {
        return cms;
    }

    public AuthenticationManager getAuthManager() {
        return authManager;
    }

    public void setLogoutAction(String logoutAction) {
        this.logoutAction = logoutAction;
    }

    public String getLogoutAction() {
        return logoutAction;
    }

    public void setFileDB(FileDB fileDB) {
        this.fileDB = fileDB;
    }

    public FileDB getFileDB() {
        return fileDB;
    }

    @Override
    public void refreshContent() {

    }

    @Override
    public PasswordAuthentication getPasswordAuthentication(String realm, URL url) {
        // Return the user's stored credentials, if any, if the request is for this authority's
        // configured HTTP authentication realm.
        if( realm != null && realm.equals( getCMS().getAuthRealm() ) ) {
            return getAuthManager().getPasswordAuthentication();
        }
        return null;
    }

}
