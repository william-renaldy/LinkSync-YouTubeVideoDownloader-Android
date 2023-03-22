package com.william.youtubevideodownloader;

import com.chaquo.python.PyObject;

public interface DownloadCallBack {
    void onSuccess(PyObject stream, PyObject chunk, PyObject bytes_remaining);
}
