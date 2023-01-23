package cat.dam.andy.camera_galeria;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    String currentPhotoPath;
    ImageView iv_imatge;
    Button btn_foto, btn_galeria;
    Uri uriPhotoImage;
    ContentValues values;

    private final ActivityResultLauncher<Intent> activityResultLauncherGallery = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                //here we will handle the result of our intent
                if (result.getResultCode() == Activity.RESULT_OK) {
                    //image picked
                    //get uri of image
                    Intent data = result.getData();
                    assert data != null;
                    Uri imageUri = data.getData();
                    System.out.println("galeria: "+imageUri);
                    iv_imatge.setImageURI(imageUri);
                } else {
                    //cancelled
                    Toast.makeText(MainActivity.this, "Cancelled...", Toast.LENGTH_SHORT).show();
                }
            }
    );
    private final ActivityResultLauncher<Intent> activityResultLauncherPhoto = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                //here we will handle the result of our intent
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Toast.makeText(this, "Image saved", Toast.LENGTH_SHORT).show();
                    iv_imatge.setImageURI(uriPhotoImage); //Amb paràmetre EXIF podem canviar orientació (per defecte horiz en versions android antigues)
                    refreshGallery();//refresca gallery per veure nou fitxer
                        /* Intent data = result.getData(); //si volguessim només la miniatura
                        Uri imageUri = data.getData();
                        iv_imatge.setImageURI(imageUri);*/
                } else {
                    //cancelled
                    Toast.makeText(MainActivity.this, "Cancelled...", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv_imatge = findViewById(R.id.iv_foto);
        btn_foto = findViewById(R.id.btn_foto);
        btn_galeria = findViewById(R.id.btn_galeria);

        btn_galeria.setOnClickListener(v -> {
                if (checkPermissions()) {
                    openGallery();
                } else {
                    askForPermissions();
                }
        });

        btn_foto.setOnClickListener(v -> {
                if (checkPermissions())
                        {
                            takePicture();
                } else {
                    askForPermissions();
                }
        });

    }

    private boolean checkPermissions() {
        return (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        );
    }


    private void askForPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){  //>Android 11 (API 30)
            ActivityCompat.requestPermissions(this, new String[]
                        {
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                            }, 3);
            //NOTE: MANAGE_EXTERNAL_STORAGE is a special permission only allowed for few apps like Antivirus, file manager, etc. You have to justify the reason while publishing the app to PlayStore.
        }
        else {
            ActivityCompat.requestPermissions(this, new String[]
                        {
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                            }, 3);
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void openGallery() {
        try {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image/*");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                intent.setAction(Intent.ACTION_GET_CONTENT);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    activityResultLauncherGallery.launch(Intent.createChooser(intent, "Select File"));
                } else {
                    Toast.makeText(MainActivity.this, "El seu dispositiu no permet accedir a la galeria",
                            Toast.LENGTH_SHORT).show();
                }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    public void takePicture() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(MainActivity.this, "Error en la creació del fitxer",
                        Toast.LENGTH_SHORT).show();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                values = new ContentValues();
                values.put(MediaStore.Images.Media.TITLE, "La meva foto");
                values.put(MediaStore.Images.Media.DESCRIPTION, "Foto feta el " + System.currentTimeMillis());
                Uri uriImage = FileProvider.getUriForFile(this,
                        this.getPackageName()+ ".provider", //(use your app signature + ".provider" )
                        photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uriImage);
                    activityResultLauncherPhoto.launch(intent);
                } else {
                    Toast.makeText(MainActivity.this, "No s'ha pogut crear la imatge",
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, "El seu dispositiu no permet accedir a la càmera",
                        Toast.LENGTH_SHORT).show();
            }
    }

    private File createImageFile() throws IOException {
        boolean wasSuccessful; //just for testing mkdirs
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        // File storageDir = getFilesDir();//no es veurà a la galeria
        // File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES+File.separator+this.getPackageName());//No es veurà a la galeria
        File storageDir =Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES+File.separator+this.getPackageName());
        //NOTE: MANAGE_EXTERNAL_STORAGE is a special permission only allowed for few apps like Antivirus, file manager, etc. You have to justify the reason while publishing the app to PlayStore.
        if (!storageDir.exists()) {
            wasSuccessful =storageDir.mkdir();
        }
        else {
            wasSuccessful =storageDir.mkdirs();
        }
        if (wasSuccessful) {
            System.out.println("storageDir: " + storageDir);
        } else {
            System.out.println("storageDir: " + storageDir + " was not created");
        }
        // Save a file: path for use with ACTION_VIEW intents
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        uriPhotoImage = Uri.fromFile(image);
        System.out.println("fitxer: "+uriPhotoImage);
        return image;
    }

    private void refreshGallery() {
        //Cal refrescar per poder veure la foto creada a la galeria
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(uriPhotoImage);
        this.sendBroadcast(mediaScanIntent);
    }

}
