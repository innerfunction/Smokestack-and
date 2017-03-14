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
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.ImageView;

import com.innerfunction.q.Q;
import com.innerfunction.smokestack.AppContainer;
import com.innerfunction.smokestack.content.Authority;
import com.innerfunction.smokestack.content.Provider;
import com.innerfunction.smokestack.form.FormView;
import com.innerfunction.smokestack.form.FormViewController;

import java.util.Map;

/**
 * A form with standard login/logout behaviours.
 *
 * Created by juliangoacher on 14/03/2017.
 */
public class LoginFormViewController extends FormViewController {

    static final String Tag = LoginFormViewController.class.getSimpleName();

    private ImageView backgroundImageView;
    /** The name of the content authority the form is being used to login to. */
    private String authority;
    /** An action to be posted after a successful login. */
    private String onlogin;
    /**
     * A flag indicating that the current user should be logged out when the form is displayed.
     * This is used as part of the UI logout functionality.
     */
    private boolean logout;
    /** A message to be displayed after logout. */
    private String logoutMessage;

    public LoginFormViewController(Context context) {
        super( context );

        FormView form = getForm();
        form.setDelegate( new FormView.Delegate() {
            @Override
            public void onSubmitOk(final FormView form, Map<String,Object> data) {
                Repository repository = getContentRepository();
                if( repository != null ) {
                    form.showSubmittingAppearance( true );
                    repository.loginWithCredentials( data )
                        .then( new Q.Promise.Callback<Boolean, Void>() {
                            @Override
                            public Void result(Boolean result) {
                                form.showSubmittingAppearance( false );
                                postMessage( onlogin );
                                return null;
                            }
                        } )
                        .error( new Q.Promise.ErrorCallback() {
                            @Override
                            public void error(Exception e) {
                                form.showSubmittingAppearance( false );
                                showToast( e.getMessage() );
                            }
                        } );
                }
            }
        });

        backgroundImageView = new ImageView( context );
        LayoutParams layoutParams = new LayoutParams( LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT );
        addView( backgroundImageView, 0, layoutParams );
    }

    public void setBackgroundImage(Drawable image) {
        backgroundImageView.setImageDrawable( image );
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public void setOnlogin(String action) {
        this.onlogin = action;
    }

    public void setLogout(boolean logout) {
        this.logout = logout;
    }

    public void setLogoutMessage(String message) {
        this.logoutMessage = message;
    }

    private Repository getContentRepository() {
        Provider contentProvider = AppContainer.getAppContainer().getContentProvider();
        Authority contentAuthority = contentProvider.getContentAuthority( authority );
        if( contentAuthority instanceof Repository ) {
            return (Repository)contentAuthority;
        }
        Log.w( Tag, String.format("Invalid authority name: %s", authority ) );
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check whether the user is already logged in.
        Repository repository = getContentRepository();
        if( repository.isLoggedIn() ) {
            if( logout) {
                // Logout the current user.
                repository.logout()
                    .then( new Q.Promise.Callback<Boolean, Void>() {
                        @Override
                        public Void result(Boolean result) {
                            if( result && logoutMessage != null ) {
                                showToast( logoutMessage );
                            }
                            return null;
                        }
                    } );
            }
            else {
                postMessage( onlogin );
            }
        }
    }
}
