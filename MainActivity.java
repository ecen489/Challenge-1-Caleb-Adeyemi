package edu.tamu.ece.mp71;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class MainActivity extends AppCompatActivity {
    Button btn;
    ImageView imageTaken;
    String pathToFile;
    SQLiteDatabase db;
    int imageId = 1;
    EditText input;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn = findViewById(R.id.button);
        input = findViewById(R.id.editText);
        imageTaken = findViewById(R.id.image);
        if(Build.VERSION.SDK_INT >= 23){
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},2);

        }
        String query = "CREATE TABLE IF NOT EXISTS images ( "
                + " id INTEGER PRIMARY KEY, "
                + " image BLOB NOT NULL "
                + " )";
        db = openOrCreateDatabase("imagesdata.db", Context.MODE_PRIVATE, null);
        db.execSQL(query);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            if(requestCode == 1) {
                Bitmap bitmap = BitmapFactory.decodeFile(pathToFile);
                imageTaken.setImageBitmap(bitmap);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream);
                byte[] imageBlob = stream.toByteArray();
                addEntry(imageId, imageBlob);
                imageId++;
            }
        }
    }

    private void addEntry(int imageId, byte[] imageBlob) throws SQLiteException {
        String sql = "INSERT INTO images (id, image) VALUES (?,?)";
        SQLiteStatement insertStatement = db.compileStatement(sql);
        insertStatement.bindLong(1, imageId);
        insertStatement.bindBlob(2,imageBlob);
        try {
            insertStatement.executeInsert();
        } catch (SQLiteConstraintException e) {
            imageId++;
            addEntry(imageId, imageBlob);
        }
        Toast toast = Toast.makeText(getApplicationContext(), "Photo ID is "+ imageId, Toast.LENGTH_SHORT);
        toast.show();
        insertStatement.clearBindings();
    }

    public void takePicture(View view) {
        Intent takePic = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(takePic.resolveActivity(getPackageManager()) != null){
            File photofile = createPhotoFile();

            if(photofile != null){
                 pathToFile = photofile.getAbsolutePath();
                Uri photoURI = FileProvider.getUriForFile(MainActivity.this, "edu.tamu.ece.mp71.cameraandroid.fileprovider", photofile);
                takePic.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePic, 1);

            }


        }
    }

    private File createPhotoFile() {
        File image = null;
        String name = new SimpleDateFormat("yyyMMdd_HHmmss").format(new Date());
        File storageDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        try{
             image = File.createTempFile(name,".jpg", storageDir);

        } catch (IOException e) {
            Log.d("mylog", "Excep :" + e.toString());
        }
        return image;
    }

    public void queryImage(View view) {
        String userID = input.getText().toString();
        int useridInt = -1;
        try {
            useridInt = Integer.parseInt(userID);
        } catch (NumberFormatException e) {
            Toast toast = Toast.makeText(getApplicationContext(), "Please input a number.", Toast.LENGTH_SHORT);
            toast.show();
            //queryImage(view);
            input.setText("");
            useridInt = -2;
        }
        if (useridInt != -1) {
            String searchCommand = "SELECT * FROM images WHERE id = " + Integer.toString(useridInt);
            Cursor cr = db.rawQuery(searchCommand, null);
            if (cr.moveToFirst()) {
                do {
                    byte[] result = cr.getBlob(cr.getColumnIndex("image"));
                    imageTaken.setImageBitmap(BitmapFactory.decodeByteArray(result, 0, result.length));
                } while (cr.moveToNext());
            }
            cr.close();
        }
    }

}
