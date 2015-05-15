package tw.edu.ncu.cc.ncunfc.dummy.OAuth;

import android.content.Context;
import com.android.volley.*;
import com.android.volley.toolbox.Volley;
import com.wuman.android.auth.OAuthManager;

import java.io.IOException;

import tw.edu.ncu.cc.ncunfc.dummy.OAuth.NCUNFCConfig;
// REQUIRE CLASS_READ SCOPE

public class NCUNFCClient {

    private OAuthManager oauthManager;
    private RequestQueue queue;
    private String baseURL;
    private String language;
    private String token;

    public NCUNFCClient(NCUNFCConfig config, OAuthManager oauthManager, Context context) {
        this.baseURL = config.getServerAddress();
        this.language = config.getLanguage();
        this.queue = Volley.newRequestQueue(context);
        this.oauthManager = oauthManager;
    }

    public void initAccessToken() {
        try {
            this.token = oauthManager.authorizeExplicitly("user", null, null).getResult().getAccessToken();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteAccessToken() {
        oauthManager.deleteCredential("user", null, null);
    }
}