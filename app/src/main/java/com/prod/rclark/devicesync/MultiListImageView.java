package com.prod.rclark.devicesync;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;

import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.prod.rclark.devicesync.data.AppContract;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rclark on 6/11/2016.
 * Extends ImageView to allow setting of a mosiac tile of apks.
 * This control could be genericized to take in a bunch of drawables but want to use async
 * nature of glide and customize it for DeviceSync.
 * Allow all the standard ImageView functions to exist. But add one additional to generate
 * the image from a list of apks.
 */

public class MultiListImageView extends ImageView {
    ArrayList<String> mAPKs=null;
    Context mCtx;
    Drawable mPlaceholder=null;
    Bitmap mBackBM=null;            //backing store
    Canvas mBackCanvas;             //and the canvas for backing store
    private static final String TAG = "MultiListImageView";

    /**
     * Constructors
     * @param context
     */
    public MultiListImageView(Context context) {
        super(context);
        mCtx = context;
    }
    public MultiListImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mCtx = context;
    }

    public MultiListImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mCtx = context;
    }

    /**
     * Placeholder images - NOTE - what is passed in determines the size/shape of the mosaic
     * @param drawable
     */
    public void setPlaceholderImage(Drawable drawable) {
        mPlaceholder = drawable;
        if (mBackBM == null) {
            setImageDrawable(mPlaceholder);
            Bitmap tempBM = Utils.drawableToBitmap(mPlaceholder);
            if (tempBM.isMutable()) {
                mBackBM = tempBM;
            } else {
                //Have to make it mutable
                mBackBM = tempBM.copy(Bitmap.Config.ARGB_8888, true);
            }
            mBackCanvas = new Canvas(mBackBM);
        }
    }

    public void setPlaceholderImage(int resid) {
        mPlaceholder = getResources().getDrawable(resid);
        if (mBackBM == null) {
            setImageDrawable(mPlaceholder);
            Bitmap tempBM = Utils.drawableToBitmap(mPlaceholder);
            if (tempBM.isMutable()) {
                mBackBM = tempBM;
            } else {
                //Have to make it mutable
                mBackBM = tempBM.copy(Bitmap.Config.ARGB_8888, true);
            }
            mBackCanvas = new Canvas(mBackBM);
        }
    }


    /**
     * This is the additive function to ImageView.
     * Take in a list of apks. And create a mosiac from it which then is set to ImageView.
     * Return false for error, true for success.
     * NOTE THAT PLACEHOLDER HAS TO BE SET FIRST SO WE CORRECTLY GET SIZE OF IMAGE
     * @param apklist
     */
    public boolean setImageFromAPKList(final Activity activity, ArrayList<String> apklist) {
        mAPKs = apklist;

        //check for errors
        if ((mBackBM == null) || (apklist == null) || (apklist.size() == 0)) {
            return false;
        }

        //create an array list to indicate in second pass what has already been drawn
        //(we do this as we don't want to perturb the apklist passed in)
        ArrayList<Boolean> drawn = new ArrayList<Boolean>();
        for (int i=0; i<apklist.size();i++) {
            drawn.add(false);
        }

        //Okay - ready to start processing!
        //Step 1 - figure out the tiles - 432/242 _or_ 1:1 - call it 1.6:1 and overdraw
        //first, what are our params?
        float aspect = 1.6f;
        int dy = mBackBM.getHeight();
        int dx = mBackBM.getWidth();
        int count = mAPKs.size();

        if ((dy == 0) || (dx == 0)) {
            return false;
        }

        //Figure out layout
        int adjustedX = (int)((float)dx/aspect);
        float displayaspect = (float)adjustedX/(float)dy;
        int cols = (int) Math.sqrt((double)((double)count*(double)displayaspect));
        int rows = count/cols;
        if ((count % cols) != 0) {
            rows++;
        }

        //and if our aspect ratio is 1.6, calc height/width
        int testheight = dy / rows;
        int testwidth = (int) (aspect*(float)testheight);

        //now verify with rounding we still fix (y is where we will have problem...)
        int resultY = rows*testheight;
        if (resultY > dy) {
            float shrink = (float)dy/(float)resultY;
            testwidth = (int)((float)testwidth*shrink);
            testheight = (int) ((float)testheight*shrink);
        }

        final int tileheight = testheight;
        final int tilewidth = testwidth;

        //okay - we have our targets... and create offsets for margins
        int marginx = (dx - tilewidth*cols)/2;
        int marginy = (dy % tileheight)/2;
        long type = Utils.bIsThisATV(mCtx) ? AppContract.TYPE_ATV : AppContract.TYPE_TABLET;

        //okay - we are ready to draw...
        //Do 2 loops though - what is local that can be done immediately...
        for (int i=0; i < mAPKs.size(); i++) {
            //Have we drawn this? (should always be false)
            if (!drawn.get(i)) {
                //So lets see if we can load from local...
                Drawable banner = AppUtils.getLocalApkImage(mCtx, mAPKs.get(i), type);

                if (banner != null) {
                    //okay, get the canvas destination rect first...
                    int destcol = i % cols;
                    int destrow = i / cols;
                    int sx = destcol * tilewidth + marginx;
                    int sy = destrow * tileheight + marginy;

                    banner.setBounds(sx, sy, sx + tilewidth, sy + tileheight);
                    //and draw into our backing canvas
                    banner.draw(mBackCanvas);

                    //Set this index as drawn (don't change index ordering - we use it to map location in mosaic)
                    drawn.set(i, true);
                }
            }
        }

        //Okay - on to step2 - download from network...
        for (int i=0; i < mAPKs.size(); i++) {

            if (!drawn.get(i)) {
                //okay, get the canvas destination rect first...
                final int destcol = i % cols;
                final int destrow = i / cols;
                final int sx = destcol * tilewidth + marginx;
                final int sy = destrow * tileheight + marginy;

                final Rect destRect = new Rect(sx, sy, sx+tilewidth, sy+tileheight);
                final Rect srcRect = new Rect(0, 0, tilewidth, tileheight);

                //glide it in... leave blank for testing for now
                //get the download URL...
                ImageDetail image = DBUtils.getImageRecordFromCP(mCtx, mAPKs.get(i));
                //glide it in
                if (image != null) {
                    Log.d(TAG, "Gliding in " + image.download_url);
                    Glide.with(mCtx)
                            .load(image.download_url)
                            .asBitmap()
                            .centerCrop()
                            .error(R.drawable.noimage)
                            .into(new SimpleTarget<Bitmap>(tilewidth, tileheight) {
                                @Override
                                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                                    //draw it
                                    //but only if not recycled already (user may have already cancelled)
                                    if (!mBackBM.isRecycled() && !resource.isRecycled()) {
                                        mBackCanvas.drawBitmap(resource, srcRect, destRect, null);
                                        resource.recycle();
                                        activity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                // Run this on UI thread. Update the bitmap again
                                                setImageBitmap(mBackBM);
                                            }
                                        });
                                    }
                                }
                            });
                } else {
                    Drawable banner = mCtx.getDrawable(R.drawable.noimage);
                    banner.setBounds(sx, sy, sx+tilewidth, sy+tileheight);
                    banner.draw(mBackCanvas);
                }
            }
        }

        //set ImageView here (as initial setting)
        this.setImageBitmap(mBackBM);

        //all done...
        return true;
    }

}
