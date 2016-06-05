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

    //compares the two classes along the data structures that matter
    public boolean isEqual(ImageDetail other) {
        if (other == null) {
            return false;
        }

        //stripname
        if ((this.stripname == null) && (other.stripname != null)) {
            return false;
        } else if ((this.stripname != null) && (other.stripname == null)) {
            return false;
        } else if ((this.stripname != null) && (other.stripname != null)) {
            if (!this.stripname.equals(other.stripname)) {
                return false;
            }
        }

        //filename
        if ((this.filename == null) && (other.filename != null)) {
            return false;
        } else if ((this.filename != null) && (other.filename == null)) {
            return false;
        } else if ((this.filename != null) && (other.filename != null)) {
            if (!this.filename.equals(other.filename)) {
                return false;
            }
        }

        //apkname
        if ((this.apkname == null) && (other.apkname != null)) {
            return false;
        } else if ((this.apkname != null) && (other.apkname == null)) {
            return false;
        } else if ((this.apkname != null) && (other.apkname != null)) {
            if (!this.apkname.equals(other.apkname)) {
                return false;
            }
        }

        //download_url
        if ((this.download_url == null) && (other.download_url != null)) {
            return false;
        } else if ((this.download_url != null) && (other.download_url == null)) {
            return false;
        } else if ((this.download_url != null) && (other.download_url != null)) {
            if (!this.download_url.equals(other.download_url)) {
                return false;
            }
        }

        return true;
    }
}
