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
package com.innerfunction.smokestack;

import android.content.Context;

import com.innerfunction.smokestack.content.Provider;

import static com.innerfunction.util.DataLiterals.kv;
import static com.innerfunction.util.DataLiterals.m;

/**
 * Created by juliangoacher on 07/03/2017.
 */

public class AppContainer extends com.innerfunction.scffld.app.AppContainer {

    private Provider contentProvider;

    public AppContainer(Context context) {
        super( context );
    }

    public Provider getContentProvider() {
        return contentProvider;
    }

    public void setContentProvider(Provider provider) {
        this.contentProvider = provider;
    }

    /** The app container's singleton instance. */
    static AppContainer Instance;

    public static synchronized AppContainer getAppContainer(Context context) {
        if( Instance == null ) {
            Instance = new AppContainer( context );
            Instance.addTypes( CoreTypes );
            Instance.loadConfiguration( m(
                kv("types",             "@app:/SCFFLD/types.json"),
                kv("schemes",           "@dirmap:/SCFFLD/schemes"),
                kv("patterns",          "@dirmap:/SCFFLD/patterns"),
                kv("nameds",            "@dirmap:/SCFFLD/nameds"),
                kv("contentProvider", m(
                    kv("authorities",   "@dirmap:/SCFFLD/cas")
                ))
            ));
        }
        return Instance;
    }

    public static ContentAppContainer getAppContainer() {
        return Instance;
    }

}
