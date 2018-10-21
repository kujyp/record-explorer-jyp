package com.yangyinetwork.recordexplorerjyp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private final String[] mPermissions = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };
    private List<UploadFileJob> mUploadFileJobs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, mPermissions, 123);
        ensurePermissionOrExit();
        checkAudioFiles();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void ensurePermissionOrExit() {
        Log.d(TAG, "ensurePermissionOrExit: ");

        for (String permission: mPermissions) {
            if (!hasPermission(permission)) {
                exitApplication();
                return;
            }
        }
    }

    private boolean hasPermission(String permission) {
        int permissionStatus = ContextCompat.checkSelfPermission(
                this, permission);

        return permissionStatus == PackageManager.PERMISSION_GRANTED;
    }

    private void exitApplication() {
        Log.d(TAG, "exitApplication: ");
        finish();
    }

    private void checkAudioFiles() {
        File directory = new File(Environment.getExternalStorageDirectory(), "Voice Recorder");

        Log.d(TAG, "checkAudioFiles: directory=" + directory);
        File[] files = directory.listFiles();

        int renamedCount = 0;
        for (File eachFile : files) {
            if (isDefaultFilename(eachFile)) {
                if (!rename(eachFile, enname(eachFile))) {
                    appendLog(String.format(
                            "Rename fail file=[%s], rename=[%s]",
                            eachFile.getName(), enname(eachFile)));
                }
                renamedCount++;
            }
//            uploadFileIfNotUploaded(eachFile);
        }
        appendLog(String.format("Rename done %d files.", renamedCount));
    }

    private void appendLog(String content) {
        TextView tvHelloWorld = findViewById(R.id.tv_helloworld);
        tvHelloWorld.setText(tvHelloWorld.getText() + "\n" + content);
    }

    private boolean isDefaultFilename(File file) {
        boolean ret = file.getName().startsWith("Voice ");
        Log.d(TAG, "isDefaultFilename() called with: file = [" + file + "]" + ", ret=" + ret);
        return ret;
    }

    private boolean rename(File file, String name) {
        File renameFile = new File(file.getParent() + "/" + name);
        Log.d(TAG, "rename() called with: file = [" + file + "], renameFile = [" + renameFile + "]");
        return file.renameTo(renameFile);
//        return true;
    }

    private void uploadFileIfNotUploaded(final File file) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();

        String name = enname(file);
        final StorageReference fileRef = storageRef.child("android/" + name);

        doesFileExistOnStorage(fileRef, (doesExist) -> {
            if (doesExist) {
                return;
            }

            enqueueUploadFile(file, fileRef);
        });
    }

    private void enqueueUploadFile(File file, StorageReference fileRef) {
        UploadFileJob uploadFileJob = new UploadFileJob(file, fileRef);
        mUploadFileJobs.add(uploadFileJob);
        // TODO: 2018. 10. 14. JYP Add recyclerview
        showJobStatus();

        int idx = mUploadFileJobs.indexOf(uploadFileJob);
        // TODO: 2018. 10. 14. JYP Show progress
        // TODO: 2018. 10. 14. JYP Implement pause, resume handler
    }

    private void showJobStatus() {
        StringBuilder sb = new StringBuilder();
        for (UploadFileJob each :
                mUploadFileJobs) {
            sb.append(String.format("[%03d] ", mUploadFileJobs.indexOf(each)));
            sb.append("File: ");
            sb.append(each.file.getName());
            sb.append("\t");
            sb.append("FileRef: ");
            sb.append(each.fileRef.getName());
            sb.append("\n");
        }
        ((TextView) findViewById(R.id.tv_helloworld)).setText(sb);
    }

    private void uploadFile(File file, StorageReference fileRef) {
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("audio/*")
                .setContentDisposition(file.getName())
                .build();

        UploadTask uploadTask = fileRef.putFile(Uri.fromFile(file), metadata);
        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                Log.d(TAG, "onProgress: Upload is " + progress + "% done");
            }
        }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG, "onPaused: Upload is paused");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d(TAG, "onFailure: Upload failed");
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG, "onSuccess: Upload succeeded");
                // Handle successful uploads on complete
                // ...
            }
        });
    }

    private void doesFileExistOnStorage(StorageReference fileRef, Consumer<Boolean> resultCallback) {
        fileRef.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata storageMetadata) {
                // Metadata now contains the metadata for 'images/forest.jpg'
                Log.d(TAG, "doesFileExistOnStorage: onSuccess: storageMetadata=" + storageMetadata);
                try {
                    resultCallback.accept(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Uh-oh, an error occurred!
                Log.d(TAG, "doesFileExistOnStorage: onFailure: ");
                try {
                    resultCallback.accept(false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private String enname(File file) {
        Log.d(TAG, "enname() called with: file = [" + file + "]");

        String prefixDateString = getPrefixDateString(file.lastModified());
        Log.d(TAG, "enname(): prefixDateString = [" + prefixDateString + "]");
        String ext = getFileExtFromName(file.getName());

        return prefixDateString + "_" + "_" + "." + ext;
    }

    private String getFileExtFromName(String name) {
        int seperatorIdx = name.lastIndexOf(".");
        if (seperatorIdx == -1) {
            return "";
        }

        return name.substring(seperatorIdx + 1, name.length());
    }

    private String getPrefixDateString(long timeInMillis) {
        Date date = new Date(timeInMillis);
        int yearFrom2000 = date.getYear() - 100;
        int monthInRealworld = date.getMonth() + 1;
        String dayOfWeekInString = getDayOfWeekInString(date.getDay());
        String ret = String.format("%2d%02d%02d_%02d_%s_", yearFrom2000, monthInRealworld, date.getDate(),
                date.getHours(), dayOfWeekInString);

        return ret;
    }

    private String getDayOfWeekInString(int day) {
        assert false;

        int dayInCalendar = day + 1;
        switch (dayInCalendar) {
            case Calendar.SUNDAY:
                return "Sun";
            case Calendar.MONDAY:
                return "Mon";
            case Calendar.TUESDAY:
                return "Tue";
            case Calendar.WEDNESDAY:
                return "Wed";
            case Calendar.THURSDAY:
                return "Thu";
            case Calendar.FRIDAY:
                return "Fri";
            case Calendar.SATURDAY:
                return "Sat";
        }

        assert false;
        return "";
    }
}
