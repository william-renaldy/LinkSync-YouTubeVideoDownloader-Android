# import android
import pytube
import os
import datetime



def download_video(url, output_path,initial_callback, progress_callback):

    # Download the video
    initial_callback()
    current_time = datetime.datetime.now().timestamp()
    video = pytube.YouTube(url,on_progress_callback = progress_callback)
    stream = video.streams.get_highest_resolution()

    progress_callback(stream,True,0)
    filename = stream.download(output_path,skip_existing = False)

    os.utime(filename, (current_time, current_time))