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

import com.innerfunction.util.KeyPath;
import com.innerfunction.util.UserDefaults;

import java.net.PasswordAuthentication;
import java.util.Map;

import static com.innerfunction.util.DataLiterals.*;

/**
 * A class for storing CMS user authentication credentials.
 *
 * Created by juliangoacher on 11/03/2017.
 */
public class AuthenticationManager {

    /** Local storage. */
    private UserDefaults userDefaults;
    /** HTTP authentication credentials. */
    private PasswordAuthentication passwordAuth;

    public AuthenticationManager(Context context) {
        userDefaults = new UserDefaults( context, "Smokestack.auth");
        loadPasswordAuthentication();
    }

    public void setCMSSettings(Settings settings) {
        // TODO Load preconfigured username/password from settings
    }

    /**
     * Register basic auth credentials to be used with subsequent requests.
     * The credentials map argument should have username and password values.
     */
    public void registerCredentials(Map<String,Object> credentials) {
        String username = KeyPath.getValueAsString("username", credentials );
        String password = KeyPath.getValueAsString("password", credentials );
        if( username != null && password != null ) {
            userDefaults.set( credentials );
        }
        loadPasswordAuthentication();
    }

    /** Register basic auth credentials to be used with subsequent requests. */
    public void registerCredentials(String username, String password) {
        if( username != null && password != null ) {
            userDefaults.set( m(
                kv("username", username ),
                kv("password", password )
            ));
        }
        loadPasswordAuthentication();
    }

    /** Delete any stored user credentials */
    public void removeCredentials() {
        userDefaults.remove("username","password");
        loadPasswordAuthentication();
    }

    /** Check whether any user credentials are stored. */
    public boolean hasCredentials() {
        return userDefaults.get("username") != null && userDefaults.get("password") != null;
    }

    /** Return user credentials as a PasswordAuthentication instance. */
    public PasswordAuthentication getPasswordAuthentication() {
        return passwordAuth;
    }

    /** Load HTTP authentication credentials from local storage. */
    private void loadPasswordAuthentication() {
        passwordAuth = null;
        String username = userDefaults.getString("username");
        String password = userDefaults.getString("password");
        if( username != null && password != null ) {
            passwordAuth = new PasswordAuthentication( username, password.toCharArray() );
        }
    }

}
