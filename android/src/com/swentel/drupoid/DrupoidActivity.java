package com.swentel.drupoid;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class DrupoidActivity extends Activity {

  // @todo add url and password through app, not here.
  // Change this to your Drupoid URL.
  private static final String DrupoidURL = "http://10.0.2.2/drupal7/drupoid";
  // Change this to your Drupoid Password.
  private static final String DrupoidPassword = "test";

  // private static String selectedImagePath = "";
  private ImageView imgView;
  private Bitmap bitmap;
  private String image_title;
  private String selectedImagePath;
  private ProgressDialog dialog;
  private final int SELECT_PICTURE = 1;
  InputStream inputStream;

  public void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    // Add listener on image preview.
    imgView = (ImageView) findViewById(R.id.image_preview);
    imgView.setOnClickListener(onSelectPress);

    // Add listener on upload button.
    Button upload = (Button) findViewById(R.id.upload_button);
    upload.setOnClickListener(onUploadPress);
  }

  /**
   * OnClickListener on select button.
   */
  private final View.OnClickListener onSelectPress = new View.OnClickListener() {
    public void onClick(View v) {
      Intent intent = new Intent();
      intent.setType("image/*");
      intent.setAction(Intent.ACTION_GET_CONTENT);
      startActivityForResult(Intent.createChooser(intent, getString(R.string.picture_select)), SELECT_PICTURE);
    }
  };

  /**
   * Start onActivityResult for image select.
   */
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode == RESULT_OK) {
      if (requestCode == SELECT_PICTURE) {
        Uri selectedImageUri = data.getData();
        selectedImagePath = getPath(selectedImageUri);
        bitmap = BitmapFactory.decodeFile(selectedImagePath);
        ImageView imageView = (ImageView) findViewById(R.id.image_preview);
        imageView.setImageBitmap(bitmap);
      }
    }
  }

  /**
   * OnClickListener on upload button.
   */
  private final View.OnClickListener onUploadPress = new View.OnClickListener() {
    public void onClick(View v) {
      EditText title = (EditText) findViewById(R.id.title);
      if (title.getText().toString().length() > 0 && selectedImagePath.toString().length() > 0) {
        image_title = title.getText().toString();
        dialog = ProgressDialog.show(DrupoidActivity.this, getString(R.string.uploading), getString(R.string.please_wait), true);
        new DrupoidUploadTask().execute();
      }
      else {
        Toast.makeText(getBaseContext(), R.string.missing_data, Toast.LENGTH_LONG).show();
      }
    }
  };

  /**
   * Get path of image.
   */
  public String getPath(Uri uri) {
    String[] projection = {
      MediaStore.Images.Media.DATA
    };
    Cursor cursor = managedQuery(uri, projection, null, null, null);
    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
    cursor.moveToFirst();
    return cursor.getString(column_index);
  }

  /**
   * Upload to Drupoid enabled server.
   * 
   * @todo do not use base64, but direct httpPost.
   */
  class DrupoidUploadTask extends AsyncTask<Void, Void, String> {

    protected String doInBackground(Void... unused) {
      String sResponse = "";
      BitmapFactory.Options o2 = new BitmapFactory.Options();
      int scale = 2;
      o2.inSampleSize = scale;
      Bitmap bitmap = BitmapFactory.decodeFile(selectedImagePath, o2);
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
      byte[] byte_arr = stream.toByteArray();
      String image_str = Base64.encodeBytes(byte_arr);

      ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
      nameValuePairs.add(new BasicNameValuePair("image", image_str));
      nameValuePairs.add(new BasicNameValuePair("title", image_title));
      nameValuePairs.add(new BasicNameValuePair("password", DrupoidPassword));

      try {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(DrupoidURL);
        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        HttpResponse response = httpclient.execute(httppost);
        sResponse = convertResponseToString(response);
      }
      catch (Exception e) {
        if (dialog.isShowing()) {
          dialog.dismiss();
        }
        Toast.makeText(getBaseContext(), "ERROR " + e.getMessage(), Toast.LENGTH_LONG).show();
      }

      return sResponse;
    }

    protected void onPostExecute(String sResponse) {
      if (dialog.isShowing()) {
        dialog.dismiss();
      }
      // Show message and reset application.
      Toast.makeText(getBaseContext(), sResponse, Toast.LENGTH_LONG).show();
      selectedImagePath = "";
      EditText title = (EditText) findViewById(R.id.title);
      title.setText("");
      ImageView imageView = (ImageView) findViewById(R.id.image_preview);
      imageView.setImageResource(R.drawable.insert_image);
    }
  }

  /**
   * Convert response.
   */
  public String convertResponseToString(HttpResponse response) throws IllegalStateException, IOException {

    String res = "";
    StringBuffer buffer = new StringBuffer();
    inputStream = response.getEntity().getContent();
    int contentLength = (int) response.getEntity().getContentLength();

    if (contentLength > 0) {
      byte[] data = new byte[512];
      int len = 0;
      try {
        while (-1 != (len = inputStream.read(data))) {
          buffer.append(new String(data, 0, len));
        }
      }
      catch (IOException e) {
        e.printStackTrace();
      }
      try {
        inputStream.close();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
      res = buffer.toString();
    }

    return res;
  }
}