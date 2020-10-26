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

  public String getCFSvg(Integer level,  String topOutline, String bottomOutline, String topInner, String bottomInner, PromotedMarkerArgStructure promotedMarkerArg) {
    String topInnerEncoded = topInner.replace("#", "%23");
    String topOutlineEncoded = topOutline.replace("#", "%23");
    String bottomOutlineEncoded = bottomOutline.replace("#", "%23");
    String bottomInnerEncoded = bottomInner.replace("#", "%23");
    String promotedEncoded = promotedMarkerArg.getColor().replace("#", "%23");
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
      String svgSrc = "%3Csvg xmlns='http://www.w3.org/2000/svg' width='%%width%%' height='%%height%%' viewBox='0 0 210 300'%3E%3Cpath fill='%2$s' d='M186.873,99.819C177.278,64.59,144.263,38.412,105,38.412S32.722,64.59,23.127,99.819 c-1.876,6.884-2.824,13.991-2.824,21.126c0,12.437,2.102,26.143,5.929,40.266c5.992,21.511,14.767,42.153,26.103,61.396 c14.546,24.859,32.87,47.06,52.666,61.402c19.766-14.343,38.132-36.521,52.67-61.402c11.332-19.242,20.103-39.885,26.095-61.396 c3.819-14.123,5.932-27.829,5.932-40.266C189.696,113.811,188.753,106.703,186.873,99.819z' /%3E%3Cpath fill='%1$s' d='M198.715,99.819C189.427,57.826,151.048,26.081,105,26.081c-46.045,0-84.424,31.746-93.719,73.739 c-1.431,6.46-2.153,13.059-2.153,19.677c0,13.08,2.063,27.218,5.827,41.715c5.284,20.38,13.932,41.479,24.953,61.396 C57.102,253.652,80.081,281.904,105,300c24.941-18.096,47.898-46.303,65.088-77.394c11.025-19.916,19.669-41.016,24.956-61.396 c3.761-14.497,5.828-28.635,5.828-41.715C200.872,112.878,200.152,106.279,198.715,99.819z M157.625,222.606 c-14.531,24.859-32.866,47.06-52.663,61.402c-19.766-14.343-38.12-36.521-52.665-61.402 c-11.321-19.242-20.085-39.885-26.065-61.396c-3.827-14.123-5.929-27.829-5.929-40.266c0-7.135,0.948-14.242,2.824-21.126 C32.722,64.59,65.737,38.412,105,38.412s72.278,26.178,81.873,61.407c1.88,6.884,2.823,13.991,2.823,21.126 c0,12.437-2.112,26.143-5.932,40.266c-5.992,21.511-14.763,42.153-26.095,61.396H157.625z' /%3E";

      if (promotedMarkerArg.getIsPromoted())
      {
        svgSrc += "%3Cpath fill='%%promotedColor%%' d='M153.79,0c26.118,0,47.299,21.178,47.299,47.303c0,26.127-21.181,47.302-47.299,47.302 c-26.126,0-47.308-21.175-47.308-47.302C106.482,21.178,127.664,0,153.79,0z'/%3E%3Cpath fill='%23FFFFFF' d='M146.809,69.018c0-4.655,3.102-7.756,7.752-7.756c4.652,0,7.753,3.101,7.753,7.756 c0,4.651-3.101,7.752-7.753,7.752C149.91,76.77,146.809,73.669,146.809,69.018z M149.139,54.284l-2.33-36.447h13.184l-1.551,36.447 H149.139z'/%3E";
      }

      svgSrc += "%3C/svg%3E";

      return "data:image/svg+xml," + svgSrc.replace("%2$s", topInnerEncoded).replace("%1$s", topOutlineEncoded)
              .replace("%%width%%", Double.toString(svgWidth)).replace("%%height%%", Double.toString(svgHeight))
              .replace("%%promotedColor%%", promotedEncoded);
    }

    if (level > 0 && level <= 37) {
      // svg with level 25
      String svgSrc = "%3Csvg xmlns='http://www.w3.org/2000/svg' width='%%width%%' height='%%height%%' viewBox='0 0 210 300'%3E%3Cpath fill='%1$s' d='M186.873,99.819C177.278,64.59,144.263,38.412,105,38.412S32.722,64.59,23.127,99.819 c-1.876,6.884-2.824,13.991-2.824,21.126c0,12.437,2.102,26.143,5.929,40.266c5.992,21.511,14.767,42.153,26.103,61.396H157.67 c11.332-19.242,20.103-39.885,26.095-61.396c3.819-14.123,5.932-27.829,5.932-40.266 C189.696,113.811,188.753,106.703,186.873,99.819z'/%3E%3Cpath fill='%2$s' d='M105,284.009c19.766-14.343,38.132-36.521,52.67-61.402H52.334C66.88,247.466,85.233,269.666,105,284.009z'/%3E%3Cpath fill='%3$s' d='M105,284.009c-19.767-14.343-38.12-36.521-52.666-61.402H39.908C57.102,253.652,80.081,281.904,105,300 c24.941-18.096,47.898-46.303,65.088-77.394H157.67C143.132,247.466,124.766,269.666,105,284.009z'/%3E%3Cpath fill='%4$s' d='M198.715,99.819C189.427,57.826,151.048,26.081,105,26.081c-46.045,0-84.424,31.746-93.719,73.739 c-1.431,6.46-2.153,13.059-2.153,19.677c0,13.08,2.063,27.218,5.827,41.715c5.284,20.38,13.932,41.479,24.953,61.396h12.426 c-11.336-19.242-20.11-39.885-26.103-61.396c-3.827-14.123-5.929-27.829-5.929-40.266c0-7.135,0.948-14.242,2.824-21.126 C32.722,64.59,65.737,38.412,105,38.412s72.278,26.178,81.873,61.407c1.88,6.884,2.823,13.991,2.823,21.126 c0,12.437-2.112,26.143-5.932,40.266c-5.992,21.511-14.763,42.153-26.095,61.396h12.418c11.025-19.916,19.669-41.016,24.956-61.396 c3.761-14.497,5.828-28.635,5.828-41.715C200.872,112.878,200.152,106.279,198.715,99.819z'/%3E";

      if (promotedMarkerArg.getIsPromoted())
      {
        svgSrc += "%3Cpath fill='%%promotedColor%%' d='M153.79,0c26.118,0,47.299,21.178,47.299,47.303c0,26.127-21.181,47.302-47.299,47.302 c-26.126,0-47.308-21.175-47.308-47.302C106.482,21.178,127.664,0,153.79,0z'/%3E%3Cpath fill='%23FFFFFF' d='M146.809,69.018c0-4.655,3.102-7.756,7.752-7.756c4.652,0,7.753,3.101,7.753,7.756 c0,4.651-3.101,7.752-7.753,7.752C149.91,76.77,146.809,73.669,146.809,69.018z M149.139,54.284l-2.33-36.447h13.184l-1.551,36.447 H149.139z'/%3E";
      }

      svgSrc += "%3C/svg%3E";

      return "data:image/svg+xml," + svgSrc.replace("%1$s", topInnerEncoded).replace("%2$s", bottomInnerEncoded)
              .replace("%3$s", bottomOutlineEncoded).replace("%4$s", topOutlineEncoded)
              .replace("%%width%%", Double.toString(svgWidth)).replace("%%height%%", Double.toString(svgHeight))
              .replace("%%promotedColor%%", promotedEncoded);
    }

    if (level > 37 && level <= 62) {
      // svg with level 50
      String svgSrc = "%3Csvg xmlns='http://www.w3.org/2000/svg' width='%%width%%' height='%%height%%' viewBox='0 0 210 300'%3E%3Cpath fill='%1$s' d='M186.873,100.037c-9.52-35.244-42.438-61.486-81.734-61.576c-39.3-0.094-72.335,25.969-82.012,61.175 c-1.892,6.884-2.854,13.987-2.869,21.13c0,12.433,2.045,26.139,5.835,40.273l157.552,0.348c3.843-14.119,5.978-27.821,6.007-40.258 C189.667,114.006,188.738,106.912,186.873,100.037z'/%3E%3Cpath fill='%2$s' d='M26.231,161.211c5.992,21.511,14.767,42.153,26.103,61.396c14.546,24.859,32.87,47.06,52.666,61.402 c19.766-14.343,38.132-36.521,52.67-61.402c11.332-19.242,20.103-39.885,26.095-61.396H26.231z'/%3E%3Cpath fill='%3$s' d='M198.715,99.819C189.427,57.826,151.048,26.081,105,26.081c-46.045,0-84.424,31.746-93.719,73.739 c-1.431,6.46-2.153,13.059-2.153,19.677c0,13.08,2.063,27.218,5.827,41.715h11.276c-3.827-14.123-5.929-27.829-5.929-40.266 c0-7.135,0.948-14.242,2.824-21.126C32.722,64.59,65.737,38.412,105,38.412s72.278,26.178,81.873,61.407 c1.88,6.884,2.823,13.991,2.823,21.126c0,12.437-2.112,26.143-5.932,40.266h11.279c3.761-14.497,5.828-28.635,5.828-41.715 C200.872,112.878,200.152,106.279,198.715,99.819z'/%3E%3Cpath fill='%4$s' d='M183.765,161.211c-5.992,21.511-14.763,42.153-26.095,61.396c-14.538,24.859-32.874,47.06-52.67,61.402 c-19.767-14.343-38.12-36.521-52.666-61.402c-11.336-19.242-20.11-39.885-26.103-61.396H14.955 c5.284,20.38,13.932,41.479,24.953,61.396C57.102,253.652,80.081,281.904,105,300c24.941-18.096,47.898-46.303,65.088-77.394 c11.025-19.916,19.669-41.016,24.956-61.396H183.765z'/%3E";

      if (promotedMarkerArg.getIsPromoted())
      {
        svgSrc += "%3Cpath fill='%%promotedColor%%' d='M153.79,0c26.118,0,47.299,21.178,47.299,47.303c0,26.127-21.181,47.302-47.299,47.302 c-26.126,0-47.308-21.175-47.308-47.302C106.482,21.178,127.664,0,153.79,0z'/%3E%3Cpath fill='%23FFFFFF' d='M146.809,69.018c0-4.655,3.102-7.756,7.752-7.756c4.652,0,7.753,3.101,7.753,7.756 c0,4.651-3.101,7.752-7.753,7.752C149.91,76.77,146.809,73.669,146.809,69.018z M149.139,54.284l-2.33-36.447h13.184l-1.551,36.447 H149.139z'/%3E";
      }

      svgSrc += "%3C/svg%3E";

      return "data:image/svg+xml," + svgSrc.replace("%1$s", topInnerEncoded).replace("%2$s", bottomInnerEncoded)
              .replace("%3$s", topOutlineEncoded).replace("%4$s", bottomOutlineEncoded)
              .replace("%%width%%", Double.toString(svgWidth)).replace("%%height%%", Double.toString(svgHeight))
              .replace("%%promotedColor%%", promotedEncoded);
    }

    if (level > 62 && level <= 88) {
      // svg with level 75
      String svgSrc = "%3Csvg xmlns='http://www.w3.org/2000/svg' width='%%width%%' height='%%height%%' viewBox='0 0 210 300'%3E%3Cpath fill='%1$s' d='M104.998,38.416c-39.262,0-72.276,26.179-81.87,61.405h163.745 C177.273,64.595,144.258,38.416,104.998,38.416z'/%3E%3Cpath fill='%2$s' d='M186.873,99.821H23.128c-1.877,6.885-2.824,13.99-2.824,21.125c0,12.438,2.103,26.145,5.93,40.267 c5.991,21.512,14.765,42.15,26.101,61.392c14.545,24.86,32.868,47.062,52.664,61.403c19.765-14.341,38.133-36.519,52.67-61.403 c11.336-19.241,20.105-39.88,26.096-61.392c3.816-14.122,5.93-27.828,5.93-40.267C189.693,113.812,188.75,106.706,186.873,99.821z' /%3E%3Cpath fill='%3$s' d='M104.998,38.416c39.26,0,72.275,26.179,81.875,61.405h11.873c-9.32-41.991-47.699-73.738-93.748-73.738 c-46.042,0-84.421,31.747-93.715,73.738h11.871C32.722,64.595,65.736,38.416,104.998,38.416z'/%3E%3Cpath fill='%4$s' d='M198.715,99.821h-11.867c1.871,6.885,2.816,13.99,2.816,21.125c0,12.438-2.109,26.145-5.924,40.267 c-5.99,21.512-14.768,42.15-26.104,61.392c-14.535,24.86-32.874,47.062-52.664,61.403c-19.771-14.341-38.118-36.519-52.664-61.403 c-11.326-19.241-20.094-39.88-26.075-61.392c-3.827-14.122-5.93-27.828-5.93-40.267c0-7.135,0.947-14.24,2.824-21.125H11.256 c-1.424,6.461-2.133,13.059-2.127,19.677c0,13.079,2.063,27.219,5.827,41.715c5.284,20.381,13.933,41.48,24.955,61.392 C57.103,253.65,80.079,281.905,104.998,300c24.944-18.095,47.896-46.301,65.09-77.396c11.025-19.911,19.668-41.011,24.951-61.392 c3.76-14.496,5.832-28.636,5.832-41.715C200.871,112.88,200.152,106.282,198.715,99.821z'/%3E";

      if (promotedMarkerArg.getIsPromoted())
      {
        svgSrc += "%3Cpath fill='%%promotedColor%%' d='M153.79,0c26.118,0,47.299,21.178,47.299,47.303c0,26.127-21.181,47.302-47.299,47.302 c-26.126,0-47.308-21.175-47.308-47.302C106.482,21.178,127.664,0,153.79,0z'/%3E%3Cpath fill='%23FFFFFF' d='M146.809,69.018c0-4.655,3.102-7.756,7.752-7.756c4.652,0,7.753,3.101,7.753,7.756 c0,4.651-3.101,7.752-7.753,7.752C149.91,76.77,146.809,73.669,146.809,69.018z M149.139,54.284l-2.33-36.447h13.184l-1.551,36.447 H149.139z'/%3E";
      }

      svgSrc += "%3C/svg%3E";

      return "data:image/svg+xml," + svgSrc.replace("%1$s", topInnerEncoded).replace("%2$s", bottomInnerEncoded)
              .replace("%3$s", topOutlineEncoded).replace("%4$s", bottomOutlineEncoded)
              .replace("%%width%%", Double.toString(svgWidth)).replace("%%height%%", Double.toString(svgHeight))
              .replace("%%promotedColor%%", promotedEncoded);
    }
    
    // svg with level 100
    String svgSrc = "%3Csvg xmlns='http://www.w3.org/2000/svg' width='%%width%%' height='%%height%%' viewBox='0 0 210 300'%3E%3Cpath fill='%1$s' d='M186.875,99.815c-9.595-35.23-42.611-61.408-81.874-61.408s-72.28,26.178-81.875,61.408 c-1.877,6.885-2.824,13.992-2.824,21.126c0,12.438,2.102,26.144,5.93,40.267c5.991,21.512,14.766,42.155,26.103,61.396 c14.546,24.86,32.87,47.061,52.667,61.404c19.766-14.344,38.132-36.521,52.67-61.404c11.332-19.241,20.104-39.885,26.096-61.396 c3.82-14.123,5.932-27.829,5.932-40.267C189.698,113.808,188.755,106.7,186.875,99.815z' /%3E%3Cpath fill='%2$s' d='M198.717,99.815c-9.288-41.994-47.666-73.74-93.716-73.74c-46.047,0-84.425,31.746-93.721,73.74 c-1.43,6.461-2.153,13.06-2.153,19.677c0,13.081,2.064,27.219,5.827,41.716c5.284,20.381,13.931,41.479,24.954,61.396 c17.193,31.046,40.173,59.3,65.093,77.396c24.941-18.096,47.899-46.304,65.088-77.396c11.026-19.917,19.669-41.016,24.958-61.396 c3.76-14.497,5.826-28.635,5.826-41.716C200.873,112.875,200.154,106.276,198.717,99.815z M183.767,161.208 c-5.992,21.512-14.764,42.155-26.096,61.396c-14.538,24.86-32.874,47.061-52.67,61.404c-19.767-14.344-38.121-36.521-52.667-61.404 c-11.337-19.241-20.111-39.885-26.103-61.396c-3.828-14.123-5.93-27.829-5.93-40.267c0-7.134,0.947-14.241,2.824-21.126 c9.595-35.23,42.612-61.408,81.875-61.408s72.279,26.178,81.874,61.408c1.88,6.885,2.823,13.992,2.823,21.126 C189.698,133.379,187.587,147.085,183.767,161.208z' /%3E";

    if (promotedMarkerArg.getIsPromoted())
    {
      svgSrc += "%3Cpath fill='%%promotedColor%%' d='M153.79,0c26.118,0,47.299,21.178,47.299,47.303c0,26.127-21.181,47.302-47.299,47.302 c-26.126,0-47.308-21.175-47.308-47.302C106.482,21.178,127.664,0,153.79,0z'/%3E%3Cpath fill='%23FFFFFF' d='M146.809,69.018c0-4.655,3.102-7.756,7.752-7.756c4.652,0,7.753,3.101,7.753,7.756 c0,4.651-3.101,7.752-7.753,7.752C149.91,76.77,146.809,73.669,146.809,69.018z M149.139,54.284l-2.33-36.447h13.184l-1.551,36.447 H149.139z'/%3E";
    }

    svgSrc += "%3C/svg%3E";

    return "data:image/svg+xml," + svgSrc.replace("%1$s", bottomInnerEncoded).replace("%2$s", bottomOutlineEncoded)
            .replace("%%width%%", Double.toString(svgWidth)).replace("%%height%%", Double.toString(svgHeight))
            .replace("%%promotedColor%%", promotedEncoded);
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

  static class PromotedMarkerArgStructure
  {
    public PromotedMarkerArgStructure(boolean isPromoted, String color)
    {
      this.isPromoted = isPromoted;
      this.color = color;
    }
    private boolean isPromoted;

    private String color;

    public boolean getIsPromoted()
    {
      return isPromoted;
    }

    public String getColor()
    {
      return color;
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
        JSONObject promoted = jsonObj.getJSONObject("promoted");
        Boolean isPromoted = new Boolean(promoted.getString("isPromoted"));
        String promotedMarkerColor = promoted.getString("color");
        PromotedMarkerArgStructure promotedMarkerArgs = new PromotedMarkerArgStructure(isPromoted, promotedMarkerColor) ;
        
        String svgDataUrI = getCFSvg(Math.round(Float.parseFloat(level)), topOutline, bottomOutline, topInner, bottomInner, promotedMarkerArgs);
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
