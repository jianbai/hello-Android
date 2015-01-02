package io.spw.hello;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * Created by scottwang on 12/31/14.
 */
public class SetProfileActivity extends ActionBarActivity {

    public static final String TAG = SetProfileActivity.class.getSimpleName();

    private ParseUser currentUser;

    private Boolean noGender;
    private Boolean noAge;
    private Boolean noHometown;

    private EditText mGenderEditText;
    private AlertDialog mGenderDialog;

    private EditText mBirthdayEditText;
    private DatePickerDialog mDatePickerDialog;
    private SimpleDateFormat mDateFormatter;

    private EditText mHometownEditText;

    private Button mSaveButton;
    private AlertDialog mIncompleteDialog;
    private ProgressBar mProgressSpinner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_profile);
        currentUser = ParseUser.getCurrentUser();

        findViews();
        initializeBooleans();

        if (noGender) {
            setUpGender();
        }

        if (noAge) {
            setUpAge();
        }

        if (noHometown) {
            setUpHometown();
        }

        setUpSaveButton();
    }

    private void findViews() {
        mGenderEditText = (EditText) findViewById(R.id.set_profile_gender_edittext);
        mBirthdayEditText = (EditText) findViewById(R.id.set_profile_birthday_edittext);
        mHometownEditText = (EditText) findViewById(R.id.set_profile_hometown_edittext);
        mSaveButton = (Button) findViewById(R.id.set_profile_save_button);
        mProgressSpinner = (ProgressBar) findViewById(R.id.set_profile_progress_spinner);
    }

    private void initializeBooleans() {
        Intent intent = getIntent();
        noGender = intent.getExtras().getBoolean("noGender");
        noAge = intent.getExtras().getBoolean("noAge");
        noHometown = intent.getExtras().getBoolean("noHometown");
    }

    private void setUpGender() {
        mGenderEditText.setVisibility(View.VISIBLE);
        mGenderEditText.setInputType(InputType.TYPE_NULL);

        initializeGenderDialog();

        mGenderEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGenderDialog.show();
            }
        });
    }

    private void initializeGenderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.set_profile_gender_hint);
        builder.setItems(R.array.gender_array, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                switch(which) {
                    case 0:
                        mGenderEditText.setText(R.string.gender_female);
                        break;
                    case 1:
                        mGenderEditText.setText(R.string.gender_male);
                }
            }
        });

        mGenderDialog = builder.create();
    }

    private void setUpAge() {
        mBirthdayEditText.setVisibility(View.VISIBLE);
        mBirthdayEditText.setInputType(InputType.TYPE_NULL);
        mDateFormatter = new SimpleDateFormat("MM/dd/yyyy", Locale.CANADA);

        initializeDatePickerDialog();

        mBirthdayEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDatePickerDialog.show();
            }
        });
    }

    private void initializeDatePickerDialog() {
        GregorianCalendar cal = new GregorianCalendar();

        mDatePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                GregorianCalendar date = new GregorianCalendar();
                date.set(year, monthOfYear, dayOfMonth);
                mBirthdayEditText.setText(mDateFormatter.format(date.getTime()));
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
    }

    private void setUpHometown() {
        mHometownEditText.setVisibility(View.VISIBLE);
    }

    private void setUpSaveButton() {
        initializeIncompleteDialog();

        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgressSpinner();

                Boolean emptyGender = noGender && mGenderEditText.getText().toString().matches("");
                Boolean emptyBirthday = noAge && mBirthdayEditText.getText().toString().matches("");
                Boolean emptyHometown = noHometown && mHometownEditText.getText().toString().matches("");

                if (emptyGender || emptyBirthday || emptyHometown) {
                    mIncompleteDialog.show();
                    hideProgressSpinner();
                } else {
                    if (noGender) {
                        updateUserGender();
                    }

                    if (noAge) {
                        updateUserAge();
                    }

                    if (noHometown) {
                        updateUserHometown();
                    }

                    saveToParse();
                }
            }
        });
    }

    private void initializeIncompleteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.set_profile_incomplete_message)
                .setTitle(R.string.set_profile_incomplete_title)
                .setPositiveButton(android.R.string.ok, null);

        mIncompleteDialog = builder.create();
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

    private void showInvalidAgeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.set_profile_invalid_age_message)
                .setTitle(R.string.set_profile_invalid_age_title)
                .setPositiveButton(android.R.string.ok, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateUserGender() {
        String gender = mGenderEditText.getText().toString();
        currentUser.put(ParseConstants.KEY_GENDER, gender);
    }

    private void updateUserAge() {
        String birthday = mBirthdayEditText.getText().toString();
        try {
            String age = calculateAge(birthday);
            currentUser.put(ParseConstants.KEY_AGE, age);
        } catch (ParseException e) {
            showInvalidAgeDialog();
        }
        currentUser.put(ParseConstants.KEY_BIRTHDAY, birthday);
    }

    private void updateUserHometown() {
        String hometown = mHometownEditText.getText().toString();
        currentUser.put(ParseConstants.KEY_HOMETOWN, hometown);
    }

    private void saveToParse() {
        currentUser.saveInBackground(new SaveCallback() {
            @Override
            public void done(com.parse.ParseException e) {
                hideProgressSpinner();
                navigateToMain();
            }
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private void showProgressSpinner() {
        mSaveButton.setVisibility(View.GONE);
        mProgressSpinner.setVisibility(View.VISIBLE);
    }

    private void hideProgressSpinner() {
        mProgressSpinner.setVisibility(View.GONE);
        mSaveButton.setVisibility(View.VISIBLE);
    }

}