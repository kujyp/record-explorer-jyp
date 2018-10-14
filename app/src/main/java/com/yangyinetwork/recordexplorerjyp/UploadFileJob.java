package com.yangyinetwork.recordexplorerjyp;

import com.google.firebase.storage.StorageReference;

import java.io.File;

public class UploadFileJob {
    public File file;
    public StorageReference fileRef;

    public UploadFileJob(File file, StorageReference fileRef) {
        this.file = file;
        this.fileRef = fileRef;
    }
}
