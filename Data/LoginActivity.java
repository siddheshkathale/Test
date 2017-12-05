package com.kudize.Activities;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Color;
import android.os.Build;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.facebook.login.widget.ProfilePictureView;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.kudize.App.MyApplication;
import com.kudize.Model.FriendPojo;
import com.kudize.R;
import com.kudize.Services.LoginService;
import com.kudize.gcm.GcmIntentService;
import com.kudize.helper.CheckConnection;
import com.kudize.helper.Constant;
import com.kudize.helper.MyLocation;
import com.kudize.helper.PrefManager;
import com.kudize.helper.URLList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class LoginActivity extends AppCompatActivity
{
    CallbackManager callbackManager;
    //Button share,details;
    Button btnLogout;
    ShareDialog shareDialog;
    LoginButton login;
    ProfilePictureView profile;
    Dialog details_dialog;
    TextView details_txt;
    private String facebook_id,f_name, m_name, l_name, gender, profile_image, full_name, email_id;
    CoordinatorLayout coordinatorLayout;
    Toolbar toolbar;
    //private PrefManager prefManager;
    public static String ProfileLink = "";
    public static String ProfileName = "";
    String facebookId = "";
    private RequestQueue mRequestQueue;
    MyLocation myLocation;
    List<FriendPojo> friendPojoList = new ArrayList<>();
    ProgressBar loader;
    //private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //PrefSetting();
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        mRequestQueue = Volley.newRequestQueue(getApplicationContext());

        setView();
        getKeyHash();
        setListener();

    }

    void setView()
    {
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
        callbackManager     = CallbackManager.Factory.create();

        login               = (LoginButton)findViewById(R.id.login_button);
        profile             = (ProfilePictureView)findViewById(R.id.picture);
        btnLogout           = (Button) findViewById(R.id.btnLogout);

        loader  = (ProgressBar) findViewById(R.id.loader);


        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Kudize");


        //shareDialog = new ShareDialog(this);

        //share       = (Button)findViewById(R.id.share);
        //details     = (Button)findViewById(R.id.details);

        try
        {
            login.setReadPermissions("public_profile email user_photos user_friends"); // @[@"public_profile", @"email", @"user_friends",@"user_photos"];
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void getKeyHash()
    {
        PackageInfo info;
        try
        {
            info = getPackageManager().getPackageInfo("com.kudize", PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures)
            {
                MessageDigest md;
                md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String something = new String(Base64.encode(md.digest(), 0));
                //String something = new String(Base64.encodeBytes(md.digest()));
                Log.e("@@ hash key", something);
            }
        }
        catch (PackageManager.NameNotFoundException e1)
        {
            //Log.e("@@ name not found", e1.toString());
        }
        catch (NoSuchAlgorithmException e)
        {
            //Log.e("@@ no such an algorithm", e.toString());
        }
        catch (Exception e)
        {
            //Log.e("@@ exception", e.toString());
        }
    }

    void setListener()
    {

            login.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    try
                    {
                        if (CheckConnection.isConnected(LoginActivity.this))
                        {
                            System.out.println("@@ Connected");
                        }
                        else
                        {
                            System.out.println("@@ Not Connected");
                            Snackbar snackbar = Snackbar
                                    .make(coordinatorLayout, R.string.NoConnection, Snackbar.LENGTH_LONG)
                                    .setAction("RETRY", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                        }
                                    });

                            // Changing message text color
                            snackbar.setActionTextColor(Color.RED);

                            // Changing action button text color
                            View sbView = snackbar.getView();
                            TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
                            textView.setTextColor(Color.YELLOW);

                            snackbar.show();
                        }

                        if (AccessToken.getCurrentAccessToken() != null)
                        {
                            RequestData();
                            profile.setProfileId(null);
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            });
            login.registerCallback(callbackManager, new FacebookCallback<LoginResult>()
            {
                @Override
                public void onSuccess(LoginResult loginResult)
                {
                    try
                    {
                        System.out.println("@@ registerCallback onSuccess");
                        facebook_id = f_name = m_name = l_name = gender = profile_image = full_name = email_id = "";

                        if (AccessToken.getCurrentAccessToken() != null)
                        {
                            /*if(!loader.isShown())
                            {
                                loader.setVisibility(View.VISIBLE);
                                getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                            }*/

                            if (CheckConnection.isConnected(LoginActivity.this))
                            {
                                Intent intent = new Intent(LoginActivity.this,LoginService.class);
                                intent.putExtra("Token",AccessToken.getCurrentAccessToken().getToken());
                                startActivity(intent);
                            }


                            RequestData();

                            System.out.println("@@ 1token:: " + AccessToken.getCurrentAccessToken().getToken());


                            //sendToken(AccessToken.getCurrentAccessToken().getToken());

                            MyApplication.getInstance().getprefManager().setLogin(true);
                            startActivity(new Intent(LoginActivity.this,MainActivity.class));
                            finish();

                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onCancel()
                {
                    System.out.println("@@ onCancel");

                    Snackbar snackbar = Snackbar
                            .make(coordinatorLayout, "Slow internet connection!", Snackbar.LENGTH_LONG)
                            .setAction("RETRY", new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                }
                            });

                    // Changing message text color
                    snackbar.setActionTextColor(Color.RED);

                    // Changing action button text color
                    View sbView = snackbar.getView();
                    TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
                    textView.setTextColor(Color.YELLOW);

                    snackbar.show();

                }

                @Override
                public void onError(FacebookException exception) {
                    System.out.println("@@ onError:: " + exception.toString());
                }
            });


            btnLogout.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    //LoginManager.getInstance().logOut();
                }
            });
    }

    public void RequestData()
    {
        try
        {
            GraphRequest request = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                @Override
                public void onCompleted(JSONObject object, GraphResponse response) {

                    JSONObject json = response.getJSONObject();
                    //System.out.println("@@ Json data :"+json);
                    //Log.d("@@ object", object.toString());
                    //Log.d("@@ Data", json.toString());
                    try
                    {
                        if (json != null)
                        {
                            //System.out.println("@@ Json data :"+json);

                            //Constant.ProfileName = json.getString("name");
                            MyApplication.getInstance().getprefManager().setProfileName(json.getString("name"));
                            MyApplication.getInstance().getprefManager().setfacebookUserId(json.getString("id"));
                            //Constant.facebookUserId = json.getString("id");
                            //System.out.println("@@ facebookUserId:: " + MyApplication.getInstance().getprefManager().getfacebookUserId());
                            MyApplication.getInstance().getprefManager().setProfileLink("https://graph.facebook.com/"+MyApplication.getInstance().getprefManager().getfacebookUserId()+"/picture?type=large");
                            //System.out.println("@@ ProfileLink:: " + MyApplication.getInstance().getprefManager().getProfileLink());


                        }

                    }
                    catch (JSONException e)
                    {
                        e.printStackTrace();
                        //System.out.println("@@ innner e.getMessage()::" + e.getMessage());
                    }
                }
            });
            Bundle parameters = new Bundle();
            //parameters.putString("fields", "id,name,link,email,picture,albums.limit(1){photos{picture}}");//me?fields=id,name,albums{photos{picture}}
            //parameters.putString("fields", "id,name,link,email,picture,albums.limit(1){photos{picture}}");//me?fields=id,name,albums{photos{picture,name}}
            parameters.putString("fields", "id,name,link,email,picture");//me?fields=id,name,albums{photos{picture,name}}
            request.setParameters(parameters);
            request.executeAsync();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            //System.out.println("@@ outer e.getMessage()::" + e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
        //System.out.println("@@ onActivityResult");
    }

    void sendToken(final String token)
    {
        if(!CheckConnection.isConnected(this))
        {
            Toast.makeText(this,R.string.NoConnection,Toast.LENGTH_LONG).show();
            if(loader.isShown())
            {
                loader.setVisibility(View.GONE);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            }
            return;
        }

        StringRequest strReq = new StringRequest(Request.Method.POST,
                URLList.TOKEN, new Response.Listener<String>()
        {

            @Override
            public void onResponse(String response)
            {
                //Log.e("@@ response: " , response);

                if(MyApplication.getInstance().getprefManager().getfacebookUserId() != null)
                {
                    if (checkPlayServices())
                    {
                        registerGCM();
                    }
                }


                try
                {
                    JSONObject jsonObject = new JSONObject(response);

                    if (jsonObject.optString("isError").equals("false"))
                    {
                        MyApplication.getInstance().getprefManager().setPlaceImage(jsonObject.optString("photoreference"));
                    }
                    else
                    {
                        //System.out.println("@@ LoginActiivty Error Occur");
                    }
                    //Constant.PlaceImage = jsonObject.optString("photoreference");



                }
                catch (Exception e)
                {
                    e.getMessage();
                    //System.out.println("@@ e.getMessage():: " + e.getMessage());
                    if(loader.isShown())
                    {
                        loader.setVisibility(View.GONE);
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    }
                }


                MyApplication.getInstance().getprefManager().setLogin(true);
                startActivity(new Intent(LoginActivity.this,MainActivity.class));
                finish();
                if(loader.isShown())
                {
                    loader.setVisibility(View.GONE);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                }


            }
        }, new Response.ErrorListener()
        {
            @Override
            public void onErrorResponse(VolleyError error)
            {
                NetworkResponse networkResponse = error.networkResponse;
                Log.e("@@ Volley error: " , error.getMessage() + ", code: " + networkResponse);
                Toast.makeText(getApplicationContext(), "Volley error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                if(loader.isShown())
                {
                    loader.setVisibility(View.GONE);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                }
            }
        })
        {
            @Override
            protected Map<String, String> getParams()
            {
                Map<String, String> params = new HashMap<>();
                params.put("AccessToken", token);
                params.put("lattitude", "19.1783");
                params.put("longitude", "72.8337");
                params.put("flag", "1");
                Log.e("@@ params: " , params.toString());
                return params;
            }
        };

        //Adding request to request queue
        //MyApplication.getInstance().addToRequestQueue(strReq);
        mRequestQueue.add(strReq);
    }

    // starting the service to register with GCM
    private void registerGCM()
    {
        try
        {
            Intent intent = new Intent(this, GcmIntentService.class);
            intent.putExtra("key", "register");
            startService(intent);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private boolean checkPlayServices()
    {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS)
        {
            if (apiAvailability.isUserResolvableError(resultCode))
            {
                apiAvailability.getErrorDialog(this, resultCode, Constant.PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            }
            else
            {
                //Log.i("@@ This device is not supported. Google Play Services not installed!");
                System.out.println("@@ This device is not supported. Google Play Services not installed!");
                Toast.makeText(this, "This device is not supported. Google Play Services not installed!", Toast.LENGTH_LONG).show();
                //finish();
            }
            return false;
        }
        return true;
    }


}
