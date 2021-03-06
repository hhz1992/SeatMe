package com.seatme.gwu.seatme.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseUser;
import com.seatme.gwu.seatme.Constants;
import com.seatme.gwu.seatme.R;

import java.io.ByteArrayOutputStream;

/**
 * Created by Yan on 11/10/2015.
 *
 * ProfileActivity is to show the user profile information including username, email, photo and credit,allowing them to change their info as well.
 */
public class ProfileActivity extends AppCompatActivity {

    private final String TAG = "ProfileActivity";

    private TextView mUserName;
    private TextView mUserEmail;
    private EditText mEditName;
    private EditText mEditEmail;
    private Button mChangeName;
    private Button mChangeEmail;
    private TextView mCredit;
    private Button mSaveUserChange;
    private ParseUser user;
    private Button mChangePhoto;
    private ImageView mPhoto;


    private static final int CAMERA_PIC_REQUEST = 1337;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        mUserName = (TextView)findViewById(R.id.username);
        mUserEmail = (TextView)findViewById(R.id.user_email);
        mChangeName = (Button)findViewById(R.id.change_username);
        mChangeEmail=(Button)findViewById(R.id.change_email);
        mCredit=(TextView)findViewById(R.id.current_credit);

        mEditName = (EditText)findViewById(R.id.edit_name);
        mEditEmail=(EditText)findViewById(R.id.edit_email);
        mSaveUserChange =(Button)findViewById(R.id.save_usersetting);
        mChangePhoto = (Button)findViewById(R.id.add_photo);
        mPhoto = (ImageView)findViewById(R.id.my_photo);

        mEditEmail.setVisibility(View.INVISIBLE);
        mEditName.setVisibility(View.INVISIBLE);
        user = ParseUser.getCurrentUser();
        // if guest user, ask them to signin or signup
        if(user==null){
            mUserName.setMinWidth(300);
            mUserName.setText(R.string.profile_requirelogin);
            mUserEmail.setVisibility(View.INVISIBLE);
            mChangeEmail.setVisibility(View.INVISIBLE);
            mCredit.setVisibility(View.INVISIBLE);
            mChangeName.setVisibility(View.INVISIBLE);
            mChangePhoto.setVisibility(View.INVISIBLE);
            mSaveUserChange.setText(R.string.back_home);
            mSaveUserChange.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getBaseContext(), LoginActivity.class);
                    startActivity(intent);
                }
            });
        }
        //if signed in user, show full information
        else {
            String username = user.getString(Constants.NICKNAME);
            String userEmail = user.getEmail();
            int credit = (int)user.getNumber(Constants.CREDIT);
            mCredit.setText(mCredit.getText().toString() + credit);
            //download image from parse server, if this user have setup a custom pic yet, use default image
            ParseFile image = user.getParseFile(Constants.PIC);
            if(image!=null){
                image.getDataInBackground(new GetDataCallback() {
                    public void done(byte[] data, ParseException e) {
                        if (e == null) {
                            // Decode the Byte[] into bitmap
                            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                            // Set the Bitmap into the imageView
                            mPhoto.setImageBitmap(bmp);
                        } else {
                            // Something went wrong. Send a error notification toast to user.
                            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }

            mUserName.setText(mUserName.getText() + username);
            mUserEmail.setText(mUserEmail.getText() + userEmail);
            mChangeName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mEditName.setVisibility(View.VISIBLE);
                }
            });

            mEditName.setOnEditorActionListener(new EditText.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    boolean handled = false;
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        mUserName.setText(Constants.SHOW_USERNAME + mEditName.getText());
                        user.put(Constants.NICKNAME, mEditName.getText().toString());
                        mEditName.setVisibility(View.INVISIBLE);
                    }
                    return handled;
                }
            });

            mChangeEmail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mEditEmail.setVisibility(View.VISIBLE);

                }
            });

            mEditEmail.setOnEditorActionListener(new EditText.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    boolean handled = false;
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        mUserEmail.setText(Constants.SHOW_EMAIL + mEditEmail.getText());
                        user.setUsername(mEditEmail.getText().toString());
                        user.setEmail(mEditEmail.getText().toString());
                        mEditEmail.setVisibility(View.INVISIBLE);
                    }
                    return handled;
                }
            });

            mChangePhoto.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent,CAMERA_PIC_REQUEST);
                }
            });
            mSaveUserChange.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    try {
                        user.save();
                    } catch (ParseException e) {
                        //print exception in log to help developer find the bug
                        e.printStackTrace();
                        Log.d(TAG, e.toString());
                    }
                }
            });
        }
    }


    /**
     * After taking picture using camera, try to read the image and update the parse profile.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        byte[] image_byte_array;

        Bundle extras = data.getExtras();
        Bitmap image = (Bitmap) extras.get("data");
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        image_byte_array = stream.toByteArray();
        ParseFile picture_file = new ParseFile(Constants.PICTURE_FILE, image_byte_array);
        picture_file.saveInBackground();
        user.put(Constants.PIC, picture_file);
        mPhoto.setImageBitmap(image);
    }

}

