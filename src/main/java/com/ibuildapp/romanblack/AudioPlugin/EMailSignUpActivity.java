/****************************************************************************
*                                                                           *
*  Copyright (C) 2014-2015 iBuildApp, Inc. ( http://ibuildapp.com )         *
*                                                                           *
*  This file is part of iBuildApp.                                          *
*                                                                           *
*  This Source Code Form is subject to the terms of the iBuildApp License.  *
*  You can obtain one at http://ibuildapp.com/license/                      *
*                                                                           *
****************************************************************************/
package com.ibuildapp.romanblack.AudioPlugin;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.*;
import com.appbuilder.sdk.android.AppBuilderModuleMain;
import com.appbuilder.sdk.android.authorization.Authorization;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This activity provides authorization via email functionality.
 */
public class EMailSignUpActivity extends AppBuilderModuleMain implements OnClickListener,
        TextWatcher, View.OnFocusChangeListener {

    private final int CLOSE_ACTIVITY_OK = 0;
    private final int CLOSE_ACTIVITY_BAD = 1;
    private final int SHOW_PROGRESS_DIALOG = 2;
    private final int HIDE_PROGRESS_DIALOG = 3;
    private final int CHECK_ACTIVE_SIGN_IN = 4;
    private String DEFAULT_FIRST_NAME_TEXT = "First name";
    private String DEFAULT_LAST_NAME_TEXT = "Last name";
    private String DEFAULT_EMAIL_TEXT = "Email";
    private String DEFAULT_PASSWORD_TEXT = "Password";
    private boolean signUpActive = false;
    private boolean needCheckFields = false;
    private EditText firstNameEditText = null;
    private EditText lastNameEditText = null;
    private EditText emailEditText = null;
    private EditText passwordEditText = null;
    private EditText rePasswordEditText = null;
    private CheckBox termsCheckBox = null;
    private ProgressDialog progressDialog = null;
    private LinearLayout termsLayout = null;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case CLOSE_ACTIVITY_OK: {
                    closeActivityOk();
                }
                break;
                case CLOSE_ACTIVITY_BAD: {
                    closeActivityBad();
                }
                break;
                case SHOW_PROGRESS_DIALOG: {
                    showProgressDialog();
                }
                break;
                case HIDE_PROGRESS_DIALOG: {
                    hideProgressDialog();
                }
                break;
                case CHECK_ACTIVE_SIGN_IN: {
                }
                break;
            }
        }
    };

    @Override
    public void create() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.romanblack_audio_emailsignup);

        String DEFAULT_FIRST_NAME_TEXT = getString(R.string.romanblack_audio_first_name);
        String DEFAULT_LAST_NAME_TEXT = getString(R.string.romanblack_audio_last_name);
        String DEFAULT_EMAIL_TEXT = getString(R.string.romanblack_audio_email);
        String DEFAULT_PASSWORD_TEXT = getString(R.string.romanblack_audio_password);

        firstNameEditText = (EditText) findViewById(R.id.romanblack_audio_emailsignup_fname);
        firstNameEditText.addTextChangedListener(this);
        firstNameEditText.setOnFocusChangeListener(this);
        firstNameEditText.setText(DEFAULT_FIRST_NAME_TEXT);
        firstNameEditText.setTextColor(Color.GRAY);

        lastNameEditText = (EditText) findViewById(R.id.romanblack_audio_emailsignup_lname);
        lastNameEditText.addTextChangedListener(this);
        lastNameEditText.setOnFocusChangeListener(this);
        lastNameEditText.setText(DEFAULT_LAST_NAME_TEXT);
        lastNameEditText.setTextColor(Color.GRAY);

        emailEditText = (EditText) findViewById(R.id.romanblack_audio_emailsignup_email);
        emailEditText.addTextChangedListener(this);
        emailEditText.setOnFocusChangeListener(this);
        emailEditText.setText(DEFAULT_EMAIL_TEXT);
        emailEditText.setTextColor(Color.GRAY);

        passwordEditText = (EditText) findViewById(R.id.romanblack_audio_emailsignup_pwd);
        passwordEditText.addTextChangedListener(this);
        passwordEditText.setOnFocusChangeListener(this);
        passwordEditText.setText(DEFAULT_PASSWORD_TEXT);
        passwordEditText.setTextColor(Color.GRAY);

        rePasswordEditText = (EditText) findViewById(R.id.romanblack_audio_emailsignup_rpwd);
        rePasswordEditText.addTextChangedListener(this);
        rePasswordEditText.setOnFocusChangeListener(this);
        rePasswordEditText.setText(DEFAULT_PASSWORD_TEXT);
        rePasswordEditText.setTextColor(Color.GRAY);

        termsCheckBox = (CheckBox) findViewById(R.id.romanblack_audio_emailsignup_chbterms);
        termsCheckBox.setOnClickListener(this);

        termsLayout = (LinearLayout) findViewById(R.id.romanblack_audio_emailsignup_layouttems);
        termsLayout.setVisibility(View.INVISIBLE);

        // toptab initialization
        setTopBarLeftButtonText(getResources().getString(R.string.back), true, new OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        setTopBarRightButtonText(getResources().getString(R.string.signup), false, new OnClickListener() {
            @Override
            public void onClick(View view) {
                signUp();
            }
        });
        setTopBarTitle(getResources().getString(R.string.login));
        swipeBlock();

        needCheckFields = true;
    }

    /**
     * Validates values of input fields.
     */
    private void checkFields() {
        if (!needCheckFields) {
            return;
        }

        if ((firstNameEditText.getText().toString().length() > 0)
                && (lastNameEditText.getText().toString().length() > 0)
                && (emailEditText.getText().toString().length() > 0)
                && (passwordEditText.getText().toString().length() > 0)
                && (rePasswordEditText.getText().toString().length() > 0)) {
            String regExpn =
                    "^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]{1}|[\\w-]{2,}))@"
                    + "((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                    + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
                    + "([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                    + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])){1}|"
                    + "([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$";

            Pattern pattern = Pattern.compile(regExpn, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(emailEditText.getText().toString());

            if (matcher.matches()) {
            } else {
                signUpActive = false;
                return;
            }

            if (!firstNameEditText.getText().toString().equals(
                    lastNameEditText.getText().toString())) {
            } else {
                signUpActive = false;
                return;
            }

            if (passwordEditText.getText().toString().equals(rePasswordEditText.getText().toString())) {
            } else {
                signUpActive = false;
                return;
            }

            if (passwordEditText.getText().toString().length() >= 4) {
            } else {
                signUpActive = false;
                return;
            }

            signUpActive = true;

        } else {
            signUpActive = false;
        }
    }

    /**
     * Closes activity with "OK" result.
     */
    private void closeActivityOk() {
        Intent resIntent = new Intent();
        setResult(RESULT_OK, resIntent);

        finish();
    }

    /**
     * Closes activity with "Cancel" result.
     */
    private void closeActivityBad() {
        finish();
    }

    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = ProgressDialog.show(this, null, getString(R.string.romanblack_audio_loading));
        } else {
            if (!progressDialog.isShowing()) {
                progressDialog = ProgressDialog.show(this, null, getString(R.string.romanblack_audio_loading));
            }
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    public void onClick(View arg0) {
        if (arg0 == termsCheckBox) {
            checkFields();
            handler.sendEmptyMessage(CHECK_ACTIVE_SIGN_IN);
        }
    }

    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
    }

    public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
    }

    public void afterTextChanged(Editable arg0) {
        checkFields();

        handler.sendEmptyMessage(CHECK_ACTIVE_SIGN_IN);
    }

    public void onFocusChange(View arg0, boolean arg1) {
        if (arg0.getId() == R.id.romanblack_audio_emailsignup_fname) {
            if (arg1) {
                if (((TextView) arg0).getText().toString().equals(DEFAULT_FIRST_NAME_TEXT)) {
                    ((TextView) arg0).setText("");
                    ((TextView) arg0).setTextColor(Color.BLACK);
                }
            } else {
                if (((TextView) arg0).getText().toString().equals("")) {
                    ((TextView) arg0).setText(DEFAULT_FIRST_NAME_TEXT);
                    ((TextView) arg0).setTextColor(Color.GRAY);
                }
            }
        } else if (arg0.getId() == R.id.romanblack_audio_emailsignup_lname) {
            if (arg1) {
                if (((TextView) arg0).getText().toString().equals(DEFAULT_LAST_NAME_TEXT)) {
                    ((TextView) arg0).setText("");
                    ((TextView) arg0).setTextColor(Color.BLACK);
                }
            } else {
                if (((TextView) arg0).getText().toString().equals("")) {
                    ((TextView) arg0).setText(DEFAULT_LAST_NAME_TEXT);
                    ((TextView) arg0).setTextColor(Color.GRAY);
                }
            }
        } else if (arg0.getId() == R.id.romanblack_audio_emailsignup_email) {
            if (arg1) {
                if (((TextView) arg0).getText().toString().equals(DEFAULT_EMAIL_TEXT)) {
                    ((TextView) arg0).setText("");
                    ((TextView) arg0).setTextColor(Color.BLACK);
                }
            } else {
                if (((TextView) arg0).getText().toString().equals("")) {
                    ((TextView) arg0).setText(DEFAULT_EMAIL_TEXT);
                    ((TextView) arg0).setTextColor(Color.GRAY);
                }
            }
        } else if (arg0.getId() == R.id.romanblack_audio_emailsignup_pwd) {
            if (arg1) {
                if (((TextView) arg0).getText().toString().equals(DEFAULT_PASSWORD_TEXT)) {
                    ((TextView) arg0).setText("");
                    ((TextView) arg0).setTextColor(Color.BLACK);
                }
            } else {
                if (((TextView) arg0).getText().toString().equals("")) {
                    ((TextView) arg0).setText(DEFAULT_PASSWORD_TEXT);
                    ((TextView) arg0).setTextColor(Color.GRAY);
                }
            }
        } else if (arg0.getId() == R.id.romanblack_audio_emailsignup_rpwd) {
            if (arg1) {
                if (((TextView) arg0).getText().toString().equals(DEFAULT_PASSWORD_TEXT)) {
                    ((TextView) arg0).setText("");
                    ((TextView) arg0).setTextColor(Color.BLACK);
                }
            } else {
                if (((TextView) arg0).getText().toString().equals("")) {
                    ((TextView) arg0).setText(DEFAULT_PASSWORD_TEXT);
                    ((TextView) arg0).setTextColor(Color.GRAY);
                }
            }
        }
    }

    /**
     * Signs up user depending on input fields values.
     */
    private void signUp() {
        if (signUpActive) {
            handler.sendEmptyMessage(SHOW_PROGRESS_DIALOG);

            new Thread(new Runnable() {
                public void run() {
                    String firstNameString = firstNameEditText.getText().toString();
                    String lastNameString = lastNameEditText.getText().toString();
                    String emailString = emailEditText.getText().toString();
                    String passwordString = passwordEditText.getText().toString();
                    String rePasswordString = rePasswordEditText.getText().toString();

                    if (Authorization.registerEmail(firstNameString, lastNameString,
                            emailString, passwordString, rePasswordString)) {
                        handler.sendEmptyMessage(HIDE_PROGRESS_DIALOG);

                        handler.sendEmptyMessage(CLOSE_ACTIVITY_OK);
                    } else {
                        handler.sendEmptyMessage(CLOSE_ACTIVITY_BAD);
                    }

                }
            }).start();
        } else {
            if (firstNameEditText.getText().toString().length() == 0
                    || lastNameEditText.getText().toString().length() == 0
                    || emailEditText.getText().toString().length() == 0
                    || passwordEditText.getText().toString().length() == 0
                    || rePasswordEditText.getText().toString().length() == 0) {
                Toast.makeText(this, R.string.romanblack_audio_alert_fill_all_fields, Toast.LENGTH_LONG).show();
                return;
            }

            if (firstNameEditText.getText().toString().equals(
                    lastNameEditText.getText().toString())) {
                Toast.makeText(this, R.string.romanblack_audio_alert_names_match_each, Toast.LENGTH_LONG).show();
                return;
            }

            if (firstNameEditText.getText().toString().length() <= 2
                    || lastNameEditText.getText().toString().length() <= 2) {
                Toast.makeText(this, R.string.romanblack_audio_alert_short_name, Toast.LENGTH_LONG).show();
                return;
            }

            String regExpn =
                    "^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]{1}|[\\w-]{2,}))@"
                    + "((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                    + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
                    + "([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                    + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])){1}|"
                    + "([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$";

            Pattern pattern = Pattern.compile(regExpn, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(emailEditText.getText().toString());

            if (matcher.matches()) {
            } else {
                Toast.makeText(this, R.string.romanblack_audio_alert_incorrect_email, Toast.LENGTH_LONG).show();
                return;
            }

            if (passwordEditText.getText().toString().length() < 4) {
                Toast.makeText(this, R.string.romanblack_audio_alert_short_name, Toast.LENGTH_LONG).show();
                return;
            }

            if (!passwordEditText.getText().toString().equals(
                    rePasswordEditText.getText().toString())) {
                Toast.makeText(this, R.string.romanblack_audio_alert_passwords_dont_match, Toast.LENGTH_LONG).show();
                return;
            }
        }
    }
}
