package tw.edu.ncu.cc.ncunfc.dummy.OAuth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.api.client.auth.oauth2.Credential;
import com.wuman.android.auth.OAuthManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import tw.edu.ncu.cc.ncunfc.R;

public class NCUNFCClient {
    private static final String ACCESS_TOKEN = "accessToken";
    private static final String REFRESH_TOKEN = "refreshToken";
    private static final String TOKEN_VALID_UNTIL = "tokenValidUntil";
    private static final String NCUSIGN_SETTINGS = "NCUSIGNSettings";

    private static OAuthManager oauthManager;
    public static String refreshToken = "";
    public static String accessToken = "";
    public static long tokenValidUntil = -1;

    public NCUNFCClient(OAuthManager oauthManager) {
        NCUNFCClient.oauthManager = oauthManager;
    }

    public static void initAccessToken(Context context) {
        try {
            SharedPreferences settings = context.getSharedPreferences(NCUSIGN_SETTINGS, Context.MODE_PRIVATE);
            accessToken = settings.getString(ACCESS_TOKEN,"");
            refreshToken = settings.getString(REFRESH_TOKEN, "");
            tokenValidUntil = settings.getLong(TOKEN_VALID_UNTIL, -1);

            if(accessToken.equals("") || refreshToken.equals("") || tokenValidUntil == -1){
                Log.e("debug","auth from begining");

                // get new access Token, refresh Token, expire time and save them into NNCUNFCClient and SharedPreference
                Credential authResult  = oauthManager.authorizeExplicitly("user",null,null).getResult();
                accessToken = authResult.getAccessToken();
                refreshToken = authResult.getRefreshToken();
                tokenValidUntil = authResult.getExpirationTimeMilliseconds();

                settings.edit().putString(ACCESS_TOKEN, accessToken).commit();
                settings.edit().putString(REFRESH_TOKEN, refreshToken).commit();
                settings.edit().putLong(TOKEN_VALID_UNTIL, tokenValidUntil).commit();
            }

            if(System.currentTimeMillis() > tokenValidUntil) {// need to refresh access token
                refreshAccessToken(context);
            }
            Log.e("debug", "Access Token: " + accessToken);
            Log.e("debug", "Refresh Token: " + refreshToken);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteAllToken(Context context) {
        // delete tokens
        oauthManager.deleteCredential("user", null, null);

        // delete Access Token, Refresh Token, Expire time in NCUNFCClient and SharedPreference
        SharedPreferences settings = context.getSharedPreferences(NCUSIGN_SETTINGS, Context.MODE_PRIVATE);
        settings.edit().remove(ACCESS_TOKEN).commit();
        settings.edit().remove(REFRESH_TOKEN).commit();
        settings.edit().remove(TOKEN_VALID_UNTIL).commit();
    }

    public static void refreshAccessToken(final Context context){
        Log.e("debug","refreshing token");
        //refresh access token
        RequestQueue queue = Volley.newRequestQueue(context);

        String refreshURL = "https://api.cc.ncu.edu.tw/oauth/oauth/token";
        queue.add(new StringRequest(Request.Method.POST, refreshURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.e("debug", response);
                        try {
                            JSONObject obj = new JSONObject(response);

                            // save new access token into NCUNFCClient and SharedPreference
                            accessToken = (String) obj.get("access_token");
                            Long tempLong = ((Integer) obj.get("expires_in")).longValue();
                            long templong = tempLong*1000;

                            tokenValidUntil = System.currentTimeMillis() + templong;// save some buffer time

                            SharedPreferences settings = context.getSharedPreferences(NCUSIGN_SETTINGS, Context.MODE_PRIVATE);
                            settings.edit().putString(ACCESS_TOKEN, accessToken).commit();
                            settings.edit().putLong(TOKEN_VALID_UNTIL, tokenValidUntil).commit();

                            Log.e("debug", "refresh access token successed");
                        } catch (JSONException e) {
                            Log.e("debug", e.toString());
                            e.printStackTrace();
                            Log.e("debug", "refresh access token failed, due to json error");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("debug", error.toString());
                        Log.e("debug", "refresh access token failed, due to request error");
                    }
                }
        ) {
            public Map<String, String> getParams() {
                HashMap<String, String> mParams = new HashMap<String, String>();
                mParams.put("grant_type", "refresh_token");
                mParams.put("refresh_token", refreshToken);
                mParams.put("client_id", context.getResources().getString(R.string.clientID));
                mParams.put("client_secret", context.getResources().getString(R.string.clientSecret));
                return mParams;
            }

            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                //headers.put( "X-NCU-API-TOKEN", getResources().getString(R.string.apiToken));
                headers.put("Authorization", "Bearer " + NCUNFCClient.accessToken);
                return headers;
            }
        });
    }

    public static boolean isLoggedIn(Context context){
        SharedPreferences settings = context.getSharedPreferences(NCUSIGN_SETTINGS, Context.MODE_PRIVATE);
        accessToken = settings.getString(ACCESS_TOKEN,"");
        refreshToken = settings.getString(REFRESH_TOKEN, "");
        tokenValidUntil = settings.getLong(TOKEN_VALID_UNTIL, -1);
        if(accessToken.equals("") || refreshToken.equals("") || tokenValidUntil == -1){
            return false;
        }else{
            return true;
        }

    }

    //debug
    public static void setTokenUnvalid(Context context){
        SharedPreferences settings = context.getSharedPreferences(NCUSIGN_SETTINGS, Context.MODE_PRIVATE);
        settings.edit().putLong(TOKEN_VALID_UNTIL, 0).commit();
    }

}