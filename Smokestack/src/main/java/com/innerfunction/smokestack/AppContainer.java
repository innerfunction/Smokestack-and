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

import com.innerfunction.smokestack.content.ContentScheme;
import com.innerfunction.smokestack.content.Provider;

import static com.innerfunction.util.DataLiterals.kv;
import static com.innerfunction.util.DataLiterals.m;

/**
 * The Smokestack content app container.
 *
 * Created by juliangoacher on 07/03/2017.
 */
public class AppContainer extends com.innerfunction.scffld.app.AppContainer {

    private Provider contentProvider;
    private ContentScheme contentScheme = new ContentScheme();

    public AppContainer(Context context) {
        super( context );
        uriHandler.addHandlerForScheme("content", contentScheme );
    }

    public Provider getContentProvider() {
        return contentProvider;
    }

    public void setContentProvider(Provider provider) {
        this.contentProvider = provider;
        contentScheme.setContentProvider( provider );
    }

    /** The app container's singleton instance. */
    static AppContainer Instance;

    public static AppContainer initialize(Context context, Object configuration) {
        Instance = new AppContainer( context );
        com.innerfunction.scffld.app.AppContainer.initialize( Instance, configuration );
        return Instance;
    }

    public static AppContainer getAppContainer() {
        if( Instance == null ) {
            throw new IllegalStateException("App container not initialized");
        }
        return Instance;
    }

}
