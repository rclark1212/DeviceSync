package com.prod.rclark.devicesync.cloud;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.prod.rclark.devicesync.AppUtils;
import com.prod.rclark.devicesync.DBUtils;
import com.prod.rclark.devicesync.ImageDetail;
import com.prod.rclark.devicesync.ObjectDetail;
import com.prod.rclark.devicesync.Utils;
import com.prod.rclark.devicesync.data.AppContract;
import com.prod.rclark.devicesync.sync.GCESync;

import java.io.ByteArrayOutputStream;

/**
 * Created by rclark on 5/26/16.
 * Provides firebase services
 */
public class Firebase {
    private Context mCtx = null;
    private String mUser = null;
    private FirebaseDatabase mFirebaseDB = null;
    private ChildEventListener mFirebaseDeviceListener = null;
    private ChildEventListener mFirebaseImageListener = null;
    private ChildEventListener mFirebaseAppListener = null;
    private FirebaseStorage mFirebaseStorage = null;

    private static boolean bValue = false;

    // Message defines for communicating back to calling activity
    // Defined in GCESync

    private static final String TAG = "Firebase";
    private static final String DEVICES = "device";
    private static final String APPS = "apps";
    private static final String IMAGE = "image";

    /**
     * DATA STRUCTURE IN FIREBASE...
     * user(root)   \device \serial(s)  \deviceinfo
     *              \apps   \serial(s)  \app(s) \\appinfo
     *              \image  \filename   \imageinfo
     */

    private static final String NULL = "null";

    private static final String FIREBASE_STORAGE_BUCKET = "gs://project-4088008660350137649.appspot.com";

    /**
     * Constructor - give me my context and my user UUID
     * @param ctx
     * @param user
     */
    public Firebase(Context ctx, String user) {
        mCtx = ctx;
        mUser = user;
        mFirebaseDB = FirebaseDatabase.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();
    }

    /**
     * Pushes serial number record to firebase - both apps and device
     * @param serial
     */
    public void pushRecordsToFirebase(String serial) {
        Log.d(TAG, "Start Push");

        sendNetworkBusyIndicator(true);

        writeToFirebase(serial);

        //to tell user what devices got updated...
        //And finally, send a message back to indicate that we are all done with the local work
        Intent localIntent = new Intent(GCESync.BROADCAST_ACTION).putExtra(GCESync.EXTENDED_DATA_STATUS,
                GCESync.EXTENDED_DATA_STATUS_PUSHCOMPLETE);
        //And broadcast the message
        LocalBroadcastManager.getInstance(mCtx).sendBroadcast(localIntent);

        sendNetworkBusyIndicator(false);

        Log.d(TAG, "All done with record push");

    }

    /**
     * Unregister firebase listeners
     */
    public void unregisterFirebaseDataListeners() {
        if (mFirebaseDeviceListener == null) {
            return;
        }

        //get the database
        final DatabaseReference dataBase = mFirebaseDB.getReference();

        //and remove the device listener
        dataBase.child(mUser).child(DEVICES).removeEventListener(mFirebaseDeviceListener);

        //app listener
        dataBase.child(mUser).child(APPS).removeEventListener(mFirebaseAppListener);

        //finally remove the image listener
        dataBase.child(mUser).child(IMAGE).removeEventListener(mFirebaseImageListener);

        mFirebaseDeviceListener = null;
        mFirebaseImageListener = null;
        mFirebaseAppListener = null;
    }

    /**
     * Sets up data listeners for firebase (for data reads/syncs)
     */
    public void registerFirebaseDataListeners() {
        //have we already set up a listener?
        if (mFirebaseDeviceListener != null) {
            return;
        }

        //get the database
        final DatabaseReference dataBase = mFirebaseDB.getReference();

        Log.d(TAG, "Setting up firebase db listeners");
        sendNetworkBusyIndicator(true);

        //set up the serial number device listener
        mFirebaseDeviceListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                String key = dataSnapshot.getKey();

                Log.d(TAG, "Device child added - " + key);
                //was this under devices child?
                //add to CP
                ObjectDetail object = dataSnapshot.getValue(ObjectDetail.class);

                //by definition, this will be more up to date than our CP so just shove it in...
                if (object.serial != null) {
                    //as long as it is not our serial number...
                    if (!Build.SERIAL.equals(object.serial)) {
                        Log.d(TAG, "Adding new serial to CP " + object.serial);
                        DBUtils.saveDeviceToCP(mCtx, object);
                    }
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                String key = dataSnapshot.getKey();

                Log.d(TAG, "Device child changed - " + key);
                //update CP
                ObjectDetail object = dataSnapshot.getValue(ObjectDetail.class);

                if (object.serial != null) {
                    //Check if this is us that pushed. Do this by comparing timestamps
                    boolean bUpdate = true;
                    ObjectDetail cp_object = DBUtils.getDeviceFromCP(mCtx, object.serial);
                    if (object.timestamp <= cp_object.timestamp) {
                        //oh... an older instance... skip
                        bUpdate = false;
                    }
                    if (bUpdate) {
                        Log.d(TAG, "Updating device serial in CP " + object.serial);
                        DBUtils.saveDeviceToCP(mCtx, object);
                    } else {
                        Log.d(TAG, "Got an event for a device record with stale timestamp - must be due to our trigger. Punt on updating " + object.serial);
                    }
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                String key = dataSnapshot.getKey();

                Log.d(TAG, "Device child removed - " + key);
                //remove record from CP
                ObjectDetail object = dataSnapshot.getValue(ObjectDetail.class);
                if (object.serial != null) {
                    String serial = object.serial;
                    if (Build.SERIAL.equals(serial)) {
                        Log.d(TAG, "WARNING - deleting our own serial from CP");
                    }
                    Log.d(TAG, "Removing serial from CP " + serial);
                    DBUtils.deleteDeviceFromCP(mCtx, serial);
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                //do nothing here - don't care if child moved (and should never happen)
                Log.e(TAG, "Child moved - should never happen!!!");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Firebase onCancelled called in childlistener - reason:" + databaseError.getDetails());
            }
        };

        //set up the serial number app listener
        mFirebaseAppListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                String key = dataSnapshot.getKey();

                Log.d(TAG, "App child added - " + key);
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    Log.d(TAG, "App child adding - " + child.getKey());
                    //add to CP
                    ObjectDetail object = child.getValue(ObjectDetail.class);

                    //by definition, this will be more up to date than our CP if it is from another serial number so just shove it in...
                    //i.e. nobody should *ever* add an app for our serial number which is not already in our CP...
                    if (object.serial != null) {
                        if (!Build.SERIAL.equals(object.serial)) {
                            Log.d(TAG, "Adding new seria/app to CP " + object.serial + " " + object.pkg);
                            DBUtils.saveAppToCP(mCtx, object);
                        }
                    }
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                String key = dataSnapshot.getKey();

                Log.d(TAG, "App child changed - " + key);
                //update CP

                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    Log.d(TAG, "App child updating - " + child.getKey());
                    ObjectDetail object = child.getValue(ObjectDetail.class);

                    if (object.serial != null) {
                        //Check if this is us that pushed. Do this by comparing timestamps
                        boolean bUpdate = true;
                        ObjectDetail cp_object = DBUtils.getAppFromCP(mCtx, object.serial, object.pkg);
                        if (object.timestamp <= cp_object.timestamp) {
                            //oh... an older instance... skip
                            bUpdate = false;
                        }
                        if (bUpdate) {
                            Log.d(TAG, "Updating app serial/app in CP " + object.serial + " " + object.pkg);
                            DBUtils.saveAppToCP(mCtx, object);
                        } else {
                            Log.d(TAG, "Got an event for a device record with stale timestamp - must be due to our trigger. Punt on updating " + object.serial);
                        }
                    }
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                String key = dataSnapshot.getKey();

                Log.d(TAG, "App child removed - " + key);
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    Log.d(TAG, "App child deleting - " + child.getKey());

                    //remove record from CP
                    //Note - this should likely never trigger an actual CP removal (since will be removed from CP before we get here)
                    ObjectDetail object = child.getValue(ObjectDetail.class);
                    if (object.serial != null) {
                        String serial = object.serial;
                        Log.d(TAG, "Removing serial/App from CP " + serial + " " + object.pkg);
                        DBUtils.deleteAppFromCP(mCtx, serial, object.pkg);
                    }
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                //do nothing here - don't care if child moved (and should never happen)
                Log.e(TAG, "Child moved - should never happen!!!");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Firebase onCancelled called in childlistener - reason:" + databaseError.getDetails());
            }
        };

        //set up the photo selector device listener
        mFirebaseImageListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                String key = dataSnapshot.getKey();

                Log.d(TAG, "Image child added - " + key);
                //new photo!
                ImageDetail object = dataSnapshot.getValue(ImageDetail.class);

                if (object.filename != null) {
                    Log.d(TAG, "Adding new image to CP " + object.filename);
                    DBUtils.setImageRecordToCP(mCtx, object);
                    //and send a message in case there needs to be an update (in case of not using cursor loader for images)
                    Intent localIntent = new Intent(GCESync.BROADCAST_ACTION).putExtra(GCESync.EXTENDED_DATA_STATUS,
                            GCESync.EXTENDED_DATA_STATUS_PHOTO_COMPLETE);
                    LocalBroadcastManager.getInstance(mCtx).sendBroadcast(localIntent);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                String key = dataSnapshot.getKey();

                Log.d(TAG, "Image child changed - " + key);
                //image changed!
                ImageDetail object = dataSnapshot.getValue(ImageDetail.class);
                Log.d(TAG, "Image filename " + object.filename);
                Log.d(TAG, "Image URL " + object.download_url);

                if (object.filename != null) {
                    Log.d(TAG, "Updating image in CP " + object.filename);
                    DBUtils.setImageRecordToCP(mCtx, object);
                    //and send a message in case there needs to be an update (in case of not using cursor loader for images)
                    Intent localIntent = new Intent(GCESync.BROADCAST_ACTION).putExtra(GCESync.EXTENDED_DATA_STATUS,
                            GCESync.EXTENDED_DATA_STATUS_PHOTO_COMPLETE);
                    LocalBroadcastManager.getInstance(mCtx).sendBroadcast(localIntent);
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                String key = dataSnapshot.getKey();

                Log.d(TAG, "Image child removed - " + key);
                //photo removed
                ImageDetail object = dataSnapshot.getValue(ImageDetail.class);
                if (object.stripname != null) {
                    String removed = object.stripname;
                    Log.d(TAG, "Removing image from CP " + removed);
                    DBUtils.deleteImageRecordFromCP(mCtx, removed);
                    //and send a message in case there needs to be an update (in case of not using cursor loader for images)
                    Intent localIntent = new Intent(GCESync.BROADCAST_ACTION).putExtra(GCESync.EXTENDED_DATA_STATUS,
                            GCESync.EXTENDED_DATA_STATUS_PHOTO_COMPLETE);
                    LocalBroadcastManager.getInstance(mCtx).sendBroadcast(localIntent);
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                //do nothing here - don't care if child moved (and should never happen)
                Log.e(TAG, "Child moved - should never happen!!!");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Firebase onCancelled called in childlistener - reason:" + databaseError.getDetails());
            }
        };

        //and add the device listener
        dataBase.child(mUser).child(DEVICES).addChildEventListener(mFirebaseDeviceListener);

        //add the app listener...
        dataBase.child(mUser).child(APPS).addChildEventListener(mFirebaseAppListener);

        //finally add the photo listener
        dataBase.child(mUser).child(IMAGE).addChildEventListener(mFirebaseImageListener);

        sendNetworkBusyIndicator(false);
    }


    /**
     * Delete all files/data for user on firebase
     * @return
     */
    public void deleteAllFiles() {
        //delete the user's node
        DatabaseReference dataBase = mFirebaseDB.getReference();

        Log.d(TAG, "Erasing firebase db for user " + mUser);

        dataBase.child(mUser).removeValue();

        Log.d(TAG, "Deleting cloud files for user " + mUser);

        //Get bucket
        StorageReference storageRef = mFirebaseStorage.getReferenceFromUrl(FIREBASE_STORAGE_BUCKET);

        //move down to user and images
        StorageReference userRef = storageRef.child(mUser);
        userRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // File deleted successfully
                Log.d(TAG, "Successful deletion");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Uh-oh, an error occurred!
                Log.d(TAG, "Delete failed - " + exception.getMessage());
            }
        });
    }

    /**
     * Delete the file in firebase storage
     * @param photo
     * @return
     */
    public void deleteImage(String filename) {

        final String removefile = filename;
        Log.d(TAG, "Deleting image file " + filename);

        //Get bucket
        StorageReference storageRef = mFirebaseStorage.getReferenceFromUrl(FIREBASE_STORAGE_BUCKET);

        //move down to user and images
        StorageReference userRef = storageRef.child(mUser);
        StorageReference imagesRef = userRef.child(IMAGE);

        //Finally fileref
        StorageReference fileRef = imagesRef.child(filename);

        fileRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // File deleted successfully
                Log.d(TAG, "Successful image deletion");
                deleteImageKey(removefile);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Uh-oh, an error occurred!
                Log.d(TAG, "Delete failed - " + exception.getMessage());
            }
        });
    }

    /**
     * Copies the local file to the cloud/firebase. Note filename is a path+file.
     * @return
     */
    public void copyToFirebase(Drawable icon, String apkname) {

        if (icon == null) {
            return;
        }

        final String filename = apkname + ".jpg";

        Log.d(TAG, "Copying icon - " + filename);
        sendNetworkBusyIndicator(true);

        //Get bucket
        StorageReference storageRef = mFirebaseStorage.getReferenceFromUrl(FIREBASE_STORAGE_BUCKET);

        //move down to user and images
        StorageReference userRef = storageRef.child(mUser);
        StorageReference imagesRef = userRef.child(IMAGE);

        //get the last segment of filename (since it is really a path)
        final String apk = apkname;

        //Finally filerefs
        StorageReference fileRef = imagesRef.child(filename);

        //Get the bitmap...
        Bitmap bmap = Utils.drawableToBitmap(icon);

        //Get the source databuffer
        ByteArrayOutputStream bitmapStream = new ByteArrayOutputStream();
        bmap.compress(Bitmap.CompressFormat.JPEG, 100, bitmapStream);
        byte[] data = bitmapStream.toByteArray();
        UploadTask uploadTask = fileRef.putBytes(data);

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                Log.d(TAG, "Oops - upload to firebase failed!");
                sendNetworkBusyIndicator(false);
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                Log.d(TAG, "Upload to firebase succeeded!");
                final Uri downloadUrl = taskSnapshot.getDownloadUrl();
                addImageKey(filename, apk, downloadUrl);
                sendNetworkBusyIndicator(false);
            }
        });

        //All done
    }

    /**
     * No good way to enumerate firebase storage. So we put image files into the realtime data
     * base under image key
     * Adds an image key to realtime database
     * @param filename
     */
    public void addImageKey(String filename, String apk, Uri downloadurl) {
        DatabaseReference dataBase = mFirebaseDB.getReference();

        String stripfile = Utils.stripForFirebase(filename);
        Log.d(TAG, "Writing firebase image key " + stripfile);

        //TODO - just using filename for description - could improve here...
        ImageDetail image = new ImageDetail(stripfile, filename, apk, downloadurl.toString());

        if (stripfile != null) {
            //and push
            dataBase.child(mUser).child(IMAGE).child(stripfile).setValue(image);
        }
    }


    //  Deletes an image key from realtime database
    public void deleteImageKey(String filename) {
        DatabaseReference dataBase = mFirebaseDB.getReference();

        String stripfile = Utils.stripForFirebase(filename);

        Log.d(TAG, "Deleting firebase image key " + stripfile);

        if (stripfile != null) {
            //and push
            dataBase.child(mUser).child(IMAGE).child(stripfile).removeValue();
        }
    }

    /**
     * Delete the local CP record for the serial, delete record in the cloud and send a delete tickle out
     * @return
     */
    public void deleteFirebaseRecord(String serial) {
        Log.d(TAG, "Deleting serial " + serial);

        if (serial == null) {
            Log.d(TAG, "Someone passed in a null serial to deleteRecord - exit");
            return;
        }

        //delete the user's node
        DatabaseReference dataBase = mFirebaseDB.getReference();

        dataBase.child(mUser).child(DEVICES).child(serial).removeValue();
        dataBase.child(mUser).child(APPS).child(serial).removeValue();

        //Last delete the local record in CP - have to do this last as it will trigger a push update which will cause
        //all the remote devices to then do a merge...
        DBUtils.deleteDeviceFromCP(mCtx, serial);
        Uri appDB = AppContract.AppEntry.CONTENT_URI;
        //build up the local device query
        appDB = appDB.buildUpon().appendPath(serial).build();
        mCtx.getContentResolver().delete(appDB, null, null);
    }


    /**
     * Does what it says - writes device serial number from CP to firebase
     * @param serial
     */
    public void writeDeviceToFirebase(String serial) {
        DatabaseReference dataBase = mFirebaseDB.getReference();

        ObjectDetail device = DBUtils.getDeviceFromCP(mCtx, serial);
        if (device != null) {
            dataBase.child(mUser).child(DEVICES).child(device.serial).setValue(device);
        }
    }

    /**
     * Does what it says
     * @param serial
     */
    public void writeAppToFirebase(String serial, String apkname) {
        DatabaseReference dataBase = mFirebaseDB.getReference();

        ObjectDetail app = DBUtils.getAppFromCP(mCtx, serial, apkname);
        if (app != null) {
            dataBase.child(mUser).child(APPS).child(app.serial).child(Utils.stripForFirebase(app.pkg)).setValue(app);

            //FIXME - done and verify - firebase - now make this auto-magic for image pushing
            //check if this app is local
            if (DBUtils.isAppLocal(mCtx, apkname)) {
                //then check if this app's image exists already in photo cp...
                if (DBUtils.getImageRecordFromCP(mCtx, apkname) == null) {
                    Log.d(TAG, "Could not find image in CP for " + app.pkg + " - uploading");
                    //if not, then upload it...
                    //note - calling copy to firebase ends up generating the db key which in turn causes listener to create CP record
                    Drawable icon = AppUtils.getLocalApkImage(mCtx, app.pkg, app.type);
                    copyToFirebase(icon, app.pkg);
                } else {
                    Log.d(TAG, "Skip updating " + app.pkg + " image - already exists");
                }
            }
        }
    }


    /**
     * Does what it says
     * @param serial
     */
    public void deleteAppFromFirebase(String serial, String apkname) {
        DatabaseReference dataBase = mFirebaseDB.getReference();

        dataBase.child(mUser).child(APPS).child(serial).child(apkname).removeValue();

        //FIXME - done and verify - firebase - now make this auto-magic for image pushing
        //check if this app exists in the CP database at all for any serial number
        if (DBUtils.countApp(mCtx, apkname) == 0) {
            //if not, then delete photo...
            //get the photo...
            ImageDetail image = DBUtils.getImageRecordFromCP(mCtx, Utils.stripForFirebase(apkname));

            //delete photo in storage
            //this also deletes photo note in fb_db. which then ends up deleting node in CP
            deleteImage(image.filename);
        }
    }

    /**
     * writeRecordToFirebase - writes the serial number record to firebase
     */
    private void writeToFirebase(String serial) {
        DatabaseReference dataBase = mFirebaseDB.getReference();

        Log.d(TAG, "Writing firebase db");

        //write the device first...
        writeDeviceToFirebase(serial);

        //now the apps...
        //Set up the query
        Uri appDB = AppContract.AppEntry.CONTENT_URI;
        //set up a local app query
        Uri localApp = appDB.buildUpon().appendPath(serial).build();

        //grab the cursor
        Cursor c = mCtx.getContentResolver().query(localApp, null, null, null, null);

        if (c.getCount() > 0) {
            //Okay - we have a cursor with all the apps for the serial...
            //Loop through the app names
            for (int i = 0; i < c.getCount(); i++) {
                //move to position
                c.moveToPosition(i);

                //get the app name
                String appname = c.getString(c.getColumnIndex(AppContract.AppEntry.COLUMN_APP_PKG));

                //and write
                writeAppToFirebase(serial, appname);
            }
        }

        c.close();
    }



    /**
     * Routine to send message back to mainactivity to enable/stop loading/network indicator
     */
    private void sendNetworkBusyIndicator(boolean bEnable) {
        //Send back message to let activity know how to show UI
        Intent localIntent;
        if (bEnable) {
            localIntent = new Intent(GCESync.BROADCAST_ACTION).putExtra(GCESync.EXTENDED_DATA_STATUS, GCESync.MAINACTIVITY_SHOW_NETWORKBUSY);
        } else {
            localIntent = new Intent(GCESync.BROADCAST_ACTION).putExtra(GCESync.EXTENDED_DATA_STATUS, GCESync.MAINACTIVITY_SHOW_NETWORKFREE);
        }

        //And broadcast the message
        LocalBroadcastManager.getInstance(mCtx).sendBroadcast(localIntent);
    }

}
