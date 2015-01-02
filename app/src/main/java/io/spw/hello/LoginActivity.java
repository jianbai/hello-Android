package io.spw.hello;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphUser;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Created by scottwang on 12/26/14.
 * ga0RGNYHvNM5d0SLGQfpQWAPGJ8=
 * n+xcAaOIG1e1XpxStAc4PkDDnXM
 */

public class LoginActivity extends Activity {

    public static final String TAG = LoginActivity.class.getSimpleName();

    private ProgressBar mProgressSpinner;
    private Button mLoginButton;

    private ParseUser currentUser;
    private Boolean noGender;
    private Boolean noAge;
    private Boolean noHometown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mProgressSpinner = (ProgressBar) findViewById(R.id.login_progress_spinner);
        mLoginButton = (Button) findViewById(R.id.button_facebook_login);

        Log.d(TAG, "got to login");

        currentUser = ParseUser.getCurrentUser();
        if ((currentUser != null) && ParseFacebookUtils.isLinked(currentUser)) {
            Log.d(TAG, "user already logged in");
        } else {
            Log.d(TAG, "user logged out");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ParseFacebookUtils.finishAuthentication(requestCode, resultCode, data);
    }

    public void onLoginButtonClicked(View v) {
        showProgressSpinner();

        List<String> permissions = Arrays.asList("public_profile", "user_hometown", "user_birthday", "email");

        ParseFacebookUtils.logIn(permissions, this, new LogInCallback() {
            @Override
            public void done(ParseUser parseUser, ParseException e) {
                if (parseUser == null) {
                    Log.d(TAG, "User cancelled Facebook login :(");
                    Log.d(TAG, e.getLocalizedMessage());
                    hideProgressSpinner();
                } else if (parseUser.isNew()) {
                    Log.d(TAG, "User signed up AND logged in through Facebook :)");
                    fetchFacebookData();
                } else {
                    Log.d(TAG, "User logged in through Facebook :)");
                    hideProgressSpinner();
                    navigateToMain();
                }
            }
        });
    }

    private void fetchFacebookData() {
        Session session = ParseFacebookUtils.getSession();
        if (session != null && session.isOpened()) {
            makeMeRequest();
        }
    }

    private void makeMeRequest() {
        Request request = Request.newMeRequest(ParseFacebookUtils.getSession(),
                new Request.GraphUserCallback() {
                    @Override
                    public void onCompleted(GraphUser user, Response response) {
                        if (user != null) {
                            updateUserBasicProperties(user);
                            updateUserGender(user);
                            updateUserAge(user);
                            updateUserHometown(user);

                            // Save user info
                            currentUser.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {
                                    hideProgressSpinner();
                                    initializeBooleans();

                                    if (isUserProfileIncomplete()) {
                                        navigateToSetProfile();
                                    } else {
                                        navigateToMain();
                                    }
                                }
                            });
                        } else if (response.getError() != null) {
                            Log.d(TAG, response.getError().getErrorMessage());
                            showLoginFailedDialog();
                        }
                    }
                });
        request.executeAsync();
    }

    private void updateUserHometown(GraphUser user) {
        if (user.getProperty(ParseConstants.KEY_HOMETOWN) != null) {
            JSONObject h =
                    (JSONObject) user.getProperty(ParseConstants.KEY_HOMETOWN);

            try {
                currentUser.put(ParseConstants.KEY_HOMETOWN,
                        h.getString(ParseConstants.KEY_HOMETOWN_NAME));
            } catch (JSONException e) {
                Log.d(TAG, e.getLocalizedMessage());
            }

        }
    }

    private void updateUserBasicProperties(GraphUser user) {
        currentUser.put(ParseConstants.KEY_FACEBOOK_ID, user.getId());
        currentUser.put(ParseConstants.KEY_FIRST_NAME, user.getFirstName());
        currentUser.put(ParseConstants.KEY_LAST_NAME, user.getLastName());
        currentUser.put(ParseConstants.KEY_AGE_SPREAD, 0);
        currentUser.put(ParseConstants.KEY_GENDER_SPREAD, 0);

        if (user.getProperty(ParseConstants.KEY_EMAIL) != null) {
            currentUser.put(ParseConstants.KEY_EMAIL,
                    (String) user.getProperty(ParseConstants.KEY_EMAIL));
        }
    }

    private void updateUserGender(GraphUser user) {
        if (user.getProperty(ParseConstants.KEY_GENDER) != null) {
            currentUser.put(ParseConstants.KEY_GENDER,
                    (String) user.getProperty(ParseConstants.KEY_GENDER));
        }
    }

    private void updateUserAge(GraphUser user) {
        if (user.getBirthday() != null) {
            String birthday = (String) user.getBirthday();
            currentUser.put(ParseConstants.KEY_BIRTHDAY, birthday);

            try {
                String age = calculateAge(birthday);
                currentUser.put(ParseConstants.KEY_AGE, age);
            } catch (java.text.ParseException e) {
                Log.d(TAG, e.getLocalizedMessage());
            }

        }
    }

    private String calculateAge(String birthday) throws java.text.ParseException {
        int age;

        Date date = new SimpleDateFormat("MM/dd/yyyy").parse(birthday);
        Date now = new Date();

        GregorianCalendar cal1 = new GregorianCalendar();
        GregorianCalendar cal2 = new GregorianCalendar();
        cal1.setTime(date);
        cal2.setTime(now);

        int factor = 0;

        if (cal2.get(Calendar.DAY_OF_YEAR) < cal1.get(Calendar.DAY_OF_YEAR)) {
            factor = -1;
        }

        age = cal2.get(Calendar.YEAR) - cal1.get(Calendar.YEAR) + factor;

        return String.valueOf(age);
    }

    private void initializeBooleans() {
        noGender = currentUser.getString(ParseConstants.KEY_GENDER) == null;
        noAge = currentUser.getString(ParseConstants.KEY_AGE) == null;
        noHometown = currentUser.getString(ParseConstants.KEY_HOMETOWN) == null;
    }

    private Boolean isUserProfileIncomplete() {
        return (noGender || noAge || noHometown);
    }

    private void navigateToSetProfile() {
        Intent intent = new Intent(this, SetProfileActivity.class);
        intent.putExtra("noGender", noGender);
        intent.putExtra("noAge", noAge);
        intent.putExtra("noHometown", noHometown);
        startActivity(intent);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private void showLoginFailedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.login_failed_message)
                .setTitle(R.string.login_failed_title)
                .setPositiveButton(android.R.string.ok, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showProgressSpinner() {
        mLoginButton.setVisibility(View.GONE);
        mProgressSpinner.setVisibility(View.VISIBLE);
    }

    private void hideProgressSpinner() {
        mProgressSpinner.setVisibility(View.GONE);
        mLoginButton.setVisibility(View.VISIBLE);
    }
}