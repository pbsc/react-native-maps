package com.airbnb.android.react.maps;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.animation.ObjectAnimator;
import android.util.Property;
import android.animation.TypeEvaluator;

import androidx.annotation.Nullable;

import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.DraweeHolder;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.drawable.DrawableFactory;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.image.QualityInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.react.bridge.ReadableMap;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import org.json.JSONException;
import org.json.JSONObject;

public class AirMapMarker extends AirMapFeature {

  private MarkerOptions markerOptions;
  private Marker marker;
  private int width;
  private int height;
  private String identifier;

  private LatLng position;
  private String title;
  private String snippet;

  private boolean anchorIsSet;
  private float anchorX;
  private float anchorY;

  private AirMapCallout calloutView;
  private View wrappedCalloutView;
  private final Context context;

  private float markerHue = 0.0f; // should be between 0 and 360
  private BitmapDescriptor iconBitmapDescriptor;
  private Bitmap iconBitmap;

  private float rotation = 0.0f;
  private boolean flat = false;
  private boolean draggable = false;
  private int zIndex = 0;
  private float opacity = 1.0f;

  private float calloutAnchorX;
  private float calloutAnchorY;
  private boolean calloutAnchorIsSet;

  private boolean tracksViewChanges = true;
  private boolean tracksViewChangesActive = false;
  private boolean hasViewChanges = true;

  private boolean hasCustomMarkerView = false;
  private final AirMapMarkerManager markerManager;
  private String imageUri;

  private final DraweeHolder<?> logoHolder;
  private DataSource<CloseableReference<CloseableImage>> dataSource;
  private final ControllerListener<ImageInfo> mLogoControllerListener =
      new BaseControllerListener<ImageInfo>() {
        @Override
        public void onFinalImageSet(
            String id,
            @Nullable final ImageInfo imageInfo,
            @Nullable Animatable animatable) {
          CloseableReference<CloseableImage> imageReference = null;
          try {
            imageReference = dataSource.getResult();
            if (imageReference != null) {
              CloseableImage image = imageReference.get();
              if (image != null && image instanceof CloseableStaticBitmap) {
                CloseableStaticBitmap closeableStaticBitmap = (CloseableStaticBitmap) image;
                Bitmap bitmap = closeableStaticBitmap.getUnderlyingBitmap();
                if (bitmap != null) {
                  bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                  iconBitmap = bitmap;
                  iconBitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
                }
              }
              if(image != null && image instanceof CloseableSvgImage) {
                SVG svgImage = ((CloseableSvgImage) image).getSvg();
                Bitmap  newBM = Bitmap.createBitmap((int) Math.ceil(svgImage.getDocumentWidth()),
                        (int) Math.ceil(svgImage.getDocumentHeight()),
                        Bitmap.Config.ARGB_8888);
                Canvas  bmcanvas = new Canvas(newBM);
                svgImage.renderToCanvas(bmcanvas);
                if (newBM != null) {
                  newBM = newBM.copy(Bitmap.Config.ARGB_8888, true);
                  iconBitmap = newBM;
                  iconBitmapDescriptor = BitmapDescriptorFactory.fromBitmap(newBM);
                }
              }
            }
          } finally {
            dataSource.close();
            if (imageReference != null) {
              CloseableReference.closeSafely(imageReference);
            }
          }
          if (AirMapMarker.this.markerManager != null && AirMapMarker.this.imageUri != null) {
            AirMapMarker.this.markerManager.getSharedIcon(AirMapMarker.this.imageUri)
                .updateIcon(iconBitmapDescriptor, iconBitmap);
          }
          update(true);
        }
      };

  public AirMapMarker(Context context, AirMapMarkerManager markerManager) {
    super(context);
    this.context = context;
    this.markerManager = markerManager;
    logoHolder = DraweeHolder.create(createDraweeHierarchy(), context);
    logoHolder.onAttach();
  }

  public AirMapMarker(Context context, MarkerOptions options, AirMapMarkerManager markerManager) {
    super(context);
    this.context = context;
    this.markerManager = markerManager;
    logoHolder = DraweeHolder.create(createDraweeHierarchy(), context);
    logoHolder.onAttach();

    position = options.getPosition();
    setAnchor(options.getAnchorU(), options.getAnchorV());
    setCalloutAnchor(options.getInfoWindowAnchorU(), options.getInfoWindowAnchorV());
    setTitle(options.getTitle());
    setSnippet(options.getSnippet());
    setRotation(options.getRotation());
    setFlat(options.isFlat());
    setDraggable(options.isDraggable());
    setZIndex(Math.round(options.getZIndex()));
    setAlpha(options.getAlpha());
    iconBitmapDescriptor = options.getIcon();
  }

  private GenericDraweeHierarchy createDraweeHierarchy() {
    return new GenericDraweeHierarchyBuilder(getResources())
        .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
        .setFadeDuration(0)
        .build();
  }

  public void setCoordinate(ReadableMap coordinate) {
    position = new LatLng(coordinate.getDouble("latitude"), coordinate.getDouble("longitude"));
    if (marker != null) {
      marker.setPosition(position);
    }
    update(false);
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
    update(false);
  }

  public String getIdentifier() {
    return this.identifier;
  }

  public void setTitle(String title) {
    this.title = title;
    if (marker != null) {
      marker.setTitle(title);
    }
    update(false);
  }

  public String getCFSvg(Integer level,  String topOutline, String bottomOutline, String topInner, String bottomInner) {
    String topInnerEncoded = topInner.replace("#", "%23");
    String topOutlineEncoded = topOutline.replace("#", "%23");
    String bottomOutlineEncoded = bottomOutline.replace("#", "%23");
    String bottomInnerEncoded = bottomInner.replace("#", "%23");
    final float scale = getResources().getDisplayMetrics().density;
    DisplayMetrics displayMetrics = new DisplayMetrics();
    ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE))
            .getDefaultDisplay().getMetrics(displayMetrics);
    int height = displayMetrics.heightPixels;
    int width = displayMetrics.widthPixels;
    double scaleConstant = (Math.sqrt(Math.pow(height, 2) + Math.pow(width, 2))/Math.sqrt(Math.pow(1080, 2) + Math.pow(2076, 2)));
    double svgWidth = 23.33 * scale;
    double svgHeight = 63.67 * scale;
    if (level == 0) {
      // svg with level 0
      String svgSrc = "%3Csvg xmlns='http://www.w3.org/2000/svg' width='%%width%%' height='%%height%%' viewBox='0 0 210 300'%3E%3Cg id='_100'%3E%3Cpath fill='%2$s' d='M194.67,80.76C184.16,42.18,148,13.51,105,13.51S25.84,42.18,15.33,80.76a88.07,88.07,0,0,0-3.09,23.14c0,13.62,2.3,28.63,6.49,44.1a281.12,281.12,0,0,0,28.59,67.24c15.93,27.23,36,51.54,57.68,67.25,21.65-15.71,41.76-40,57.68-67.25A281.12,281.12,0,0,0,191.27,148c4.18-15.47,6.49-30.48,6.49-44.1A88.07,88.07,0,0,0,194.67,80.76Z'/%3E%3Cpath fill='%1$s' d='M207.64,80.76C197.46,34.77,155.43,0,105,0S12.54,34.77,2.36,80.76A99.59,99.59,0,0,0,0,102.31C0,116.64,2.26,132.12,6.38,148c5.79,22.32,15.26,45.43,27.33,67.24,18.83,34,44,64.94,71.29,84.76,27.32-19.82,52.46-50.71,71.29-84.76,12.07-21.81,21.54-44.92,27.33-67.24,4.12-15.88,6.38-31.36,6.38-45.69A99.59,99.59,0,0,0,207.64,80.76Zm-45,134.48c-15.92,27.23-36,51.54-57.68,67.25-21.65-15.71-41.75-40-57.68-67.25A281.12,281.12,0,0,1,18.73,148c-4.19-15.47-6.49-30.48-6.49-44.1a88.07,88.07,0,0,1,3.09-23.14C25.84,42.18,62,13.51,105,13.51s79.16,28.67,89.67,67.25a88.07,88.07,0,0,1,3.09,23.14c0,13.62-2.31,28.63-6.49,44.1A281.12,281.12,0,0,1,162.68,215.24Z'/%3E%3C/g%3E%3C/svg%3E";
      return "data:image/svg+xml," + svgSrc.replace("%2$s", topInnerEncoded).replace("%1$s", topOutlineEncoded).replace("%%width%%", Double.toString(svgWidth)).replace("%%height%%", Double.toString(svgHeight));
    }

    if (level > 0 && level <= 37) {
      // svg with level 25
      String svgSrc = "%3Csvg xmlns='http://www.w3.org/2000/svg' width='%%width%%' height='%%height%%' viewBox='0 0 210 300'%3E%3Cg id='_100' data-name='100'%3E%3Cpath fill='%1$s' class='cls-1' d='M194.67,80.76C184.16,42.18,148,13.51,105,13.51S25.84,42.18,15.33,80.76a88.07,88.07,0,0,0-3.09,23.14c0,13.62,2.3,28.63,6.49,44.1a281.12,281.12,0,0,0,28.59,67.24H162.68A281.12,281.12,0,0,0,191.27,148c4.18-15.47,6.49-30.48,6.49-44.1A88.07,88.07,0,0,0,194.67,80.76Z'/%3E%3Cpath fill='%2$s' class='cls-2' d='M105,282.49c21.65-15.71,41.76-40,57.68-67.25H47.32C63.25,242.47,83.35,266.78,105,282.49Z'/%3E%3Cpath fill='%3$s' class='cls-3' d='M105,282.49c-21.65-15.71-41.75-40-57.68-67.25H33.71c18.83,34,44,64.94,71.29,84.76,27.32-19.82,52.46-50.71,71.29-84.76H162.68C146.76,242.47,126.65,266.78,105,282.49Z'/%3E%3Cpath fill='%4$s' d='M207.64,80.76C197.46,34.77,155.43,0,105,0S12.54,34.77,2.36,80.76A99.59,99.59,0,0,0,0,102.31C0,116.64,2.26,132.12,6.38,148c5.79,22.32,15.26,45.43,27.33,67.24H47.32A281.12,281.12,0,0,1,18.73,148c-4.19-15.47-6.49-30.48-6.49-44.1a88.07,88.07,0,0,1,3.09-23.14C25.84,42.18,62,13.51,105,13.51s79.16,28.67,89.67,67.25a88.07,88.07,0,0,1,3.09,23.14c0,13.62-2.31,28.63-6.49,44.1a281.12,281.12,0,0,1-28.59,67.24h13.61c12.07-21.81,21.54-44.92,27.33-67.24,4.12-15.88,6.38-31.36,6.38-45.69A99.59,99.59,0,0,0,207.64,80.76Z'/%3E%3C/g%3E%3C/svg%3E";
      return "data:image/svg+xml," + svgSrc.replace("%1$s", topInnerEncoded).replace("%2$s", bottomInner).replace("%3$s", bottomOutlineEncoded).replace("%4$s", topOutlineEncoded).replace("%%width%%", Double.toString(svgWidth)).replace("%%height%%", Double.toString(svgHeight));
    }

    if (level > 37 && level <= 62) {
      // svg with level 50
      String svgSrc = "%3Csvg xmlns='http://www.w3.org/2000/svg' width='%%width%%' height='%%height%%' viewBox='0 0 210 300'%3E%3Cg id='_100' data-name='100'%3E%3Cpath class='cls-1' fill='%1$s' d='M194.67,81c-10.43-38.6-46.48-67.34-89.52-67.44S25.93,42,15.33,80.56a88.09,88.09,0,0,0-3.14,23.14c0,13.62,2.24,28.63,6.39,44.11l172.55.38c4.21-15.46,6.55-30.47,6.58-44.09A87.76,87.76,0,0,0,194.67,81Z'/%3E%3Cpath class='cls-2' fill='%2$s' d='M18.73,148a281.12,281.12,0,0,0,28.59,67.24c15.93,27.23,36,51.54,57.68,67.25,21.65-15.71,41.76-40,57.68-67.25A281.12,281.12,0,0,0,191.27,148Z'/%3E%3Cpath class='cls-3' fill='%3$s' d='M207.64,80.76C197.46,34.77,155.43,0,105,0S12.54,34.77,2.36,80.76A99.59,99.59,0,0,0,0,102.31C0,116.64,2.26,132.12,6.38,148H18.73c-4.19-15.47-6.49-30.48-6.49-44.1a88.07,88.07,0,0,1,3.09-23.14C25.84,42.18,62,13.51,105,13.51s79.16,28.67,89.67,67.25a88.07,88.07,0,0,1,3.09,23.14c0,13.62-2.31,28.63-6.49,44.1h12.35c4.12-15.88,6.38-31.36,6.38-45.69A99.59,99.59,0,0,0,207.64,80.76Z'/%3E%3Cpath fill='%4$s' class='cls-4' d='M191.27,148a281.12,281.12,0,0,1-28.59,67.24c-15.92,27.23-36,51.54-57.68,67.25-21.65-15.71-41.75-40-57.68-67.25A281.12,281.12,0,0,1,18.73,148H6.38c5.79,22.32,15.26,45.43,27.33,67.24,18.83,34,44,64.94,71.29,84.76,27.32-19.82,52.46-50.71,71.29-84.76,12.07-21.81,21.54-44.92,27.33-67.24Z'/%3E%3C/g%3E%3C/svg%3E";
      return "data:image/svg+xml," + svgSrc.replace("%1$s", topInnerEncoded).replace("%2$s", bottomInnerEncoded).replace("%3$s", topOutlineEncoded).replace("%4$s", bottomOutlineEncoded).replace("%%width%%", Double.toString(svgWidth)).replace("%%height%%", Double.toString(svgHeight));
    }

    if (level > 62 && level <= 88) {
      // svg with level 75
      String svgSrc = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='%%width%%' height='%%height%%' viewBox='0 0 210 300'%3E%3Cg id='_100' data-name='100'%3E%3Cpath class='cls-1' fill='%1$s' d='M105,13.51c-43,0-79.16,28.67-89.67,67.25H194.67C184.16,42.18,148,13.51,105,13.51Z'/%3E%3Cpath fill='%2$s' class='cls-2' d='M194.67,80.76H15.33a88.07,88.07,0,0,0-3.09,23.14c0,13.62,2.3,28.63,6.49,44.1a281.12,281.12,0,0,0,28.59,67.24c15.93,27.23,36,51.54,57.68,67.25,21.65-15.71,41.76-40,57.68-67.25A281.12,281.12,0,0,0,191.27,148c4.18-15.47,6.49-30.48,6.49-44.1A88.07,88.07,0,0,0,194.67,80.76Z'/%3E%3Cpath fill='%3$s' class='cls-3' d='M105,13.51c43,0,79.16,28.67,89.67,67.25h13C197.46,34.77,155.43,0,105,0S12.54,34.77,2.36,80.76h13C25.84,42.18,62,13.51,105,13.51Z'/%3E%3Cpath fill='%4$s' class='cls-4' d='M207.64,80.76h-13a88.07,88.07,0,0,1,3.09,23.14c0,13.62-2.31,28.63-6.49,44.1a281.12,281.12,0,0,1-28.59,67.24c-15.92,27.23-36,51.54-57.68,67.25-21.65-15.71-41.75-40-57.68-67.25A281.12,281.12,0,0,1,18.73,148c-4.19-15.47-6.49-30.48-6.49-44.1a88.07,88.07,0,0,1,3.09-23.14h-13A99.59,99.59,0,0,0,0,102.31C0,116.64,2.26,132.12,6.38,148c5.79,22.32,15.26,45.43,27.33,67.24,18.83,34,44,64.94,71.29,84.76,27.32-19.82,52.46-50.71,71.29-84.76,12.07-21.81,21.54-44.92,27.33-67.24,4.12-15.88,6.38-31.36,6.38-45.69A99.59,99.59,0,0,0,207.64,80.76Z'/%3E%3C/g%3E%3C/svg%3E";
      return svgSrc.replace("%1$s", topInnerEncoded).replace("%2$s", bottomInnerEncoded).replace("%3$s", topOutlineEncoded).replace("%4$s", bottomOutlineEncoded).replace("%%width%%", Double.toString(svgWidth)).replace("%%height%%", Double.toString(svgHeight));
    }
    // svg with level 100
    String svgSrc = "%3Csvg xmlns='http://www.w3.org/2000/svg' width='%%width%%' height='%%height%%' viewBox='0 0 210 300'%3E%3Cg id='_100' data-name='100'%3E%3Cpath class='cls-1' fill='%1$s' d='M194.67,80.76C184.16,42.18,148,13.51,105,13.51S25.84,42.18,15.33,80.76a88.07,88.07,0,0,0-3.09,23.14c0,13.62,2.3,28.63,6.49,44.1a281.12,281.12,0,0,0,28.59,67.24c15.93,27.23,36,51.54,57.68,67.25,21.65-15.71,41.76-40,57.68-67.25A281.12,281.12,0,0,0,191.27,148c4.18-15.47,6.49-30.48,6.49-44.1A88.07,88.07,0,0,0,194.67,80.76Z'/%3E%3Cpath fill='%2$s' class='cls-2' d='M207.64,80.76C197.46,34.77,155.43,0,105,0S12.54,34.77,2.36,80.76A99.59,99.59,0,0,0,0,102.31C0,116.64,2.26,132.12,6.38,148c5.79,22.32,15.26,45.43,27.33,67.24,18.83,34,44,64.94,71.29,84.76,27.32-19.82,52.46-50.71,71.29-84.76,12.07-21.81,21.54-44.92,27.33-67.24,4.12-15.88,6.38-31.36,6.38-45.69A99.59,99.59,0,0,0,207.64,80.76ZM191.27,148a281.12,281.12,0,0,1-28.59,67.24c-15.92,27.23-36,51.54-57.68,67.25-21.65-15.71-41.75-40-57.68-67.25A281.12,281.12,0,0,1,18.73,148c-4.19-15.47-6.49-30.48-6.49-44.1a88.07,88.07,0,0,1,3.09-23.14C25.84,42.18,62,13.51,105,13.51s79.16,28.67,89.67,67.25a88.07,88.07,0,0,1,3.09,23.14C197.76,117.52,195.45,132.53,191.27,148Z'/%3E%3C/g%3E%3C/svg%3E";
    return "data:image/svg+xml," + svgSrc.replace("%1$s", bottomInnerEncoded).replace("%2$s", bottomOutlineEncoded).replace("%%width%%", Double.toString(svgWidth)).replace("%%height%%", Double.toString(svgHeight));

  }

  public void setSnippet(String snippet) {
    this.snippet = snippet;
    if (marker != null) {
      marker.setSnippet(snippet);
    }
    update(false);
  }

  public void setRotation(float rotation) {
    this.rotation = rotation;
    if (marker != null) {
      marker.setRotation(rotation);
    }
    update(false);
  }

  public void setFlat(boolean flat) {
    this.flat = flat;
    if (marker != null) {
      marker.setFlat(flat);
    }
    update(false);
  }

  public void setDraggable(boolean draggable) {
    this.draggable = draggable;
    if (marker != null) {
      marker.setDraggable(draggable);
    }
    update(false);
  }

  public void setZIndex(int zIndex) {
    this.zIndex = zIndex;
    if (marker != null) {
      marker.setZIndex(zIndex);
    }
    update(false);
  }

  public void setOpacity(float opacity) {
    this.opacity = opacity;
    if (marker != null) {
      marker.setAlpha(opacity);
    }
    update(false);
  }

  public void setMarkerHue(float markerHue) {
    this.markerHue = markerHue;
    update(false);
  }

  public void setAnchor(double x, double y) {
    anchorIsSet = true;
    anchorX = (float) x;
    anchorY = (float) y;
    if (marker != null) {
      marker.setAnchor(anchorX, anchorY);
    }
    update(false);
  }

  public void setCalloutAnchor(double x, double y) {
    calloutAnchorIsSet = true;
    calloutAnchorX = (float) x;
    calloutAnchorY = (float) y;
    if (marker != null) {
      marker.setInfoWindowAnchor(calloutAnchorX, calloutAnchorY);
    }
    update(false);
  }

  public void setTracksViewChanges(boolean tracksViewChanges) {
    this.tracksViewChanges = tracksViewChanges;
    updateTracksViewChanges();
  }

  private void updateTracksViewChanges() {
    boolean shouldTrack = tracksViewChanges && hasCustomMarkerView && marker != null;
    if (shouldTrack == tracksViewChangesActive) return;
    tracksViewChangesActive = shouldTrack;

    if (shouldTrack) {
      ViewChangesTracker.getInstance().addMarker(this);
    } else {
      ViewChangesTracker.getInstance().removeMarker(this);

      // Let it render one more time to avoid race conditions.
      // i.e. Image onLoad ->
      //      ViewChangesTracker may not get a chance to render ->
      //      setState({ tracksViewChanges: false }) ->
      //      image loaded but not rendered.
      updateMarkerIcon();
    }
  }

  public boolean updateCustomForTracking() {
    if (!tracksViewChangesActive)
      return false;

    updateMarkerIcon();

    return true;
  }

  public void updateMarkerIcon() {
    if (marker == null) return;

    if (!hasCustomMarkerView) {
      // No more updates for this, as it's a simple icon
      hasViewChanges = false;
    }
    if (marker != null) {
      marker.setIcon(getIcon());
    }
  }

  public static class CloseableSvgImage extends CloseableImage {

    private final SVG mSvg;

    private boolean mClosed = false;

    public CloseableSvgImage(SVG svg) {
      mSvg = svg;
    }

    public SVG getSvg() {
      return mSvg;
    }

    @Override
    public int getSizeInBytes() {
      return 0;
    }

    @Override
    public void close() {
      mClosed = true;
    }

    @Override
    public boolean isClosed() {
      return mClosed;
    }

    @Override
    public int getWidth() {
      return 0;
    }

    @Override
    public int getHeight() {
      return 0;
    }
  }

  public static class SvgDecoder implements ImageDecoder {

    @Override
    public CloseableImage decode(
            EncodedImage encodedImage,
            int length,
            QualityInfo qualityInfo,
            ImageDecodeOptions options) {
      try {
        SVG svg = SVG.getFromInputStream(encodedImage.getInputStream());
        return new CloseableSvgImage(svg);
      } catch (SVGParseException e) {
        e.printStackTrace();
      }
      return null;
    }
  }

  public LatLng interpolate(float fraction, LatLng a, LatLng b) {
    double lat = (b.latitude - a.latitude) * fraction + a.latitude;
    double lng = (b.longitude - a.longitude) * fraction + a.longitude;
    return new LatLng(lat, lng);
  }

  public void animateToCoodinate(LatLng finalPosition, Integer duration) {
    TypeEvaluator<LatLng> typeEvaluator = new TypeEvaluator<LatLng>() {
      @Override
      public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
        return interpolate(fraction, startValue, endValue);
      }
    };
    Property<Marker, LatLng> property = Property.of(Marker.class, LatLng.class, "position");
    ObjectAnimator animator = ObjectAnimator.ofObject(
      marker,
      property,
      typeEvaluator,
      finalPosition);
    animator.setDuration(duration);
    animator.start();
  }

  public static class SvgDrawableFactory implements DrawableFactory {

    @Override
    public boolean supportsImageType(CloseableImage image) {
      return image instanceof CloseableSvgImage;
    }

    @Nullable
    @Override
    public Drawable createDrawable(CloseableImage image) {
      return new SvgPictureDrawable(((CloseableSvgImage) image).getSvg());
    }
  }

  public static class SvgPictureDrawable extends PictureDrawable {

    private final SVG mSvg;

    public SvgPictureDrawable(SVG svg) {
      super(null);
      mSvg = svg;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
      super.onBoundsChange(bounds);
      setPicture(mSvg.renderToPicture(bounds.width(), bounds.height()));
    }
  }

  public void setImage(String uri) {
    hasViewChanges = true;

    boolean shouldLoadImage = true;

    if (this.markerManager != null) {
      // remove marker from previous shared icon if needed, to avoid future updates from it.
      // remove the shared icon completely if no markers on it as well.
      // this is to avoid memory leak due to orphan bitmaps.
      //
      // However in case where client want to update all markers from icon A to icon B
      // and after some time to update back from icon B to icon A
      // it may be better to keep it though. We assume that is rare.
      if (this.imageUri != null) {
        this.markerManager.getSharedIcon(this.imageUri).removeMarker(this);
        this.markerManager.removeSharedIconIfEmpty(this.imageUri);
      }
      if (uri != null) {
        // listening for marker bitmap descriptor update, as well as check whether to load the image.
        AirMapMarkerManager.AirMapMarkerSharedIcon sharedIcon = this.markerManager.getSharedIcon(uri);
        sharedIcon.addMarker(this);
        shouldLoadImage = sharedIcon.shouldLoadImage();
      }
    }

    this.imageUri = uri;
    if (!shouldLoadImage) {return;}

    Log.d("URI log", uri);

    if (uri == null) {
      iconBitmapDescriptor = null;
      update(true);
    } else if (uri.startsWith("http://") || uri.startsWith("https://") ||
        uri.startsWith("file://") || uri.startsWith("asset://") || uri.startsWith("data:")) {
      ImageRequest imageRequest = ImageRequestBuilder
          .newBuilderWithSource(Uri.parse(uri))
          .build();

      ImagePipeline imagePipeline = Fresco.getImagePipeline();
      dataSource = imagePipeline.fetchDecodedImage(imageRequest, this);
      DraweeController controller = Fresco.newDraweeControllerBuilder()
          .setImageRequest(imageRequest)
          .setControllerListener(mLogoControllerListener)
          .setOldController(logoHolder.getController())
          .build();
      logoHolder.setController(controller);
    } else if(uri.startsWith("useCfMarker")) {
      String jsonString = uri.split("JSON:")[1];

      try {
        JSONObject jsonObj = new JSONObject(jsonString);
        String  topOutline = jsonObj.getString("topOutline");
        String bottomOutline = jsonObj.getString("bottomOutline");
        String  topInner = jsonObj.getString("topInner");
        String bottomInner = jsonObj.getString("bottomInner");
        String level = jsonObj.getString("level");
        String svgDataUrI = getCFSvg(Math.round(Float.parseFloat(level)), topOutline, bottomOutline, topInner, bottomInner);
        Log.d("JSONString", svgDataUrI);
        ImageRequest imageRequest = ImageRequestBuilder
                .newBuilderWithSource(Uri.parse(svgDataUrI))
                .setImageDecodeOptions(
                        ImageDecodeOptions.newBuilder()
                                .setCustomImageDecoder(new SvgDecoder())
                                .build())
                .build();

        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        dataSource = imagePipeline.fetchDecodedImage(imageRequest, this);
        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setImageRequest(imageRequest)
                .setCustomDrawableFactory(new SvgDrawableFactory())
                .setControllerListener(mLogoControllerListener)
                .setOldController(logoHolder.getController())
                .build();
        logoHolder.setController(controller);
      } catch (JSONException e) {
        Log.d("CF Svg", "unable to parse JSON");
      }
    } else {
      iconBitmapDescriptor = getBitmapDescriptorByName(uri);
      if (iconBitmapDescriptor != null) {
          int drawableId = getDrawableResourceByName(uri);
          iconBitmap = BitmapFactory.decodeResource(getResources(), drawableId);
          if (iconBitmap == null) { // VectorDrawable or similar
              Drawable drawable = getResources().getDrawable(drawableId);
              iconBitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
              drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
              Canvas canvas = new Canvas(iconBitmap);
              drawable.draw(canvas);
          }
      }
      if (this.markerManager != null && uri != null) {
        this.markerManager.getSharedIcon(uri).updateIcon(iconBitmapDescriptor, iconBitmap);
      }
      update(true);
    }
  }

  public void setIconBitmapDescriptor(BitmapDescriptor bitmapDescriptor, Bitmap bitmap) {
    this.iconBitmapDescriptor = bitmapDescriptor;
    this.iconBitmap = bitmap;
    this.hasViewChanges = true;
    this.update(true);
  }

  public void setIconBitmap(Bitmap bitmap) {
    this.iconBitmap = bitmap;
  }

  public MarkerOptions getMarkerOptions() {
    if (markerOptions == null) {
      markerOptions = new MarkerOptions();
    }

    fillMarkerOptions(markerOptions);
    return markerOptions;
  }

  @Override
  public void addView(View child, int index) {
    super.addView(child, index);
    // if children are added, it means we are rendering a custom marker
    if (!(child instanceof AirMapCallout)) {
      hasCustomMarkerView = true;
      updateTracksViewChanges();
    }
    update(true);
  }

  @Override
  public void requestLayout() {
    super.requestLayout();

    if (getChildCount() == 0) {
      if (hasCustomMarkerView) {
        hasCustomMarkerView = false;
        clearDrawableCache();
        updateTracksViewChanges();
        update(true);
      }

    }
  }

  @Override
  public Object getFeature() {
    return marker;
  }

  @Override
  public void addToMap(GoogleMap map) {
    marker = map.addMarker(getMarkerOptions());
    updateTracksViewChanges();
  }

  @Override
  public void removeFromMap(GoogleMap map) {
    if (marker == null) {
      return;
    }
    marker.remove();
    marker = null;
    updateTracksViewChanges();
  }

  private BitmapDescriptor getIcon() {
    if (hasCustomMarkerView) {
      // creating a bitmap from an arbitrary view
      if (iconBitmapDescriptor != null) {
        Bitmap viewBitmap = createDrawable();
        int width = Math.max(iconBitmap.getWidth(), viewBitmap.getWidth());
        int height = Math.max(iconBitmap.getHeight(), viewBitmap.getHeight());
        Bitmap combinedBitmap = Bitmap.createBitmap(width, height, iconBitmap.getConfig());
        Canvas canvas = new Canvas(combinedBitmap);
        canvas.drawBitmap(iconBitmap, 0, 0, null);
        canvas.drawBitmap(viewBitmap, 0, 0, null);
        return BitmapDescriptorFactory.fromBitmap(combinedBitmap);
      } else {
        return BitmapDescriptorFactory.fromBitmap(createDrawable());
      }
    } else if (iconBitmapDescriptor != null) {
      // use local image as a marker
      return iconBitmapDescriptor;
    } else {
      // render the default marker pin
      return BitmapDescriptorFactory.defaultMarker(this.markerHue);
    }
  }

  private MarkerOptions fillMarkerOptions(MarkerOptions options) {
    options.position(position);
    if (anchorIsSet) options.anchor(anchorX, anchorY);
    if (calloutAnchorIsSet) options.infoWindowAnchor(calloutAnchorX, calloutAnchorY);
    options.title(title);
    options.snippet(snippet);
    options.rotation(rotation);
    options.flat(flat);
    options.draggable(draggable);
    options.zIndex(zIndex);
    options.alpha(opacity);
    options.icon(getIcon());
    return options;
  }

  public void update(boolean updateIcon) {
    if (marker == null) {
      return;
    }

    if (updateIcon)
      updateMarkerIcon();

    if (anchorIsSet) {
      marker.setAnchor(anchorX, anchorY);
    } else {
      marker.setAnchor(0.5f, 1.0f);
    }

    if (calloutAnchorIsSet) {
      marker.setInfoWindowAnchor(calloutAnchorX, calloutAnchorY);
    } else {
      marker.setInfoWindowAnchor(0.5f, 0);
    }
  }

  public void update(int width, int height) {
    this.width = width;
    this.height = height;

    update(true);
  }

  private Bitmap mLastBitmapCreated = null;

  private void clearDrawableCache() {
    mLastBitmapCreated = null;
  }

  private Bitmap createDrawable() {
    int width = this.width <= 0 ? 100 : this.width;
    int height = this.height <= 0 ? 100 : this.height;
    this.buildDrawingCache();

    // Do not create the doublebuffer-bitmap each time. reuse it to save memory.
    Bitmap bitmap = mLastBitmapCreated;

    if (bitmap == null ||
            bitmap.isRecycled() ||
            bitmap.getWidth() != width ||
            bitmap.getHeight() != height) {
      bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
      mLastBitmapCreated = bitmap;
    } else {
      bitmap.eraseColor(Color.TRANSPARENT);
    }

    Canvas canvas = new Canvas(bitmap);
    this.draw(canvas);

    return bitmap;
  }

  public void setCalloutView(AirMapCallout view) {
    this.calloutView = view;
  }

  public AirMapCallout getCalloutView() {
    return this.calloutView;
  }

  public View getCallout() {
    if (this.calloutView == null) return null;

    if (this.wrappedCalloutView == null) {
      this.wrapCalloutView();
    }

    if (this.calloutView.getTooltip()) {
      return this.wrappedCalloutView;
    } else {
      return null;
    }
  }

  public View getInfoContents() {
    if (this.calloutView == null) return null;

    if (this.wrappedCalloutView == null) {
      this.wrapCalloutView();
    }

    if (this.calloutView.getTooltip()) {
      return null;
    } else {
      return this.wrappedCalloutView;
    }
  }

  private void wrapCalloutView() {
    // some hackery is needed to get the arbitrary infowindow view to render centered, and
    // with only the width/height that it needs.
    if (this.calloutView == null || this.calloutView.getChildCount() == 0) {
      return;
    }

    LinearLayout LL = new LinearLayout(context);
    LL.setOrientation(LinearLayout.VERTICAL);
    LL.setLayoutParams(new LinearLayout.LayoutParams(
        this.calloutView.width,
        this.calloutView.height,
        0f
    ));


    LinearLayout LL2 = new LinearLayout(context);
    LL2.setOrientation(LinearLayout.HORIZONTAL);
    LL2.setLayoutParams(new LinearLayout.LayoutParams(
        this.calloutView.width,
        this.calloutView.height,
        0f
    ));

    LL.addView(LL2);
    LL2.addView(this.calloutView);

    this.wrappedCalloutView = LL;
  }

  private int getDrawableResourceByName(String name) {
    return getResources().getIdentifier(
        name,
        "drawable",
        getContext().getPackageName());
  }

  private BitmapDescriptor getBitmapDescriptorByName(String name) {
    return BitmapDescriptorFactory.fromResource(getDrawableResourceByName(name));
  }

}
