package com.innerfunction.smokestack.legacy.ui;

import com.innerfunction.scffld.Message;
import com.innerfunction.scffld.app.AppContainer;
import com.innerfunction.scffld.app.ViewController;
import com.innerfunction.scffld.app.ViewControllerBehaviour;
import com.innerfunction.smokestack.legacy.WPContentContainer;

/**
 * A view behaviour which checks whether a user is currently logged into the app.
 * If a user is logged in then posts an action message. Useful for skipping the login form at
 * startup.
 *
 * Attached by juliangoacher on 12/07/16.
 */
public class WPContentLoginBehaviour implements ViewControllerBehaviour {

    private WPContentContainer contentContainer;
    private String loginAction;

    public WPContentLoginBehaviour() {}

    public WPContentLoginBehaviour(WPContentContainer container, String loginAction) {
        this.contentContainer = container;
        this.loginAction = loginAction;
    }

    public void setContainer(WPContentContainer container) {
        this.contentContainer = container;
    }

    public void setLoginAction(String action) {
        this.loginAction = action;
    }

    @Override
    public void onResume(ViewController view) {
        // Check if user already logged in, if so then dispatch a specified event.
        if( contentContainer.getAuthManager().isLoggedIn() ) {
            AppContainer.getAppContainer().postMessage( loginAction, view );
        }
    }

    @Override
    public boolean receiveMessage(Message message, Object sender) {
        return false;
    }
}
