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

import com.innerfunction.util.Paths;

/**
 * A class representing a Smokestack content repository's settings.
 * TODO: Support username + password settings also.
 *
 * Created by juliangoacher on 08/03/2017.
 */
public class Settings {

    public static final String SmokestackAPIVersion     = "0.2";
    public static final String SmokestackAPIRoot        = "semop"; // TODO Change
    public static final String SmokestackAPIProtocol    = "http";

    /** The CMS host name. */
    private String host;
    /** The CMS port number. */
    private int port;
    /** The CMS account name. */
    private String account;
    /** The CMS repo name. */
    private String repo;
    /** The CMS branch name. */
    private String branch;
    /** The CMS HTTP authentication realm. */
    private String authRealm;
    /** The CMS path root. */
    private String pathRoot;
    /** The CMS protocol, e.g. HTTP or HTTPS. */
    private String protocol;

    public Settings() {
        this.pathRoot = Paths.join( SmokestackAPIRoot, SmokestackAPIVersion );
        this.protocol = SmokestackAPIProtocol;
        this.port = 0;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getAccount() {
        return account;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public String getRepo() {
        return repo;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getBranch() {
        return branch;
    }

    public void setAuthRealm(String authRealm) {
        this.authRealm = authRealm;
    }

    public String getAuthRealm() {
        if( authRealm == null ) {
            String branch = this.branch == null ? "master" : this.branch;
            authRealm = String.format("Smokestack/%s/%s/%s", account, repo, branch );
        }
        return authRealm;
    }

    public void setPathRoot(String pathRoot) {
        this.pathRoot = pathRoot;
    }

    public String getPathRoot() {
        return pathRoot;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getProtocol() {
        return protocol;
    }

    /** Return the URL for login authentication. */
    public String getURLForAuthentication() {
        return getURLForPath( getPathForResource("authenticate", null ) );
    }

    /** Return the URL for the updates feed. */
    public String getURLForUpdates() {
        return getURLForPath( getPathForResource("updates", null ) );
    }

    /** Return the URL for downloading a fileset of the specified category. */
    public String getURLForFileset(String category) {
        return getURLForPath( getPathForResource("filesets", category ) );
    }

    /** Return the URL for downloading a file at the specified path. */
    public String getURLForFile(String path) {
        return getURLForPath( getPathForResource("files", path ) );
    }

    /** Get the API's base URL. Used as the HTTP authentication protection space. */
    public String getAPIBaseURL() {
        return getURLForPath("");
    }

    private String getPathForResource(String resourceName, String trailing) {
        // http://{host}/{apiroot}/{apiver}/path
        String path = Paths.join( pathRoot, resourceName, account, repo );
        if( branch != null ) {
            path = Paths.join( path, "~"+branch );
        }
        if( trailing != null ) {
            path = Paths.join( path, trailing );
        }
        return path;
    }

    private String getURLForPath(String path) {
        String hostSuffix = port == 0 ? "" : ":"+Integer.toString( port );
        return String.format("%s//%s%s/%s", protocol, host, hostSuffix, path );
    }

}
