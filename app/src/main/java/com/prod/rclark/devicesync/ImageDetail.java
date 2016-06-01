package com.prod.rclark.devicesync;

/**
 * Created by rclark on 5/31/2016.
 */
public class ImageDetail {
    public String stripname;
    public String filename;
    public String apkname;
    public String download_url;

    public ImageDetail() {
    }

    public ImageDetail(String stripname, String filename, String apkname, String download_url) {
        this.stripname = stripname;
        this.filename = filename;
        this.apkname = apkname;
        this.download_url = download_url;
    }
}
