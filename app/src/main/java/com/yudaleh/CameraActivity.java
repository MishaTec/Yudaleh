package com.yudaleh;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseQuery;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CameraActivity extends AppCompatActivity {

    public static final String TAG = "CameraActivity";
    private static final int SMALL_IMAGE_SIZE = 200;
    private static final int BIG_IMAGE_SIZE = 800;

    private Debt debt;
    private Camera camera;// FIXME: 13/10/2015 deprecated
    private SurfaceView surfaceView;
    private ParseFile photoFile;
    private ParseFile thumbFile;
    private ImageButton photoButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        photoButton = (ImageButton) findViewById(R.id.camera_photo_button);

        if (camera == null) {
            try {
                camera = Camera.open();
                photoButton.setEnabled(true);
            } catch (Exception e) {
                Log.e(TAG, "No camera with exception: " + e.getMessage());
                photoButton.setEnabled(false);
                Toast.makeText(this, "No camera detected",
                        Toast.LENGTH_LONG).show();
            }
        }

        photoButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (camera == null)
                    return;
                camera.takePicture(new Camera.ShutterCallback() {

                    @Override
                    public void onShutter() {
                        // nothing to do
                    }

                }, null, new Camera.PictureCallback() {

                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        saveScaledAndOrigPhoto(data);
                    }

                });

            }
        });

        surfaceView = (SurfaceView) findViewById(R.id.camera_surface_view);
        SurfaceHolder holder = surfaceView.getHolder();
        holder.addCallback(new Callback() {

            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if (camera != null) {
                        camera.setDisplayOrientation(90);
                        camera.setPreviewDisplay(holder);
                        camera.startPreview();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error setting up preview", e);
                }
            }

            public void surfaceChanged(SurfaceHolder holder, int format,
                                       int width, int height) {
                // nothing to do here
            }

            public void surfaceDestroyed(SurfaceHolder holder) {
                // nothing here
            }

        });
        try {
            loadExistingDebt(getIntent().getStringExtra(Debt.KEY_UUID));
        } catch (ParseException e) {
            Toast.makeText(CameraActivity.this, "Failed loading debt", Toast.LENGTH_SHORT).show();// TODO: 13/10/2015 make sure toast is displayed
            setResult(RESULT_CANCELED);
            finish();
        }
    }


    /**
     * Loads the current <code>Debt</code> from local database.
     *
     * @throws ParseException
     */
    private void loadExistingDebt(String debtId) throws ParseException {
        ParseQuery<Debt> query = Debt.getQuery();
        query.fromLocalDatastore();
        query.whereEqualTo(Debt.KEY_UUID, debtId);
        debt = query.getFirst();
    }

    /*
     * ParseQueryAdapter loads ParseFiles into a ParseImageView at whatever size
     * they are saved. Since we never need a full-size image in our app, we'll
     * save a scaled one right away.
     */
    private void saveScaledAndOrigPhoto(byte[] data) {

        Bitmap debtImage = BitmapFactory.decodeByteArray(data, 0, data.length);

        // Override Android default landscape orientation and save portrait
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap rotatedDebtImage = Bitmap.createBitmap(debtImage, 0,
                0, debtImage.getWidth(), debtImage.getHeight(),
                matrix, true);

        // Resize photo from camera byte array
        Bitmap rotatedDebtImageSmall = Bitmap.createScaledBitmap(rotatedDebtImage, SMALL_IMAGE_SIZE, SMALL_IMAGE_SIZE
                * rotatedDebtImage.getHeight() / rotatedDebtImage.getWidth(), false);

        // Resize photo from camera byte array
        Bitmap rotatedDebtImageBig = Bitmap.createScaledBitmap(rotatedDebtImage, BIG_IMAGE_SIZE, BIG_IMAGE_SIZE
                * rotatedDebtImage.getHeight() / rotatedDebtImage.getWidth(), false);


        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        rotatedDebtImageBig.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        byte[] origData = bos.toByteArray();

        ByteArrayOutputStream bosScaled = new ByteArrayOutputStream();
        rotatedDebtImageSmall.compress(Bitmap.CompressFormat.JPEG, 100, bosScaled);
        byte[] scaledData = bosScaled.toByteArray();

        // Save the image to Parse
        photoFile = new ParseFile("debt_photo.jpg", origData);
        thumbFile = new ParseFile("debt_thumb.jpg", scaledData);
        try {
            photoFile.save();
            debt.setPhotoFile(photoFile);
            thumbFile.save();
            debt.setThumbFile(thumbFile);
            setResult(RESULT_OK);
            finish();
        } catch (ParseException e) {
            Toast.makeText(getApplicationContext(),
                    "Error saving: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
            finish();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (camera == null) {
            try {
                camera = Camera.open();
                photoButton.setEnabled(true);
            } catch (Exception e) {
                Log.i(TAG, "No camera: " + e.getMessage());
                photoButton.setEnabled(false);
                Toast.makeText(getApplicationContext(), "No camera detected",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onPause() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
        }
        super.onPause();
    }

}
