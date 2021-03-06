// Copyright 2016 InnerFunction Ltd.
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
package com.innerfunction.smokestack.legacy;

import android.text.TextUtils;
import android.util.Log;

import com.innerfunction.http.AuthenticationDelegate;
import com.innerfunction.http.Client;
import com.innerfunction.http.Response;
import com.innerfunction.scffld.app.AppContainer;
import com.innerfunction.q.Q;
import com.innerfunction.util.KeyPath;
import com.innerfunction.util.Null;
import com.innerfunction.util.Paths;
import com.innerfunction.util.UserDefaults;

import static com.innerfunction.util.DataLiterals.*;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class for managing user authentication via Wordpress login and registration.
 * Attached by juliangoacher on 08/07/16.
 */
public class WPAuthManager implements AuthenticationDelegate {

    static final String Tag = WPAuthManager.class.getSimpleName();

    private WPContentContainer container;
    private List<String> profileFieldNames;
    private UserDefaults userDefaults;

    public WPAuthManager(WPContentContainer container) {
        this.container = container;
        this.profileFieldNames = Arrays.asList( "ID", "first_name", "last_name", "user_email" );
        this.userDefaults = AppContainer.getAppContainer().getUserDefaults();
    }

    private String getFeedURL() {
        return container.getFeedURL();
    }

    public String getLoginURL() {
        return Paths.join( getFeedURL(), "account/login");
    }

    public String getRegistrationURL() {
        return Paths.join( getFeedURL(), "account/create");
    }

    public String getCreateAccountURL() {
        return getRegistrationURL();
    }

    public String getProfileURL() {
        return Paths.join( getFeedURL(), "account/profile");
    }

    public void setProfileFieldNames(List<String> names) {
        this.profileFieldNames = names;
    }

    public String getWPRealmKey(String key) {
        return String.format("%s/%s", container.getWpRealm(), key);
    }

    public boolean isLoggedIn() {
        return userDefaults.getBoolean( getWPRealmKey("logged-in") );
    }

    public void storeUserCredentials(Map<String, Object> values) {
        String username = KeyPath.getValueAsString("user_login", values);
        String password = KeyPath.getValueAsString("user_pass", values);
        // NOTE this will work for all forms - login, create account + update profile. In the latter
        // case, if the password is not updated then password will be empty and the keystore won't
        // be updated.
        if( username != null && username.length() > 0 && password != null && password.length() > 0 ) {
            userDefaults.set( m(
                kv( getWPRealmKey("user_login"), username ),
                kv( getWPRealmKey("user_pass"), password ),
                kv( getWPRealmKey("logged-in"), true )
            ));
        }
    }

    public void storeUserProfile(Map<String, Object> values) {
        // Store standard profile values.
        Map<String, Object> profileValues = new HashMap<>();
        for( String key : profileFieldNames ) {
            Object value = KeyPath.getValueAsString( key, values );
            String profileKey = getWPRealmKey( key );
            if( value != null ) {
                profileValues.put( profileKey, value);
            }
            else {
                profileValues.put( profileKey, Null.Placeholder);
            }
        }
        // Search for and store any meta data values.
        List<String> metaKeys = new ArrayList<>();
        for( String key : values.keySet() ) {
            if( key.startsWith("meta_") ) {
                Object value = values.get( key );
                String profileKey = getWPRealmKey( key );
                profileValues.put( profileKey, value );
                metaKeys.add( key );
            }
        }
        // Store list of meta-data keys.
        String metaDataKeys = TextUtils.join(",", metaKeys);
        String profileKey = getWPRealmKey("metaDataKeys");
        profileValues.put( profileKey, metaDataKeys );
        // Store values.
        userDefaults.set( profileValues );
    }

    public Map<String, Object> getUserProfile() {
        Map<String, Object> values = new HashMap();
        String profileKey = getWPRealmKey("user_login");
        values.put("user_login", userDefaults.getString( profileKey ) );
        // Read standard profile fields.
        for( String name : profileFieldNames ) {
            profileKey = getWPRealmKey( name );
            Object value = userDefaults.get( profileKey );
            if( value != null ) {
                values.put( name, value );
            }
        }
        // Read profile meta-data.
        profileKey = getWPRealmKey("metaDataKeys");
        String[] metaDataKeys = TextUtils.split( userDefaults.getString(profileKey), ",");
        for( String metaDataKey : metaDataKeys ) {
            profileKey = getWPRealmKey( metaDataKey );
            Object value = userDefaults.get( profileKey );
            if( value != null ) {
                values.put( metaDataKey, value );
            }
        }
        // Return result.
        return values;
    }

    public String getUsername() {
        return userDefaults.getString( getWPRealmKey("user_login") );
    }

    public void logout() {
        userDefaults.set( getWPRealmKey("logged-in"), false );
    }

    public void showPasswordReminder() {
        // Fetch the password reminder URL from the server.
        String url = Paths.join( getFeedURL(), "account/password-reminder");
        try {
            container.getHTTPClient().get( url )
                .then( new Q.Promise.Callback<Response, Response>() {
                    @Override
                    public Response result(Response response) {
                        Map<String, Object> data = (Map<String,Object>)response.parseBodyData();
                        String reminderURL = KeyPath.getValueAsString( "lost_password_url", data );
                        if( reminderURL != null ) {
                            // Open the URL in the device browser.
                            AppContainer.getAppContainer().openURL( reminderURL );
                        }
                        return response;
                    }
                } );
        }
        catch(MalformedURLException e) {
            Log.e(Tag, String.format("Bad URL: %s", url ));
        }
    }

    private void handleAuthenticationFailure() {
        String message = "Authentication%20failure";
        String toastAction = String.format("post:toast+message=%s", message );
        AppContainer.getAppContainer().postMessage( toastAction, container );
        container.showLoginForm();
    }

    @Override
    public boolean isAuthenticationErrorResponse(Client client, Response response) {
        String requestURL = response.getRequestURL();
        // Note that authentication failures returned by login don't count as authentication errors
        // here.
        return response.getStatusCode() == 401 && !requestURL.equals( getLoginURL() );
    }

    @Override
    public Q.Promise<Response> authenticateUsingHTTPClient(Client client) {
        final Q.Promise<Response> promise = new Q.Promise<>();
        // Read username and password from local storage and keychain.
        String username = userDefaults.getString( getWPRealmKey("user_login") );
        String password = userDefaults.getString( getWPRealmKey("user_pass") );
        if( username != null && password != null ) {
            // Submit a new login request.
            Map<String,Object> data = m(
                kv("user_login", username ),
                kv("user_pass",  password )
            );
            try {
                client.post( getLoginURL(), data )
                    .then( new Q.Promise.Callback<Response, Response>() {
                        public Response result(Response response) {
                            int statusCode = response.getStatusCode();
                            if( statusCode == 200 || statusCode == 201 ) {
                                promise.resolve( response );
                            }
                            else {
                                handleAuthenticationFailure();
                                promise.reject( String.format( "Authentication failure: Status code %d", statusCode ) );
                            }
                            return response;
                        }
                    } )
                    .error( new Q.Promise.ErrorCallback() {
                        public void error(Exception e) {
                            handleAuthenticationFailure();
                            promise.reject( e );
                        }
                    } );
            }
            catch(MalformedURLException e) {
                handleAuthenticationFailure();
                promise.reject( e );
            }
        }
        else {
            handleAuthenticationFailure();
            promise.reject("Authentication failure: Username and password not available");
        }
        return promise;
    }

}
