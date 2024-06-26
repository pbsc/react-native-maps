package com.rnmaps.maps;

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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import org.json.JSONException;
import org.json.JSONObject;
import com.google.maps.android.collections.MarkerManager;

public class MapMarker extends MapFeature {

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

  private MapCallout calloutView;
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

  private boolean hasCustomMarkerView = false;
  private final MapMarkerManager markerManager;
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
              if (MapMarker.this.markerManager != null && MapMarker.this.imageUri != null) {
                MapMarker.this.markerManager.getSharedIcon(MapMarker.this.imageUri)
                    .updateIcon(iconBitmapDescriptor, iconBitmap);
              }
              update(true);
            }
          };

  public MapMarker(Context context, MapMarkerManager markerManager) {
    super(context);
    this.context = context;
    this.markerManager = markerManager;
    logoHolder = DraweeHolder.create(createDraweeHierarchy(), context);
    logoHolder.onAttach();
  }

  public MapMarker(Context context, MarkerOptions options, MapMarkerManager markerManager) {
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

  public String getCFSvg(Boolean hasValetService, Integer level,  String topOutline, String bottomOutline, String topInner, String bottomInner, PromotedMarkerArgStructure promotedMarkerArg, String depotType, Float traffic, Integer zoomLevel, PlannedMarkerArgStructure plannedMarkerArgs, MaintenanceMarkerArgStructure maintenanceMarkerArgs) {
    String topInnerEncoded = topInner.replace("#", "%23");
    String topOutlineEncoded = topOutline.replace("#", "%23");
    String bottomOutlineEncoded = bottomOutline.replace("#", "%23");
    String bottomInnerEncoded = bottomInner.replace("#", "%23");
    String promotedEncoded = promotedMarkerArg.getColor().replace("#", "%23");
    String plannedOutlineEncoded = plannedMarkerArgs.getOutlineColor().replace("#", "%23");
    String plannedFillEncoded = plannedMarkerArgs.getFillColor().replace("#", "%23");
    String maintenanceOutlineEncoded = maintenanceMarkerArgs.getOutlineColor().replace("#", "%23");
    String maintenanceFillEncoded = maintenanceMarkerArgs.getFillColor().replace("#", "%23");
    final float scale = getResources().getDisplayMetrics().density;
    DisplayMetrics displayMetrics = new DisplayMetrics();
    ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(displayMetrics);
    int height = displayMetrics.heightPixels;
    int width = displayMetrics.widthPixels;
    double scaleConstant = (Math.sqrt(Math.pow(height, 2) + Math.pow(width, 2))/Math.sqrt(Math.pow(1080, 2) + Math.pow(2076, 2)));
    double svgWidth = 23.33 * scale;
    double svgHeight = 100.67 * scale;

    String svgSrc;
    if (hasValetService == true) {
      // svg with level 0
      if (depotType != null && "VIRTUAL".equalsIgnoreCase(depotType)) {
        svgSrc = "<svg  width='%%width%%' height='%%height%%' version=\"1.1\" viewBox=\"0 125 210 300\" xml:space=\"preserve\" xmlns=\"http://www.w3.org/2000/svg\"><defs><clipPath id=\"clipPath3003\"><path d=\"m0 68h25v-68h-25z\"/></clipPath></defs><path transform=\"matrix(8.3806 0 0 -8.3806 186.67 100.15)\" d=\"m0 0c-1.142 4.194-5.072 7.311-9.746 7.311s-8.605-3.117-9.747-7.311c-0.223-0.819-0.336-1.665-0.336-2.515 0-1.48 0.25-3.112 0.706-4.793 0.713-2.561 1.758-5.019 3.107-7.309 1.732-2.96 3.913-5.603 6.27-7.31 2.353 1.707 4.539 4.348 6.27 7.31 1.349 2.29 2.394 4.748 3.107 7.309 0.455 1.681 0.706 3.313 0.706 4.793 0 0.85-0.112 1.696-0.337 2.515\" fill=\"%2$s\"/><g><path transform=\"matrix(6.2854 0 0 6.2854 .23874 -134.73)\" d=\"m16.682 25.604c-0.676-4e-3 -1.4218 0.04791-2.1191 0.14258l0.26953 1.9824c0.60533-0.08267 1.2239-0.125 1.8359-0.125 0.63067-0.016 1.2827 0.0441 1.916 0.13476l0.28125-1.9785c-0.72-0.10267-1.4543-0.15625-2.1836-0.15625zm-4.9219 0.79492c-1.368 0.45733-2.6655 1.1044-3.8535 1.9258l1.1367 1.6445c1.032-0.71333 2.1609-1.2765 3.3516-1.6738zm9.9043 0.0293-0.64648 1.8945c1.188 0.404 2.3118 0.97336 3.3398 1.6934l1.1465-1.6387c-1.1813-0.82667-2.4745-1.4839-3.8398-1.9492zm-15.963 3.7441c-1.0187 1.032-1.8812 2.1982-2.5625 3.4648l1.7617 0.94726c0.59067-1.0987 1.3412-2.1099 2.2266-3.0059zm21.994 0.06641-1.4316 1.3945c0.88 0.90267 1.623 1.9188 2.207 3.0215l1.7676-0.9375c-0.67467-1.2707-1.5296-2.4412-2.543-3.4785zm-25.672 6.0566c-0.12133 0.392-0.22583 0.79265-0.3125 1.1953-0.22133 1.0093-0.3377 2.0429-0.3457 3.0723l2 0.01172c0.00667-0.88933 0.10683-1.7822 0.29883-2.6582 0.076-0.34933 0.16748-0.69258 0.27148-1.0312zm29.313 0.0918-1.916 0.57617c0.09333 0.31467 0.178 0.63303 0.25 0.95703 0.19867 0.896 0.29883 1.8183 0.29883 2.7383h2c0-1.0653-0.11832-2.1326-0.34766-3.1699-0.08133-0.372-0.17582-0.74023-0.28516-1.1016zm-27.814 6.75-1.9863 0.24023c0.148 1.2213 0.39314 2.5051 0.73047 3.8145l0.074219 0.2793 1.9336-0.50976-0.070312-0.26758c-0.316-1.2253-0.54564-2.422-0.68164-3.5566zm26.279 0.08594c-0.136 1.1027-0.36059 2.2694-0.66992 3.4707l-0.09375 0.35352 1.9316 0.51953 0.09766-0.37695c0.32933-1.2813 0.57341-2.5327 0.71875-3.7207zm-24.758 6.2383-1.8887 0.66016c0.444 1.2747 0.96669 2.5627 1.5547 3.832l1.8145-0.8418c-0.56-1.2093-1.0578-2.4371-1.4805-3.6504zm23.217 0.08594c-0.424 1.2067-0.9249 2.4345-1.4902 3.6465l1.8125 0.84766c0.59333-1.2733 1.1197-2.5627 1.5664-3.832zm-20.557 5.918-1.7598 0.95117 0.13672 0.25195c0.62 1.128 1.2915 2.2344 1.9941 3.2891l1.6641-1.1094c-0.672-1.0067-1.3116-2.0625-1.9062-3.1465zm17.879 0.08593-0.08203 0.15234c-0.608 1.1067-1.2677 2.1914-1.957 3.2207l1.6641 1.1133c0.72-1.0787 1.4077-2.212 2.041-3.3613l0.0957-0.17969zm-14.316 5.4375-1.5898 1.2129c0.86533 1.1333 1.7707 2.1955 2.6934 3.1582l1.4453-1.3828c-0.87067-0.90933-1.7288-1.9149-2.5488-2.9883zm10.748 0.07422c-0.81867 1.0667-1.678 2.0678-2.5566 2.9785l1.4414 1.3887c0.93067-0.96533 1.8391-2.0257 2.7031-3.1523zm-6.2559 4.7656-1.3066 1.5137c0.53467 0.46267 1.0757 0.89421 1.625 1.2969l0.58984 0.43359 0.5918-0.43359c0.548-0.40133 1.0903-0.83421 1.625-1.2969l-1.3066-1.5137c-0.30133 0.26-0.60482 0.51-0.91016 0.75-0.30533-0.24-0.6082-0.49-0.9082-0.75z\" fill=\"%1$s\"/></g><g transform=\"matrix(1.3333 0 0 -1.3333 0 90.667)\"><g transform=\"matrix(6.2856 0 0 6.2856 .17906 -258.37)\"><g clip-path=\"url(#clipPath3003)\"/></g></g>";
      } else {
        svgSrc = "%3Csvg xmlns='http://www.w3.org/2000/svg' width='%%width%%' height='%%height%%' viewBox='0 125 210 300'%3E%3Cpath fill='%2$s' d='M186.873,99.819C177.278,64.59,144.263,38.412,105,38.412S32.722,64.59,23.127,99.819 c-1.876,6.884-2.824,13.991-2.824,21.126c0,12.437,2.102,26.143,5.929,40.266c5.992,21.511,14.767,42.153,26.103,61.396 c14.546,24.859,32.87,47.06,52.666,61.402c19.766-14.343,38.132-36.521,52.67-61.402c11.332-19.242,20.103-39.885,26.095-61.396 c3.819-14.123,5.932-27.829,5.932-40.266C189.696,113.811,188.753,106.703,186.873,99.819z' /%3E%3Cpath fill='%1$s' d='M198.715,99.819C189.427,57.826,151.048,26.081,105,26.081c-46.045,0-84.424,31.746-93.719,73.739 c-1.431,6.46-2.153,13.059-2.153,19.677c0,13.08,2.063,27.218,5.827,41.715c5.284,20.38,13.932,41.479,24.953,61.396 C57.102,253.652,80.081,281.904,105,300c24.941-18.096,47.898-46.303,65.088-77.394c11.025-19.916,19.669-41.016,24.956-61.396 c3.761-14.497,5.828-28.635,5.828-41.715C200.872,112.878,200.152,106.279,198.715,99.819z M157.625,222.606 c-14.531,24.859-32.866,47.06-52.663,61.402c-19.766-14.343-38.12-36.521-52.665-61.402 c-11.321-19.242-20.085-39.885-26.065-61.396c-3.827-14.123-5.929-27.829-5.929-40.266c0-7.135,0.948-14.242,2.824-21.126 C32.722,64.59,65.737,38.412,105,38.412s72.278,26.178,81.873,61.407c1.88,6.884,2.823,13.991,2.823,21.126 c0,12.437-2.112,26.143-5.932,40.266c-5.992,21.511-14.763,42.153-26.095,61.396H157.625z' /%3E";
      }

      svgSrc += "<path fill='black' d=\"M3430 12793c-133-5-331-24-419-39-110-19-266-71-373-124-264-133-498-364-629-622-146-289-173-487-173-1283 0-708 20-908 111-1142l36-93h6268l20 48c58 133 87 264 111 497 17 166 17 1214 0 1380-28 273-58 389-146 570-193 396-568 685-993 765-227 42-242 43-2023 45-943 1-1749 0-1790-2zM1867 8643c-3-5-11-61-17-126-18-174-8-604 18-772 56-362 178-739 334-1035 508-963 1420-1599 2503-1746 181-25 647-25 825-1 998 136 1847 678 2376 1517 263 416 424 886 480 1400 13 124 15 206 10 410-3 140-9 278-14 305l-7 50-3252 3c-1788 1-3254-1-3256-5zM2375 4140c-482-18-738-60-1009-165-510-197-897-552-1131-1037-137-285-195-533-225-958-5-81-10-551-10-1062V0h10242l-5 963c-5 951-9 1075-43 1337-30 233-98 448-217 685-232 466-646 826-1157 1008-207 75-396 109-745 138-162 14-5363 21-5700 9z\" transform=\"matrix(.011 0 0 -.011 50 210)\"/%3E";

      if (promotedMarkerArg.getIsPromoted())
      {
        svgSrc += getCFPromotionIconSVG();
      }

      if (traffic != null && traffic != 0 && zoomLevel != null){
        svgSrc += getPredictionIconSVG(traffic, zoomLevel);
      }

      svgSrc += "%3C/svg%3E";

      return "data:image/svg+xml," + svgSrc.replace("%2$s", topInnerEncoded).replace("%1$s", topOutlineEncoded).replace("%4$s", topOutlineEncoded)
              .replace("%%width%%", Double.toString(svgWidth)).replace("%%height%%", Double.toString(svgHeight))
              .replace("%%promotedColor%%", promotedEncoded);
    }

    if(plannedMarkerArgs.getIsPlannedStation()) {
      svgSrc = "<svg  width='%%width%%' height='%%height%%' version=\"1.1\" viewBox=\"0 125 210 300\" xml:space=\"preserve\" xmlns=\"http://www.w3.org/2000/svg\"><defs><clipPath id=\"clipPath3003\"><path d=\"m0 68h25v-68h-25z\"/></clipPath></defs><path transform=\"matrix(8.3806 0 0 -8.3806 186.67 100.15)\" d=\"m0 0c-1.142 4.194-5.072 7.311-9.746 7.311s-8.605-3.117-9.747-7.311c-0.223-0.819-0.336-1.665-0.336-2.515 0-1.48 0.25-3.112 0.706-4.793 0.713-2.561 1.758-5.019 3.107-7.309 1.732-2.96 3.913-5.603 6.27-7.31 2.353 1.707 4.539 4.348 6.27 7.31 1.349 2.29 2.394 4.748 3.107 7.309 0.455 1.681 0.706 3.313 0.706 4.793 0 0.85-0.112 1.696-0.337 2.515\" fill=\"%2$s\"/><g><path transform=\"matrix(6.2854 0 0 6.2854 .23874 -134.73)\" d=\"m16.682 25.604c-0.676-4e-3 -1.4218 0.04791-2.1191 0.14258l0.26953 1.9824c0.60533-0.08267 1.2239-0.125 1.8359-0.125 0.63067-0.016 1.2827 0.0441 1.916 0.13476l0.28125-1.9785c-0.72-0.10267-1.4543-0.15625-2.1836-0.15625zm-4.9219 0.79492c-1.368 0.45733-2.6655 1.1044-3.8535 1.9258l1.1367 1.6445c1.032-0.71333 2.1609-1.2765 3.3516-1.6738zm9.9043 0.0293-0.64648 1.8945c1.188 0.404 2.3118 0.97336 3.3398 1.6934l1.1465-1.6387c-1.1813-0.82667-2.4745-1.4839-3.8398-1.9492zm-15.963 3.7441c-1.0187 1.032-1.8812 2.1982-2.5625 3.4648l1.7617 0.94726c0.59067-1.0987 1.3412-2.1099 2.2266-3.0059zm21.994 0.06641-1.4316 1.3945c0.88 0.90267 1.623 1.9188 2.207 3.0215l1.7676-0.9375c-0.67467-1.2707-1.5296-2.4412-2.543-3.4785zm-25.672 6.0566c-0.12133 0.392-0.22583 0.79265-0.3125 1.1953-0.22133 1.0093-0.3377 2.0429-0.3457 3.0723l2 0.01172c0.00667-0.88933 0.10683-1.7822 0.29883-2.6582 0.076-0.34933 0.16748-0.69258 0.27148-1.0312zm29.313 0.0918-1.916 0.57617c0.09333 0.31467 0.178 0.63303 0.25 0.95703 0.19867 0.896 0.29883 1.8183 0.29883 2.7383h2c0-1.0653-0.11832-2.1326-0.34766-3.1699-0.08133-0.372-0.17582-0.74023-0.28516-1.1016zm-27.814 6.75-1.9863 0.24023c0.148 1.2213 0.39314 2.5051 0.73047 3.8145l0.074219 0.2793 1.9336-0.50976-0.070312-0.26758c-0.316-1.2253-0.54564-2.422-0.68164-3.5566zm26.279 0.08594c-0.136 1.1027-0.36059 2.2694-0.66992 3.4707l-0.09375 0.35352 1.9316 0.51953 0.09766-0.37695c0.32933-1.2813 0.57341-2.5327 0.71875-3.7207zm-24.758 6.2383-1.8887 0.66016c0.444 1.2747 0.96669 2.5627 1.5547 3.832l1.8145-0.8418c-0.56-1.2093-1.0578-2.4371-1.4805-3.6504zm23.217 0.08594c-0.424 1.2067-0.9249 2.4345-1.4902 3.6465l1.8125 0.84766c0.59333-1.2733 1.1197-2.5627 1.5664-3.832zm-20.557 5.918-1.7598 0.95117 0.13672 0.25195c0.62 1.128 1.2915 2.2344 1.9941 3.2891l1.6641-1.1094c-0.672-1.0067-1.3116-2.0625-1.9062-3.1465zm17.879 0.08593-0.08203 0.15234c-0.608 1.1067-1.2677 2.1914-1.957 3.2207l1.6641 1.1133c0.72-1.0787 1.4077-2.212 2.041-3.3613l0.0957-0.17969zm-14.316 5.4375-1.5898 1.2129c0.86533 1.1333 1.7707 2.1955 2.6934 3.1582l1.4453-1.3828c-0.87067-0.90933-1.7288-1.9149-2.5488-2.9883zm10.748 0.07422c-0.81867 1.0667-1.678 2.0678-2.5566 2.9785l1.4414 1.3887c0.93067-0.96533 1.8391-2.0257 2.7031-3.1523zm-6.2559 4.7656-1.3066 1.5137c0.53467 0.46267 1.0757 0.89421 1.625 1.2969l0.58984 0.43359 0.5918-0.43359c0.548-0.40133 1.0903-0.83421 1.625-1.2969l-1.3066-1.5137c-0.30133 0.26-0.60482 0.51-0.91016 0.75-0.30533-0.24-0.6082-0.49-0.9082-0.75z\" fill=\"%1$s\"/></g><g transform=\"matrix(1.3333 0 0 -1.3333 0 90.667)\"><g transform=\"matrix(6.2856 0 0 6.2856 .17906 -258.37)\"><g clip-path=\"url(#clipPath3003)\"/></g></g>";

      svgSrc += "%3C/svg%3E";

      return "data:image/svg+xml," + svgSrc.replace("%2$s", plannedFillEncoded).replace("%1$s", plannedOutlineEncoded).replace("%4$s", plannedOutlineEncoded)
        .replace("%%width%%", Double.toString(svgWidth)).replace("%%height%%", Double.toString(svgHeight));

    }

    if(maintenanceMarkerArgs.getMaintenanceStation()) {
      svgSrc = "%3Csvg xmlns='http://www.w3.org/2000/svg' width='%%width%%' height='%%height%%' viewBox='0 125 210 300'%3E%3Cpath fill='%2$s' d='M186.873,99.819C177.278,64.59,144.263,38.412,105,38.412S32.722,64.59,23.127,99.819 c-1.876,6.884-2.824,13.991-2.824,21.126c0,12.437,2.102,26.143,5.929,40.266c5.992,21.511,14.767,42.153,26.103,61.396 c14.546,24.859,32.87,47.06,52.666,61.402c19.766-14.343,38.132-36.521,52.67-61.402c11.332-19.242,20.103-39.885,26.095-61.396 c3.819-14.123,5.932-27.829,5.932-40.266C189.696,113.811,188.753,106.703,186.873,99.819z' /%3E%3Cpath fill='%1$s' d='M198.715,99.819C189.427,57.826,151.048,26.081,105,26.081c-46.045,0-84.424,31.746-93.719,73.739 c-1.431,6.46-2.153,13.059-2.153,19.677c0,13.08,2.063,27.218,5.827,41.715c5.284,20.38,13.932,41.479,24.953,61.396 C57.102,253.652,80.081,281.904,105,300c24.941-18.096,47.898-46.303,65.088-77.394c11.025-19.916,19.669-41.016,24.956-61.396 c3.761-14.497,5.828-28.635,5.828-41.715C200.872,112.878,200.152,106.279,198.715,99.819z M157.625,222.606 c-14.531,24.859-32.866,47.06-52.663,61.402c-19.766-14.343-38.12-36.521-52.665-61.402 c-11.321-19.242-20.085-39.885-26.065-61.396c-3.827-14.123-5.929-27.829-5.929-40.266c0-7.135,0.948-14.242,2.824-21.126 C32.722,64.59,65.737,38.412,105,38.412s72.278,26.178,81.873,61.407c1.88,6.884,2.823,13.991,2.823,21.126 c0,12.437-2.112,26.143-5.932,40.266c-5.992,21.511-14.763,42.153-26.095,61.396H157.625z' /%3E";

      svgSrc += "%3C/svg%3E";

      return "data:image/svg+xml," + svgSrc.replace("%2$s", maintenanceFillEncoded).replace("%1$s", maintenanceOutlineEncoded).replace("%4$s", maintenanceOutlineEncoded)
        .replace("%%width%%", Double.toString(svgWidth)).replace("%%height%%", Double.toString(svgHeight));

    }

    if (level == 0) {
      // svg with level 0
      if (depotType != null && "VIRTUAL".equalsIgnoreCase(depotType)) {
        svgSrc = "<svg  width='%%width%%' height='%%height%%' version=\"1.1\" viewBox=\"0 125 210 300\" xml:space=\"preserve\" xmlns=\"http://www.w3.org/2000/svg\"><defs><clipPath id=\"clipPath3003\"><path d=\"m0 68h25v-68h-25z\"/></clipPath></defs><path transform=\"matrix(8.3806 0 0 -8.3806 186.67 100.15)\" d=\"m0 0c-1.142 4.194-5.072 7.311-9.746 7.311s-8.605-3.117-9.747-7.311c-0.223-0.819-0.336-1.665-0.336-2.515 0-1.48 0.25-3.112 0.706-4.793 0.713-2.561 1.758-5.019 3.107-7.309 1.732-2.96 3.913-5.603 6.27-7.31 2.353 1.707 4.539 4.348 6.27 7.31 1.349 2.29 2.394 4.748 3.107 7.309 0.455 1.681 0.706 3.313 0.706 4.793 0 0.85-0.112 1.696-0.337 2.515\" fill=\"%2$s\"/><g><path transform=\"matrix(6.2854 0 0 6.2854 .23874 -134.73)\" d=\"m16.682 25.604c-0.676-4e-3 -1.4218 0.04791-2.1191 0.14258l0.26953 1.9824c0.60533-0.08267 1.2239-0.125 1.8359-0.125 0.63067-0.016 1.2827 0.0441 1.916 0.13476l0.28125-1.9785c-0.72-0.10267-1.4543-0.15625-2.1836-0.15625zm-4.9219 0.79492c-1.368 0.45733-2.6655 1.1044-3.8535 1.9258l1.1367 1.6445c1.032-0.71333 2.1609-1.2765 3.3516-1.6738zm9.9043 0.0293-0.64648 1.8945c1.188 0.404 2.3118 0.97336 3.3398 1.6934l1.1465-1.6387c-1.1813-0.82667-2.4745-1.4839-3.8398-1.9492zm-15.963 3.7441c-1.0187 1.032-1.8812 2.1982-2.5625 3.4648l1.7617 0.94726c0.59067-1.0987 1.3412-2.1099 2.2266-3.0059zm21.994 0.06641-1.4316 1.3945c0.88 0.90267 1.623 1.9188 2.207 3.0215l1.7676-0.9375c-0.67467-1.2707-1.5296-2.4412-2.543-3.4785zm-25.672 6.0566c-0.12133 0.392-0.22583 0.79265-0.3125 1.1953-0.22133 1.0093-0.3377 2.0429-0.3457 3.0723l2 0.01172c0.00667-0.88933 0.10683-1.7822 0.29883-2.6582 0.076-0.34933 0.16748-0.69258 0.27148-1.0312zm29.313 0.0918-1.916 0.57617c0.09333 0.31467 0.178 0.63303 0.25 0.95703 0.19867 0.896 0.29883 1.8183 0.29883 2.7383h2c0-1.0653-0.11832-2.1326-0.34766-3.1699-0.08133-0.372-0.17582-0.74023-0.28516-1.1016zm-27.814 6.75-1.9863 0.24023c0.148 1.2213 0.39314 2.5051 0.73047 3.8145l0.074219 0.2793 1.9336-0.50976-0.070312-0.26758c-0.316-1.2253-0.54564-2.422-0.68164-3.5566zm26.279 0.08594c-0.136 1.1027-0.36059 2.2694-0.66992 3.4707l-0.09375 0.35352 1.9316 0.51953 0.09766-0.37695c0.32933-1.2813 0.57341-2.5327 0.71875-3.7207zm-24.758 6.2383-1.8887 0.66016c0.444 1.2747 0.96669 2.5627 1.5547 3.832l1.8145-0.8418c-0.56-1.2093-1.0578-2.4371-1.4805-3.6504zm23.217 0.08594c-0.424 1.2067-0.9249 2.4345-1.4902 3.6465l1.8125 0.84766c0.59333-1.2733 1.1197-2.5627 1.5664-3.832zm-20.557 5.918-1.7598 0.95117 0.13672 0.25195c0.62 1.128 1.2915 2.2344 1.9941 3.2891l1.6641-1.1094c-0.672-1.0067-1.3116-2.0625-1.9062-3.1465zm17.879 0.08593-0.08203 0.15234c-0.608 1.1067-1.2677 2.1914-1.957 3.2207l1.6641 1.1133c0.72-1.0787 1.4077-2.212 2.041-3.3613l0.0957-0.17969zm-14.316 5.4375-1.5898 1.2129c0.86533 1.1333 1.7707 2.1955 2.6934 3.1582l1.4453-1.3828c-0.87067-0.90933-1.7288-1.9149-2.5488-2.9883zm10.748 0.07422c-0.81867 1.0667-1.678 2.0678-2.5566 2.9785l1.4414 1.3887c0.93067-0.96533 1.8391-2.0257 2.7031-3.1523zm-6.2559 4.7656-1.3066 1.5137c0.53467 0.46267 1.0757 0.89421 1.625 1.2969l0.58984 0.43359 0.5918-0.43359c0.548-0.40133 1.0903-0.83421 1.625-1.2969l-1.3066-1.5137c-0.30133 0.26-0.60482 0.51-0.91016 0.75-0.30533-0.24-0.6082-0.49-0.9082-0.75z\" fill=\"%1$s\"/></g><g transform=\"matrix(1.3333 0 0 -1.3333 0 90.667)\"><g transform=\"matrix(6.2856 0 0 6.2856 .17906 -258.37)\"><g clip-path=\"url(#clipPath3003)\"/></g></g>";
      } else {
        svgSrc = "%3Csvg xmlns='http://www.w3.org/2000/svg' width='%%width%%' height='%%height%%' viewBox='0 125 210 300'%3E%3Cpath fill='%2$s' d='M186.873,99.819C177.278,64.59,144.263,38.412,105,38.412S32.722,64.59,23.127,99.819 c-1.876,6.884-2.824,13.991-2.824,21.126c0,12.437,2.102,26.143,5.929,40.266c5.992,21.511,14.767,42.153,26.103,61.396 c14.546,24.859,32.87,47.06,52.666,61.402c19.766-14.343,38.132-36.521,52.67-61.402c11.332-19.242,20.103-39.885,26.095-61.396 c3.819-14.123,5.932-27.829,5.932-40.266C189.696,113.811,188.753,106.703,186.873,99.819z' /%3E%3Cpath fill='%1$s' d='M198.715,99.819C189.427,57.826,151.048,26.081,105,26.081c-46.045,0-84.424,31.746-93.719,73.739 c-1.431,6.46-2.153,13.059-2.153,19.677c0,13.08,2.063,27.218,5.827,41.715c5.284,20.38,13.932,41.479,24.953,61.396 C57.102,253.652,80.081,281.904,105,300c24.941-18.096,47.898-46.303,65.088-77.394c11.025-19.916,19.669-41.016,24.956-61.396 c3.761-14.497,5.828-28.635,5.828-41.715C200.872,112.878,200.152,106.279,198.715,99.819z M157.625,222.606 c-14.531,24.859-32.866,47.06-52.663,61.402c-19.766-14.343-38.12-36.521-52.665-61.402 c-11.321-19.242-20.085-39.885-26.065-61.396c-3.827-14.123-5.929-27.829-5.929-40.266c0-7.135,0.948-14.242,2.824-21.126 C32.722,64.59,65.737,38.412,105,38.412s72.278,26.178,81.873,61.407c1.88,6.884,2.823,13.991,2.823,21.126 c0,12.437-2.112,26.143-5.932,40.266c-5.992,21.511-14.763,42.153-26.095,61.396H157.625z' /%3E";
      }

      if (promotedMarkerArg.getIsPromoted())
      {
        svgSrc += getCFPromotionIconSVG();
      }

      if (traffic != null && traffic != 0 && zoomLevel != null){
        svgSrc += getPredictionIconSVG(traffic, zoomLevel);
      }

      svgSrc += "%3C/svg%3E";

      return "data:image/svg+xml," + svgSrc.replace("%2$s", topInnerEncoded).replace("%1$s", topOutlineEncoded).replace("%4$s", topOutlineEncoded)
              .replace("%%width%%", Double.toString(svgWidth)).replace("%%height%%", Double.toString(svgHeight))
              .replace("%%promotedColor%%", promotedEncoded);
    }

    if (level > 0 && level <= 37) {
      // svg with level 25
      if (depotType != null && "VIRTUAL".equalsIgnoreCase(depotType)) {
        svgSrc = "<svg width='%%width%%' height='%%height%%' version=\"1.1\" viewBox=\"0 125 210 300\" xml:space=\"preserve\" xmlns=\"http://www.w3.org/2000/svg\"><defs><clipPath id=\"clipPath1236\"><path d=\"m0 68h25v-68h-25z\"/></clipPath></defs><g><path transform=\"matrix(8.3778 0 0 -8.3778 105 283.73)\" d=\"m0 0c2.245 1.756 4.4 4.328 6.131 7.29h-12.272c1.732-2.96 3.825-5.508 6.141-7.29\" fill=\"%2$s\"/><path transform=\"matrix(8.3778 0 0 -8.3778 189.78 121.09)\" d=\"m0 0c0-1.48-0.25-3.11-0.71-4.79-0.71-2.57-1.76-5.02-3.1-7.31 0-0.01-0.01-0.01-0.01-0.02h-12.52c0 0.01-0.01 0.01-0.01 0.02-1.35 2.29-2.4 4.74-3.11 7.31-0.46 1.68-0.71 3.31-0.71 4.79 0 0.85 0.12 1.69 0.34 2.51 1.14 4.2 5.07 7.31 9.75 7.31 4.67 0 8.6-3.11 9.74-7.31 0.23-0.82 0.34-1.66 0.34-2.51\" fill=\"%1$s\"/><path transform=\"matrix(6.2833 0 0 6.2833 .2783 -134.6)\" d=\"m6.1875 56.854c0.6 1.0667 1.2259 2.1074 1.8926 3.1074l1.6523-1.1074c-0.42667-0.65333-0.83828-1.32-1.2383-2zm18.639 0c-0.4 0.70667-0.8393 1.3997-1.2793 2.0664l1.666 1.1191c0.68-1.0267 1.3336-2.0922 1.9336-3.1855zm-13.559 4.1328-1.5879 1.2129c0.86667 1.1333 1.7734 2.2002 2.6934 3.1602l1.4395-1.3867c-0.86667-0.90667-1.7316-1.9063-2.5449-2.9863zm10.746 0.08008c-0.81333 1.0667-1.6805 2.066-2.5605 2.9727l1.4395 1.3867c0.93333-0.96 1.8404-2.0265 2.707-3.1465zm-6.2539 4.7598-1.3066 1.5195c0.53333 0.45333 1.067 0.89492 1.627 1.2949l0.58594 0.42578 0.58789-0.42578c0.54667-0.4 1.0936-0.84159 1.627-1.2949l-1.3066-1.5195c-0.29333 0.26667-0.60154 0.50805-0.9082 0.74805-0.30667-0.24-0.61292-0.48138-0.90625-0.74805z\" fill=\"%3$s\"/><path transform=\"matrix(8.4,0,0,8.4,0,-135.6)\" d=\"m12.51 19.268c-0.50865 0-1.0554 0.03956-1.584 0.10938l0.19727 1.4863c0.45878-0.06981 0.91819-0.09961 1.377-0.09961 0.46876-0.01 0.95682 0.03977 1.4355 0.09961l0.21094-1.4766c-0.53857-0.07979-1.0882-0.11914-1.6367-0.11914zm-3.6797 0.59961c-1.0273 0.3391-1.9951 0.82718-2.8828 1.4355l0.84766 1.2363c0.77794-0.53857 1.6161-0.95665 2.5137-1.2559zm7.4102 0.01953-0.48828 1.416c0.88765 0.29921 1.734 0.72705 2.502 1.2656l0.85938-1.2266c-0.88765-0.61836-1.8557-1.106-2.873-1.4551zm-11.937 2.8027c-0.76799 0.76799-1.4072 1.6462-1.9258 2.5938l1.3262 0.70703c0.43884-0.82783 0.998-1.5758 1.6562-2.2539zm16.445 0.04883-1.0664 1.0371c0.65826 0.6782 1.2057 1.4359 1.6445 2.2637l1.3262-0.69726c-0.50865-0.94749-1.1463-1.8256-1.9043-2.6035zm-19.197 4.5273c-0.089762 0.29921-0.17042 0.58924-0.24023 0.89844-0.15957 0.74802-0.24979 1.5269-0.25977 2.2949l1.4961 0.0098c0.00997-0.66823 0.078912-1.3361 0.22852-1.9844 0.049868-0.25932 0.12138-0.51801 0.20117-0.77734zm21.922 0.07031-1.4277 0.42969c0.06981 0.23936 0.12982 0.47938 0.17969 0.71875 0.1496 0.66823 0.22852 1.3567 0.22852 2.0449h1.4961c0-0.79791-0.08827-1.5972-0.25781-2.3652-0.05984-0.27926-0.13896-0.55885-0.21875-0.82812zm-20.807 5.0469-1.4844 0.17969c0.10971 0.91759 0.29948 1.8761 0.54883 2.8535l0.048828 0.20898 1.4473-0.37891-0.050781-0.19922c-0.22939-0.91759-0.41002-1.8163-0.50977-2.6641zm19.66 0.07031c-0.09973 0.81781-0.27061 1.6961-0.5 2.5938l-0.07031 0.26758 1.4453 0.38086 0.07031-0.2793c0.24934-0.95746 0.42936-1.8955 0.53906-2.7832zm-18.523 4.668-1.4043 0.48828c0.32913 0.95746 0.7174 1.9158 1.1562 2.8633l1.3555-0.62891c-0.41889-0.89762-0.78824-1.8151-1.1074-2.7227zm17.375 0.05859c-0.31916 0.90757-0.69833 1.8249-1.1172 2.7324l1.3555 0.62891c0.43884-0.94751 0.83884-1.9138 1.168-2.8613zm-15.379 4.4297-1.3164 0.70703 0.099609 0.18945c0.029921 0.04987 0.050159 0.09076 0.080078 0.14062h1.7246c-0.16955-0.27926-0.34043-0.56818-0.5-0.85742zm13.373 0.05859-0.06836 0.12109c-0.15958 0.28924-0.32048 0.57815-0.5 0.85742h1.7363c0.02992-0.03989 0.06014-0.09097 0.08008-0.13086l0.06836-0.13867z\" fill=\"%4$s\" stroke-width=\".74802\"/></g><g display=\"none\"><path transform=\"matrix(8.3778 0 0 -8.3778 153.67 .63797)\" d=\"m0 0c3.109 0 5.631-2.521 5.631-5.631 0-3.111-2.522-5.631-5.631-5.631-3.11 0-5.632 2.52-5.632 5.631 0 3.11 2.522 5.631 5.632 5.631\"/></g><g transform=\"matrix(1.3333 0 0 -1.3333 0 90.667)\"><g transform=\"matrix(6.2835 0 0 6.2835 .20873 -258.32)\"><g clip-path=\"url(#clipPath1236)\"/></g></g><g display=\"none\"></g>";
      } else {
        svgSrc = "%3Csvg xmlns='http://www.w3.org/2000/svg' width='%%width%%' height='%%height%%' viewBox='0 125 210 300'%3E%3Cpath fill='%1$s' d='M186.873,99.819C177.278,64.59,144.263,38.412,105,38.412S32.722,64.59,23.127,99.819 c-1.876,6.884-2.824,13.991-2.824,21.126c0,12.437,2.102,26.143,5.929,40.266c5.992,21.511,14.767,42.153,26.103,61.396H157.67 c11.332-19.242,20.103-39.885,26.095-61.396c3.819-14.123,5.932-27.829,5.932-40.266 C189.696,113.811,188.753,106.703,186.873,99.819z'/%3E%3Cpath fill='%2$s' d='M105,284.009c19.766-14.343,38.132-36.521,52.67-61.402H52.334C66.88,247.466,85.233,269.666,105,284.009z'/%3E%3Cpath fill='%3$s' d='M105,284.009c-19.767-14.343-38.12-36.521-52.666-61.402H39.908C57.102,253.652,80.081,281.904,105,300 c24.941-18.096,47.898-46.303,65.088-77.394H157.67C143.132,247.466,124.766,269.666,105,284.009z'/%3E%3Cpath fill='%4$s' d='M198.715,99.819C189.427,57.826,151.048,26.081,105,26.081c-46.045,0-84.424,31.746-93.719,73.739 c-1.431,6.46-2.153,13.059-2.153,19.677c0,13.08,2.063,27.218,5.827,41.715c5.284,20.38,13.932,41.479,24.953,61.396h12.426 c-11.336-19.242-20.11-39.885-26.103-61.396c-3.827-14.123-5.929-27.829-5.929-40.266c0-7.135,0.948-14.242,2.824-21.126 C32.722,64.59,65.737,38.412,105,38.412s72.278,26.178,81.873,61.407c1.88,6.884,2.823,13.991,2.823,21.126 c0,12.437-2.112,26.143-5.932,40.266c-5.992,21.511-14.763,42.153-26.095,61.396h12.418c11.025-19.916,19.669-41.016,24.956-61.396 c3.761-14.497,5.828-28.635,5.828-41.715C200.872,112.878,200.152,106.279,198.715,99.819z'/%3E";
      }

      if (promotedMarkerArg.getIsPromoted())
      {
        svgSrc += getCFPromotionIconSVG();
      }

      if (traffic != null && traffic != 0 && zoomLevel != null){
        svgSrc += getPredictionIconSVG(traffic, zoomLevel);
      }

      svgSrc += "%3C/svg%3E";

      return "data:image/svg+xml," + svgSrc.replace("%1$s", topInnerEncoded).replace("%2$s", bottomInnerEncoded)
              .replace("%3$s", bottomOutlineEncoded).replace("%4$s", topOutlineEncoded)
              .replace("%%width%%", Double.toString(svgWidth)).replace("%%height%%", Double.toString(svgHeight))
              .replace("%%promotedColor%%", promotedEncoded);
    }

    if (level > 37 && level <= 62) {
      // svg with level 50
      if (depotType != null && "VIRTUAL".equalsIgnoreCase(depotType)) {
        svgSrc = "<svg width='%%width%%' height='%%height%%' version=\"1.1\" viewBox=\"0 125 210 300\" xml:space=\"preserve\" xmlns=\"http://www.w3.org/2000/svg\"><defs><clipPath id=\"clipPath5148\"><path d=\"m0 68h25v-68h-25z\"/></clipPath></defs><g><path transform=\"matrix(8.3778 0 0 -8.3778 26.439 161.42)\" d=\"m0 0c0.713-2.562 1.758-5.02 3.108-7.31 1.731-2.96 3.913-5.602 6.269-7.309 2.354 1.707 4.54 4.347 6.27 7.309 1.35 2.29 2.395 4.748 3.107 7.31z\" fill=\"%2$s\"/><path transform=\"matrix(8.3778 0 0 -8.3778 186.65 100.41)\" d=\"m0 0c-1.133 4.196-5.051 7.32-9.73 7.33-4.678 0.012-8.611-3.091-9.763-7.282-0.225-0.819-0.34-1.665-0.342-2.515 0-1.481 0.244-3.113 0.695-4.795l18.756-0.041c0.458 1.68 0.712 3.312 0.716 4.792 2e-3 0.848-0.109 1.693-0.332 2.511\" fill=\"%1$s\"/><path transform=\"matrix(6.2833 0 0 6.2833 .2783 -134.6)\" d=\"m2.2539 47.107c0 0.02667 3.386e-4 0.05341 0.013672 0.08008l0.066406 0.2793 1.373-0.35938zm26.986 0 1.7324 0.45312 0.09375-0.37305c0.01333-0.02667 0.01367-0.05341 0.01367-0.08008zm-24.201 2.3594-1.8789 0.65234c0.44 1.28 0.96021 2.5615 1.5469 3.8281l1.8125-0.83984c-0.56-1.2-1.0538-2.4273-1.4805-3.6406zm23.229 0.08008c-0.42667 1.2133-0.93414 2.439-1.4941 3.6523l1.8125 0.83984c0.58667-1.2667 1.1205-2.5595 1.5605-3.8262zm-20.561 5.9199-1.7598 0.94726 0.13281 0.25195c0.62667 1.1333 1.2933 2.2416 2 3.2949l1.6523-1.1074c-0.66667-1.0133-1.3062-2.0665-1.9062-3.1465zm17.879 0.08008-0.0918 0.16016c-0.6 1.1067-1.2673 2.1862-1.9473 3.2129l1.666 1.1191c0.72-1.08 1.401-2.2127 2.041-3.3594l0.0918-0.18555zm-14.318 5.4395-1.5879 1.2129c0.86667 1.1333 1.7734 2.2002 2.6934 3.1602l1.4395-1.3867c-0.86667-0.90667-1.7316-1.9063-2.5449-2.9863zm10.746 0.08008c-0.81333 1.0667-1.6805 2.066-2.5605 2.9727l1.4395 1.3867c0.93333-0.96 1.8404-2.0265 2.707-3.1465zm-6.2539 4.7598-1.3066 1.5195c0.53333 0.45333 1.067 0.89492 1.627 1.2949l0.58594 0.42578 0.58789-0.42578c0.54667-0.4 1.0936-0.84159 1.627-1.2949l-1.3066-1.5195c-0.29333 0.26667-0.60154 0.50805-0.9082 0.74805-0.30667-0.24-0.61292-0.48138-0.90625-0.74805z\" fill=\"%4$s\"/><path transform=\"matrix(8.4,0,0,8.4,0,-135.6)\" d=\"m12.51 19.268c-0.50865 0-1.0554 0.03956-1.584 0.10938l0.19727 1.4863c0.45878-0.06981 0.91819-0.09961 1.377-0.09961 0.46876-0.01 0.95682 0.03977 1.4355 0.09961l0.21094-1.4766c-0.53857-0.07979-1.0882-0.11914-1.6367-0.11914zm-3.6797 0.59961c-1.0273 0.3391-1.9951 0.82718-2.8828 1.4355l0.84766 1.2363c0.77794-0.53857 1.6161-0.95665 2.5137-1.2559zm7.4102 0.01953-0.48828 1.416c0.88765 0.29921 1.734 0.72705 2.502 1.2656l0.85938-1.2266c-0.88765-0.61836-1.8557-1.106-2.873-1.4551zm-11.937 2.8027c-0.76799 0.76799-1.4072 1.6462-1.9258 2.5938l1.3262 0.70703c0.43884-0.82783 0.998-1.5758 1.6562-2.2539zm16.445 0.04883-1.0664 1.0371c0.65826 0.6782 1.2057 1.4359 1.6445 2.2637l1.3262-0.69726c-0.50865-0.94749-1.1463-1.8256-1.9043-2.6035zm-19.197 4.5273c-0.089762 0.29921-0.17042 0.58924-0.24023 0.89844-0.15957 0.74802-0.24979 1.5269-0.25977 2.2949l1.4961 0.0098c0.00997-0.66823 0.078912-1.3361 0.22852-1.9844 0.049868-0.25932 0.12138-0.51801 0.20117-0.77734zm21.922 0.07031-1.4277 0.42969c0.06981 0.23936 0.12982 0.47938 0.17969 0.71875 0.1496 0.66823 0.22852 1.3567 0.22852 2.0449h1.4961c0-0.79791-0.08827-1.5972-0.25781-2.3652-0.05984-0.27926-0.13896-0.55885-0.21875-0.82812zm-20.807 5.0469-1.4844 0.17969c0.10971 0.89762 0.28973 1.8355 0.53906 2.793h1.0859l0.41992-0.10938-0.050781-0.19922c-0.22939-0.91759-0.41002-1.8163-0.50977-2.6641zm19.66 0.07031c-0.09973 0.81781-0.27061 1.6961-0.5 2.5938l-0.07031 0.26758 0.15039 0.04102h1.375c0.24934-0.93749 0.41958-1.855 0.5293-2.7227z\" fill=\"%3$s\" stroke-width=\".74802\"/></g><g transform=\"matrix(1.3333 0 0 -1.3333 0 90.667)\"><g transform=\"matrix(6.2835 0 0 6.2835 .20873 -258.32)\"><g clip-path=\"url(#clipPath5148)\"/></g></g>";
      } else {
        svgSrc = "%3Csvg xmlns='http://www.w3.org/2000/svg' width='%%width%%' height='%%height%%' viewBox='0 125 210 300'%3E%3Cpath fill='%1$s' d='M186.873,100.037c-9.52-35.244-42.438-61.486-81.734-61.576c-39.3-0.094-72.335,25.969-82.012,61.175 c-1.892,6.884-2.854,13.987-2.869,21.13c0,12.433,2.045,26.139,5.835,40.273l157.552,0.348c3.843-14.119,5.978-27.821,6.007-40.258 C189.667,114.006,188.738,106.912,186.873,100.037z'/%3E%3Cpath fill='%2$s' d='M26.231,161.211c5.992,21.511,14.767,42.153,26.103,61.396c14.546,24.859,32.87,47.06,52.666,61.402 c19.766-14.343,38.132-36.521,52.67-61.402c11.332-19.242,20.103-39.885,26.095-61.396H26.231z'/%3E%3Cpath fill='%3$s' d='M198.715,99.819C189.427,57.826,151.048,26.081,105,26.081c-46.045,0-84.424,31.746-93.719,73.739 c-1.431,6.46-2.153,13.059-2.153,19.677c0,13.08,2.063,27.218,5.827,41.715h11.276c-3.827-14.123-5.929-27.829-5.929-40.266 c0-7.135,0.948-14.242,2.824-21.126C32.722,64.59,65.737,38.412,105,38.412s72.278,26.178,81.873,61.407 c1.88,6.884,2.823,13.991,2.823,21.126c0,12.437-2.112,26.143-5.932,40.266h11.279c3.761-14.497,5.828-28.635,5.828-41.715 C200.872,112.878,200.152,106.279,198.715,99.819z'/%3E%3Cpath fill='%4$s' d='M183.765,161.211c-5.992,21.511-14.763,42.153-26.095,61.396c-14.538,24.859-32.874,47.06-52.67,61.402 c-19.767-14.343-38.12-36.521-52.666-61.402c-11.336-19.242-20.11-39.885-26.103-61.396H14.955 c5.284,20.38,13.932,41.479,24.953,61.396C57.102,253.652,80.081,281.904,105,300c24.941-18.096,47.898-46.303,65.088-77.394 c11.025-19.916,19.669-41.016,24.956-61.396H183.765z'/%3E";
      }

      if (promotedMarkerArg.getIsPromoted())
      {
        svgSrc += getCFPromotionIconSVG();
      }

      if (traffic != null && traffic != 0 && zoomLevel != null){
        svgSrc += getPredictionIconSVG(traffic, zoomLevel);
      }

      svgSrc += "%3C/svg%3E";

      return "data:image/svg+xml," + svgSrc.replace("%1$s", topInnerEncoded).replace("%2$s", bottomInnerEncoded)
              .replace("%3$s", topOutlineEncoded).replace("%4$s", bottomOutlineEncoded)
              .replace("%%width%%", Double.toString(svgWidth)).replace("%%height%%", Double.toString(svgHeight))
              .replace("%%promotedColor%%", promotedEncoded);
    }

    if (level > 62 && level <= 88) {
      // svg with level 75
      if (depotType != null && "VIRTUAL".equalsIgnoreCase(depotType)) {
        svgSrc = "<svg width='%%width%%' height='%%height%%' version=\"1.1\" viewBox=\"0 125 210 300\" xml:space=\"preserve\" xmlns=\"http://www.w3.org/2000/svg\"><defs><clipPath id=\"clipPath2215\"><path d=\"m0 68h25v-68h-25z\"/></clipPath></defs><g><path transform=\"matrix(8.3778 0 0 -8.3778 185.96 100.19)\" d=\"m0 0h-19.328c-0.223-0.82-0.336-1.665-0.336-2.515 0-1.481 0.25-3.112 0.706-4.794 0.713-2.561 1.757-4.935 3.106-7.226 1.732-2.96 3.864-5.598 6.188-7.392 2.253 1.769 4.473 4.445 6.203 7.407 1.35 2.29 2.379 4.65 3.092 7.211 0.454 1.682 0.705 3.313 0.705 4.794 0 0.85-0.111 1.695-0.336 2.515\" fill=\"%2$s\"/><path transform=\"matrix(8.3778 0 0 -8.3778 186.1 100.22)\" d=\"m0 0h-19.317c1.13 4.2 5.025 7.31 9.664 7.31 4.628 0 8.523-3.11 9.653-7.31\" fill=\"%1$s\"/><path transform=\"matrix(6.2833 0 0 6.2833 .2783 -134.6)\" d=\"m1.7324 37.373c-0.013333 0.04-0.012057 0.08109-0.025391 0.12109-0.21333 1-0.33432 2.0397-0.34766 3.0664l2 0.01367c0.013333-0.89333 0.10664-1.7876 0.30664-2.6543 0.04-0.18667 0.081432-0.36021 0.13477-0.54688zm27.801 0c0.05333 0.18667 0.09281 0.36021 0.13281 0.54688 0.2 0.89333 0.30664 1.8144 0.30664 2.7344h2c0-1.0667-0.11904-2.1335-0.3457-3.1602-0.01333-0.04-0.01401-0.08109-0.02734-0.12109zm-26.014 5.7598-1.9863 0.24024c0.14667 1.2267 0.40104 2.5078 0.73438 3.8145l0.066406 0.2793 1.9336-0.50586-0.068359-0.26758c-0.30667-1.2267-0.54635-2.4272-0.67969-3.5605zm26.281 0.09375c-0.13333 1.0933-0.3613 2.2668-0.66797 3.4668l-0.09375 0.35938 1.9336 0.50781 0.09375-0.37305c0.33333-1.28 0.57404-2.534 0.7207-3.7207zm-24.762 6.2402-1.8789 0.65234c0.44 1.28 0.96021 2.5615 1.5469 3.8281l1.8125-0.83984c-0.56-1.2-1.0538-2.4273-1.4805-3.6406zm23.229 0.08008c-0.42667 1.2133-0.93414 2.439-1.4941 3.6523l1.8125 0.83984c0.58667-1.2667 1.1205-2.5595 1.5605-3.8262zm-20.561 5.9199-1.7598 0.94726 0.13281 0.25195c0.62667 1.1333 1.2933 2.2416 2 3.2949l1.6523-1.1074c-0.66667-1.0133-1.3062-2.0665-1.9062-3.1465zm17.879 0.08008-0.0918 0.16016c-0.6 1.1067-1.2673 2.1862-1.9473 3.2129l1.666 1.1191c0.72-1.08 1.401-2.2127 2.041-3.3594l0.0918-0.18555zm-14.318 5.4395-1.5879 1.2129c0.86667 1.1333 1.7734 2.2002 2.6934 3.1602l1.4395-1.3867c-0.86667-0.90667-1.7316-1.9063-2.5449-2.9863zm10.746 0.08008c-0.81333 1.0667-1.6805 2.066-2.5605 2.9727l1.4395 1.3867c0.93333-0.96 1.8404-2.0265 2.707-3.1465zm-6.2539 4.7598-1.3066 1.5195c0.53333 0.45333 1.067 0.89492 1.627 1.2949l0.58594 0.42578 0.58789-0.42578c0.54667-0.4 1.0936-0.84159 1.627-1.2949l-1.3066-1.5195c-0.29333 0.26667-0.60154 0.50805-0.9082 0.74805-0.30667-0.24-0.61292-0.48138-0.90625-0.74805z\" fill=\"%4$s\"/><path transform=\"matrix(8.4,0,0,8.4,0,-135.6)\" d=\"m12.51 19.268c-0.50865 0-1.0554 0.03956-1.584 0.10938l0.19727 1.4863c0.45878-0.06981 0.91819-0.09961 1.377-0.09961 0.46876-0.01 0.95682 0.03977 1.4355 0.09961l0.21094-1.4766c-0.53857-0.07979-1.0882-0.11914-1.6367-0.11914zm-3.6797 0.59961c-1.0273 0.3391-1.9951 0.82718-2.8828 1.4355l0.84766 1.2363c0.77794-0.53857 1.6161-0.95665 2.5137-1.2559zm7.4102 0.01953-0.48828 1.416c0.88765 0.29921 1.734 0.72705 2.502 1.2656l0.85938-1.2266c-0.88765-0.61836-1.8557-1.106-2.873-1.4551zm-11.937 2.8027c-0.76799 0.76799-1.4072 1.6462-1.9258 2.5938l1.3262 0.70703c0.43884-0.82783 0.998-1.5758 1.6562-2.2539zm16.445 0.04883-1.0664 1.0371c0.65826 0.6782 1.2057 1.4359 1.6445 2.2637l1.3262-0.69726c-0.50865-0.94749-1.1463-1.8256-1.9043-2.6035zm-19.197 4.5273c-0.079791 0.26929-0.15089 0.52931-0.2207 0.80859h1.5469c0.029921-0.11968 0.059717-0.2475 0.099609-0.36719zm21.922 0.07031-1.4277 0.42969c0.02992 0.09973 0.06013 0.20886 0.08008 0.30859h1.5449c-0.05984-0.24934-0.12745-0.49892-0.19727-0.73828z\" fill=\"%3$s\" stroke-width=\".99736\"/></g><g transform=\"matrix(1.3333 0 0 -1.3333 0 90.667)\"><g transform=\"matrix(6.2835 0 0 6.2835 .20873 -258.32)\"><g clip-path=\"url(#clipPath2215)\"/></g></g>";
      } else {
        svgSrc = "%3Csvg xmlns='http://www.w3.org/2000/svg' width='%%width%%' height='%%height%%' viewBox='0 125 210 300'%3E%3Cpath fill='%1$s' d='M104.998,38.416c-39.262,0-72.276,26.179-81.87,61.405h163.745 C177.273,64.595,144.258,38.416,104.998,38.416z'/%3E%3Cpath fill='%2$s' d='M186.873,99.821H23.128c-1.877,6.885-2.824,13.99-2.824,21.125c0,12.438,2.103,26.145,5.93,40.267 c5.991,21.512,14.765,42.15,26.101,61.392c14.545,24.86,32.868,47.062,52.664,61.403c19.765-14.341,38.133-36.519,52.67-61.403 c11.336-19.241,20.105-39.88,26.096-61.392c3.816-14.122,5.93-27.828,5.93-40.267C189.693,113.812,188.75,106.706,186.873,99.821z' /%3E%3Cpath fill='%3$s' d='M104.998,38.416c39.26,0,72.275,26.179,81.875,61.405h11.873c-9.32-41.991-47.699-73.738-93.748-73.738 c-46.042,0-84.421,31.747-93.715,73.738h11.871C32.722,64.595,65.736,38.416,104.998,38.416z'/%3E%3Cpath fill='%4$s' d='M198.715,99.821h-11.867c1.871,6.885,2.816,13.99,2.816,21.125c0,12.438-2.109,26.145-5.924,40.267 c-5.99,21.512-14.768,42.15-26.104,61.392c-14.535,24.86-32.874,47.062-52.664,61.403c-19.771-14.341-38.118-36.519-52.664-61.403 c-11.326-19.241-20.094-39.88-26.075-61.392c-3.827-14.122-5.93-27.828-5.93-40.267c0-7.135,0.947-14.24,2.824-21.125H11.256 c-1.424,6.461-2.133,13.059-2.127,19.677c0,13.079,2.063,27.219,5.827,41.715c5.284,20.381,13.933,41.48,24.955,61.392 C57.103,253.65,80.079,281.905,104.998,300c24.944-18.095,47.896-46.301,65.09-77.396c11.025-19.911,19.668-41.011,24.951-61.392 c3.76-14.496,5.832-28.636,5.832-41.715C200.871,112.88,200.152,106.282,198.715,99.821z'/%3E";
      }

      if (promotedMarkerArg.getIsPromoted())
      {
        svgSrc += getCFPromotionIconSVG();
      }

      if (traffic != null && traffic != 0 && zoomLevel != null){
        svgSrc += getPredictionIconSVG(traffic, zoomLevel);
      }

      svgSrc += "%3C/svg%3E";

      return "data:image/svg+xml," + svgSrc.replace("%1$s", topInnerEncoded).replace("%2$s", bottomInnerEncoded)
              .replace("%3$s", topOutlineEncoded).replace("%4$s", bottomOutlineEncoded)
              .replace("%%width%%", Double.toString(svgWidth)).replace("%%height%%", Double.toString(svgHeight))
              .replace("%%promotedColor%%", promotedEncoded);
    }

    // svg with level 100
    if (depotType != null && "VIRTUAL".equalsIgnoreCase(depotType)) {
      svgSrc = "<svg width='%%width%%' height='%%height%%' version=\"1.1\" viewBox=\"0 125 210 300\" xml:space=\"preserve\" xmlns=\"http://www.w3.org/2000/svg\"><defs><clipPath id=\"clipPath9773\"><path d=\"m0 68h25v-68h-25z\"/></clipPath></defs><g><path transform=\"matrix(8.3806 0 0 -8.3806 185.85 100.15)\" d=\"m0 0c-1.142 4.194-4.974 7.311-9.647 7.311-4.674 0-8.538-3.117-9.68-7.311-0.223-0.82-0.336-1.666-0.336-2.515 0-1.481 0.25-3.112 0.706-4.793 0.713-2.563 1.77-4.922 3.12-7.213 1.732-2.96 3.905-5.575 6.19-7.406 2.26 1.762 4.456 4.444 6.187 7.406 1.349 2.291 2.377 4.652 3.09 7.213 0.455 1.681 0.707 3.312 0.707 4.793-1e-3 0.849-0.113 1.695-0.337 2.515\" fill=\"%1$s\"/></g><g transform=\"matrix(1.3333 0 0 -1.3333 0 90.667)\"><g transform=\"matrix(6.2856 0 0 6.2856 .17906 -258.37)\"><g clip-path=\"url(#clipPath9773)\"/></g></g><g><path transform=\"matrix(6.2854 0 0 6.2854 .23874 -134.73)\" d=\"m16.682 25.604c-0.676-4e-3 -1.4218 0.04791-2.1191 0.14258l0.26953 1.9824c0.60533-0.08267 1.2239-0.125 1.8359-0.125 0.63067-0.016 1.2827 0.0441 1.916 0.13476l0.28125-1.9785c-0.72-0.10267-1.4543-0.15625-2.1836-0.15625zm-4.9219 0.79492c-1.368 0.45733-2.6655 1.1044-3.8535 1.9258l1.1367 1.6445c1.032-0.71333 2.1609-1.2765 3.3516-1.6738zm9.9043 0.0293-0.64648 1.8945c1.188 0.404 2.3118 0.97336 3.3398 1.6934l1.1465-1.6387c-1.1813-0.82667-2.4745-1.4839-3.8398-1.9492zm-15.963 3.7441c-1.0187 1.032-1.8812 2.1982-2.5625 3.4648l1.7617 0.94726c0.59067-1.0987 1.3412-2.1099 2.2266-3.0059zm21.994 0.06641-1.4316 1.3945c0.88 0.90267 1.623 1.9188 2.207 3.0215l1.7676-0.9375c-0.67467-1.2707-1.5296-2.4412-2.543-3.4785zm-25.672 6.0566c-0.12133 0.392-0.22583 0.79265-0.3125 1.1953-0.22133 1.0093-0.3377 2.0429-0.3457 3.0723l2 0.01172c0.00667-0.88933 0.10683-1.7822 0.29883-2.6582 0.076-0.34933 0.16748-0.69258 0.27148-1.0312zm29.313 0.0918-1.916 0.57617c0.09333 0.31467 0.178 0.63303 0.25 0.95703 0.19867 0.896 0.29883 1.8183 0.29883 2.7383h2c0-1.0653-0.11832-2.1326-0.34766-3.1699-0.08133-0.372-0.17582-0.74023-0.28516-1.1016zm-27.814 6.75-1.9863 0.24023c0.148 1.2213 0.39314 2.5051 0.73047 3.8145l0.074219 0.2793 1.9336-0.50976-0.070312-0.26758c-0.316-1.2253-0.54564-2.422-0.68164-3.5566zm26.279 0.08594c-0.136 1.1027-0.36059 2.2694-0.66992 3.4707l-0.09375 0.35352 1.9316 0.51953 0.09766-0.37695c0.32933-1.2813 0.57341-2.5327 0.71875-3.7207zm-24.758 6.2383-1.8887 0.66016c0.444 1.2747 0.96669 2.5627 1.5547 3.832l1.8145-0.8418c-0.56-1.2093-1.0578-2.4371-1.4805-3.6504zm23.217 0.08594c-0.424 1.2067-0.9249 2.4345-1.4902 3.6465l1.8125 0.84766c0.59333-1.2733 1.1197-2.5627 1.5664-3.832zm-20.557 5.918-1.7598 0.95117 0.13672 0.25195c0.62 1.128 1.2915 2.2344 1.9941 3.2891l1.6641-1.1094c-0.672-1.0067-1.3116-2.0625-1.9062-3.1465zm17.879 0.08593-0.08203 0.15234c-0.608 1.1067-1.2677 2.1914-1.957 3.2207l1.6641 1.1133c0.72-1.0787 1.4077-2.212 2.041-3.3613l0.0957-0.17969zm-14.316 5.4375-1.5898 1.2129c0.86533 1.1333 1.7707 2.1955 2.6934 3.1582l1.4453-1.3828c-0.87067-0.90933-1.7288-1.9149-2.5488-2.9883zm10.748 0.07422c-0.81867 1.0667-1.678 2.0678-2.5566 2.9785l1.4414 1.3887c0.93067-0.96533 1.8391-2.0257 2.7031-3.1523zm-6.2559 4.7656-1.3066 1.5137c0.53467 0.46267 1.0757 0.89421 1.625 1.2969l0.58984 0.43359 0.5918-0.43359c0.548-0.40133 1.0903-0.83421 1.625-1.2969l-1.3066-1.5137c-0.30133 0.26-0.60482 0.51-0.91016 0.75-0.30533-0.24-0.6082-0.49-0.9082-0.75z\" fill=\"%2$s\"/></g>";
    } else {
      svgSrc = "%3Csvg xmlns='http://www.w3.org/2000/svg' width='%%width%%' height='%%height%%' viewBox='0 125 210 300'%3E%3Cpath fill='%1$s' d='M186.875,99.815c-9.595-35.23-42.611-61.408-81.874-61.408s-72.28,26.178-81.875,61.408 c-1.877,6.885-2.824,13.992-2.824,21.126c0,12.438,2.102,26.144,5.93,40.267c5.991,21.512,14.766,42.155,26.103,61.396 c14.546,24.86,32.87,47.061,52.667,61.404c19.766-14.344,38.132-36.521,52.67-61.404c11.332-19.241,20.104-39.885,26.096-61.396 c3.82-14.123,5.932-27.829,5.932-40.267C189.698,113.808,188.755,106.7,186.875,99.815z' /%3E%3Cpath fill='%2$s' d='M198.717,99.815c-9.288-41.994-47.666-73.74-93.716-73.74c-46.047,0-84.425,31.746-93.721,73.74 c-1.43,6.461-2.153,13.06-2.153,19.677c0,13.081,2.064,27.219,5.827,41.716c5.284,20.381,13.931,41.479,24.954,61.396 c17.193,31.046,40.173,59.3,65.093,77.396c24.941-18.096,47.899-46.304,65.088-77.396c11.026-19.917,19.669-41.016,24.958-61.396 c3.76-14.497,5.826-28.635,5.826-41.716C200.873,112.875,200.154,106.276,198.717,99.815z M183.767,161.208 c-5.992,21.512-14.764,42.155-26.096,61.396c-14.538,24.86-32.874,47.061-52.67,61.404c-19.767-14.344-38.121-36.521-52.667-61.404 c-11.337-19.241-20.111-39.885-26.103-61.396c-3.828-14.123-5.93-27.829-5.93-40.267c0-7.134,0.947-14.241,2.824-21.126 c9.595-35.23,42.612-61.408,81.875-61.408s72.279,26.178,81.874,61.408c1.88,6.885,2.823,13.992,2.823,21.126 C189.698,133.379,187.587,147.085,183.767,161.208z' /%3E";
    }

    if (promotedMarkerArg.getIsPromoted())
    {
      svgSrc += getCFPromotionIconSVG();
    }

    if (traffic != null && traffic != 0 && zoomLevel != null) {
      svgSrc += getPredictionIconSVG(traffic, zoomLevel);
    }

    svgSrc += "%3C/svg%3E";

    return "data:image/svg+xml," + svgSrc.replace("%1$s", bottomInnerEncoded).replace("%2$s", bottomOutlineEncoded).replace("%4$s", topOutlineEncoded)
            .replace("%%width%%", Double.toString(svgWidth)).replace("%%height%%", Double.toString(svgHeight))
            .replace("%%promotedColor%%", promotedEncoded);
  }

  private String getCFPromotionIconSVG() {
    return  "%3Cpath fill='%%promotedColor%%' d='M153.79,0c26.118,0,47.299,21.178,47.299,47.303c0,26.127-21.181,47.302-47.299,47.302 c-26.126,0-47.308-21.175-47.308-47.302C106.482,21.178,127.664,0,153.79,0z'/%3E%3Cpath fill='%23FFFFFF' d='M146.809,69.018c0-4.655,3.102-7.756,7.752-7.756c4.652,0,7.753,3.101,7.753,7.756 c0,4.651-3.101,7.752-7.753,7.752C149.91,76.77,146.809,73.669,146.809,69.018z M149.139,54.284l-2.33-36.447h13.184l-1.551,36.447 H149.139z'/%3E";
  }

  private String getPredictionIconSVG(float traffic, int zoomLevel) {
    if(zoomLevel >= 16){
      if(traffic > 1){
        return "<g   id='g1330'><g     style='fill:none'     id='g1205'     transform='matrix(8.16004,0,0,8.16004,29.438308,-139.76169)'><path       id='rect1187'       transform='rotate(-45,0,15.2549)'       style='fill:%4$s'       d='M 1.2339799,15.2549 H 11.722821 c 0.683624,0 1.233979,0.550355 1.233979,1.23398 v 10e-6 c 0,0.683625 -0.550355,1.23398 -1.233979,1.23398 H 1.2339799 C 0.55035505,17.72287 0,17.172515 0,16.48889 v -10e-6 C 0,15.805255 0.55035505,15.2549 1.2339799,15.2549 Z' /><path       id='rect1189'       transform='rotate(45,9.35809,6)'       style='fill:%4$s'       d='m 10.59207,6 h 10.488841 c 0.683625,0 1.23398,0.5503551 1.23398,1.2339799 v 1.01e-5 c 0,0.6836248 -0.550355,1.2339799 -1.23398,1.2339799 H 10.59207 c -0.6836245,0 -1.2339796,-0.5503551 -1.2339796,-1.2339799 v -1.01e-5 C 9.3580904,6.5503551 9.9084455,6 10.59207,6 Z' /><path       id='rect1191'       transform='rotate(-45,0,9.25488)'       style='fill:%4$s'       d='M 1.2339799,9.25488 H 11.722821 c 0.683624,0 1.233979,0.550355 1.233979,1.23398 v 10e-6 c 0,0.683625 -0.550355,1.23398 -1.233979,1.23398 H 1.2339799 C 0.55035505,11.72285 0,11.172495 0,10.48887 v -10e-6 C 0,9.805235 0.55035505,9.25488 1.2339799,9.25488 Z' /><path       id='rect1193'       transform='rotate(45,9.35809,0)'       style='fill:%4$s'       d='m 10.59207,0 h 10.488841 c 0.683625,0 1.23398,0.55035505 1.23398,1.2339799 v 1.01e-5 c 0,0.6836248 -0.550355,1.2339799 -1.23398,1.2339799 H 10.59207 c -0.6836245,0 -1.2339796,-0.5503551 -1.2339796,-1.2339799 v -1.01e-5 C 9.3580904,0.55035505 9.9084455,0 10.59207,0 Z' /></g></g>";
      }else if(traffic > 0 && traffic <= 1){
        return "<g   style='fill:none'   id='g1371'   transform='matrix(8.16004,0,0,8.16004,29.438308,-106.16169)'><rect     y='9.25488'     width='12.9568'     height='2.4679699'     rx='1.2339799'     transform='rotate(-45,0,9.25488)'     fill='%4$s'     id='rect1359'     x='0' /><rect     x='9.3580904'     width='12.9568'     height='2.4679699'     rx='1.2339799'     transform='rotate(45,9.35809,0)'     fill='%4$s'     id='rect1361'     y='0' /></g>";
      }else if(traffic < 0 && traffic >= -1){
        return "<g style='fill:none' id='g1569'   transform='matrix(8.1600216,0,0,8.1600216,29.438085,-106.1617)'><rect     x='18.52'     y='1.74512'     width='12.9568'     height='2.4679699'     rx='1.2339799'     transform='rotate(135,18.52,1.74512)'     fill='%4$s'     id='rect1557' /><rect     x='9.16187'     y='11'     width='12.9568'     height='2.4679699'     rx='1.2339799'     transform='rotate(-135,9.16187,11)'     fill='%4$s'     id='rect1559' /></g>";
      }else if(traffic < -1){
        return "<g   id='g1135'   transform='matrix(0.89473684,0,0,0.89473684,11.052632,-14.273687)'><g     id='g1092'     transform='matrix(0.90476189,0,0,0.90476189,10.000002,-12.914287)'><g       id='g835'       transform='matrix(10.080027,0,0,10.080027,11.658806,-140.74092)'><path         id='rect2'         transform='rotate(135,18.52,1.74512)'         style='fill:%4$s'         d='m 19.75398,1.74512 h 10.488841 c 0.683625,0 1.23398,0.5503551 1.23398,1.23398 v 1e-5 c 0,0.6836249 -0.550355,1.2339799 -1.23398,1.2339799 H 19.75398 C 19.070356,4.2130899 18.52,3.6627349 18.52,2.97911 v -1e-5 c 0,-0.6836249 0.550356,-1.23398 1.23398,-1.23398 z' /><path         id='rect4'         transform='rotate(-135,9.16187,11)'         style='fill:%4$s'         d='m 10.39585,11 h 10.488841 c 0.683624,0 1.233979,0.550355 1.233979,1.23398 v 10e-6 c 0,0.683625 -0.550355,1.23398 -1.233979,1.23398 H 10.39585 c -0.6836249,0 -1.23398,-0.550355 -1.23398,-1.23398 v -10e-6 C 9.16187,11.550355 9.7122251,11 10.39585,11 Z' /><path         id='rect6'         transform='rotate(135,18.52,7.74512)'         style='fill:%4$s'         d='m 19.75398,7.74512 h 10.488841 c 0.683625,0 1.23398,0.5503551 1.23398,1.23398 v 10e-6 c 0,0.6836249 -0.550355,1.23398 -1.23398,1.23398 H 19.75398 C 19.070356,10.21309 18.52,9.6627349 18.52,8.97911 v -10e-6 c 0,-0.6836249 0.550356,-1.23398 1.23398,-1.23398 z' /><path         id='rect8'         transform='rotate(-135,9.16187,17)'         style='fill:%4$s'         d='m 10.39585,17 h 10.488841 c 0.683624,0 1.233979,0.550355 1.233979,1.23398 v 10e-6 c 0,0.683625 -0.550355,1.23398 -1.233979,1.23398 H 10.39585 c -0.6836249,0 -1.23398,-0.550355 -1.23398,-1.23398 v -10e-6 C 9.16187,17.550355 9.7122251,17 10.39585,17 Z' /></g></g></g>";
      }
    }
    return " ";
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

    marker.setIcon(getIcon());
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

  static class PlannedMarkerArgStructure
  {
    public PlannedMarkerArgStructure(boolean isPlanned, String outlineColor, String fillColor)
    {
      this.isPlanned = isPlanned;
      this.outlineColor = outlineColor;
      this.fillColor = fillColor;
    }
    private boolean isPlanned;

    private String outlineColor;

    private String fillColor;

    public boolean getIsPlannedStation()
    {
      return isPlanned;
    }

    public String getOutlineColor()
    {
      return outlineColor;
    }
    public String getFillColor()
    {
      return fillColor;
    }
  }


  static class MaintenanceMarkerArgStructure
  {
    public MaintenanceMarkerArgStructure(boolean isMaintenance, String outlineColor, String fillColor)
    {
      this.isMaintenance = isMaintenance;
      this.outlineColor = outlineColor;
      this.fillColor = fillColor;
    }
    private boolean isMaintenance;

    private String outlineColor;

    private String fillColor;

    public boolean getMaintenanceStation()
    {
      return isMaintenance;
    }
    public String getOutlineColor()
    {
      return outlineColor;
    }
    public String getFillColor()
    {
      return fillColor;
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
        MapMarkerManager.AirMapMarkerSharedIcon sharedIcon = this.markerManager.getSharedIcon(uri);
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
        String depotType = jsonObj.has("depotType") ? jsonObj.getString("depotType") : null;
        Boolean isPromoted = new Boolean(promoted.getString("isPromoted"));
        String promotedMarkerColor = promoted.getString("color");
        PromotedMarkerArgStructure promotedMarkerArgs = new PromotedMarkerArgStructure(isPromoted, promotedMarkerColor);
        String traffic = jsonObj.has("traffic") ? jsonObj.getString("traffic"): "0";
        String zoomLevel = jsonObj.has("zoomLevel") ? jsonObj.getString("zoomLevel"): "16";
        Boolean hasValetService = jsonObj.has("hasValetService") ? true: false;
        JSONObject planned = jsonObj.has("planned") ? jsonObj.getJSONObject("planned") : null;
        Boolean isPlanned = (planned != null && planned.has("isPlanned")) ? new Boolean(planned.getString("isPlanned")) :false;
        String plannedMarkerOutlineColor = (planned != null) ? planned.getString("outlineColor") : "#D3D3D3"; // fallback to pale grey color
        String plannedMarkerFillColor = (planned != null) ? planned.getString("fillColor") : "#ffffff"; // fallback to white color
        PlannedMarkerArgStructure plannedMarkerArgs = new PlannedMarkerArgStructure(isPlanned, plannedMarkerOutlineColor, plannedMarkerFillColor);
        JSONObject maintenance = jsonObj.has("maintenance") ? jsonObj.getJSONObject("maintenance") : null;
        Boolean isMaintenance = (maintenance != null && maintenance.has("isMaintenance")) ? new Boolean(maintenance.getString("isMaintenance")) : false;
        String maintenanceMarkerOutlineColor = (maintenance != null) ? maintenance.getString("outlineColor") : "#D3D3D3"; // fallback to pale grey color
        String maintenanceMarkerFillColor = (maintenance != null) ? maintenance.getString("fillColor") : "#ffffff"; // fallback to white color;
        MaintenanceMarkerArgStructure maintenanceMarkerArgs = new MaintenanceMarkerArgStructure(isMaintenance, maintenanceMarkerOutlineColor, maintenanceMarkerFillColor);

        String svgDataUrI = getCFSvg(hasValetService, Math.round(Float.parseFloat(level)), topOutline, bottomOutline, topInner, bottomInner, promotedMarkerArgs, depotType, Float.parseFloat(traffic), Integer.valueOf(zoomLevel), plannedMarkerArgs, maintenanceMarkerArgs);
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
      if (this.markerManager != null) {
        this.markerManager.getSharedIcon(uri).updateIcon(iconBitmapDescriptor, iconBitmap);
      }
      update(true);
    }
  }

  public void setIconBitmapDescriptor(BitmapDescriptor bitmapDescriptor, Bitmap bitmap) {
    this.iconBitmapDescriptor = bitmapDescriptor;
    this.iconBitmap = bitmap;
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
    if (!(child instanceof MapCallout)) {
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
  public void addToMap(Object collection) {
    MarkerManager.Collection markerCollection = (MarkerManager.Collection) collection;
    marker = markerCollection.addMarker(getMarkerOptions());
    updateTracksViewChanges();
  }

  @Override
  public void removeFromMap(Object collection) {
    if (marker == null) {
      return;
    }
    MarkerManager.Collection markerCollection = (MarkerManager.Collection) collection;
    markerCollection.remove(marker);
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

  public void setCalloutView(MapCallout view) {
    this.calloutView = view;
  }

  public MapCallout getCalloutView() {
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
