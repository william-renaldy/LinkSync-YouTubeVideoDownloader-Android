package com.william.youtubevideodownloader;

import androidx.annotation.Nullable;

import com.chaquo.python.PyException;

public class CancelException extends PyException {


    @Nullable
    @Override
    public String getMessage() {
        return "Download Cancelled";
    }
}
