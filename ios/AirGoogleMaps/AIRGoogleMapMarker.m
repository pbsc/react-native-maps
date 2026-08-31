//
//  AIRGoogleMapMarker.m
//  AirMaps
//
//  Created by Gil Birman on 9/2/16.
//

#ifdef HAVE_GOOGLE_MAPS

#import "AIRGoogleMapMarker.h"
#import <GoogleMaps/GoogleMaps.h>
#import <React/RCTBridge.h>
#import <React/RCTBridge+Private.h>
#import <React/RCTImageLoaderProtocol.h>
#import <React/RCTUtils.h>
#import "AIRGMSMarker.h"
#import "AIRGoogleMapCallout.h"
#import "AIRDummyView.h"
#import "GlobalVars.h"

CGRect unionRect(CGRect a, CGRect b) {
    return CGRectMake(
                      MIN(a.origin.x, b.origin.x),
                      MIN(a.origin.y, b.origin.y),
                      MAX(a.size.width, b.size.width),
                      MAX(a.size.height, b.size.height));
}

// ============================================================================
// PBSC PATCH - CLEANUP WHEN UPSTREAM LANDS A FIX. Check on upgrade:
//   https://github.com/react-native-maps/react-native-maps/pull/5966
//   https://github.com/react-native-maps/react-native-maps/issues/5971
// When bumping react-native-maps past whatever version merges #5966 (or an
// equivalent official fix for the 0x0 iconView bug), diff this file against
// the new upstream AIRGoogleMapMarker.m and remove whatever it already
// covers:
//   - layoutSubviews' v.bounds.size measurement (matches #5966 exactly - just
//     delete our copy once upstream has it).
//   - the layoutSubviews calls added to didInsertInMap: and
//     iconViewInsertSubview: (also matches #5966 - delete once upstream has
//     it).
//   - airMapsFitIconViewWithAttemptsRemaining: and its call sites (our own
//     safety-net retry loop, not part of #5966 - keep only if still needed
//     after confirming the upstream fix's timing is sufficient on real
//     devices, otherwise delete along with kAIRGoogleMapMarkerIconFitMaxAttempts
//     and the @interface forward declaration below).
//   - the `redraw` call added at the end of didInsertInMap: (handles marker
//     re-attachment, e.g. after a zIndex change - #5966 does NOT cover this;
//     check whether the upstream fix (or a newer issue/PR) covers it before
//     assuming it's safe to delete too).
// ============================================================================
//
// Under Fabric, a custom marker child (View/SVG) can be mounted into _iconView
// before React Native's own layout pass has given it a real, non-zero frame -
// GMSMarker then snapshots an empty icon and never re-checks it, since nothing
// tells it the content changed later. This is the same root cause reported in
// react-native-maps/react-native-maps#5971, #5406, #5964, and fixed for one
// production user by PR #5966 (re-measure layoutSubviews once now plus once on
// the next run loop turn, which is the primary fix applied throughout this
// file). This retry loop is only a safety net for slower devices where even
// #5966's fixed two-pass timing might still see a not-yet-laid-out child - it
// keeps re-measuring for a few more run-loop turns before giving up. Upstream
// has no official/merged fix yet.
static const NSInteger kAIRGoogleMapMarkerIconFitMaxAttempts = 4;

@interface AIRGoogleMapMarker ()
- (void)airMapsFitIconViewWithAttemptsRemaining:(NSInteger)attemptsRemaining;
@end

@implementation AIRGoogleMapMarker {
    RCTImageLoaderCancellationBlock _reloadImageCancellationBlock;
    RCTBubblingEventBlock _onPress;
    RCTDirectEventBlock _onSelect;
    RCTDirectEventBlock _onDeselect;
    __weak UIImageView *_iconImageView;
    UIView *_iconView;
    UIColor *_pinColor;
    CLLocationCoordinate2D _coordinates;
    CLLocationDegrees _rotation;
    BOOL _tracksInfoWindowChanges;
    BOOL _tracksViewChanges;
    BOOL _draggable;
    BOOL _tappable;
    BOOL _flat;
    double _opacity;
    NSString* _identifier;
    NSString* _title;
    NSString* _subtitle;

}

- (instancetype)init
{
    if ((self = [super init])) {
        _tracksViewChanges = true;
        _tracksInfoWindowChanges = false;
        _tappable = true;
        _opacity = 1.0;
    }
    return self;
}

- (void)layoutSubviews {
    float width = 0;
    float height = 0;

    for (UIView *v in [_iconView subviews]) {

        // frame includes a UIView transform. Custom markers can be scaled by
        // Reanimated while entering, so use the stable layout bounds instead
        // (react-native-maps/react-native-maps#5966).
        float fw = v.bounds.size.width;
        float fh = v.bounds.size.height;

        width = MAX(fw, width);
        height = MAX(fh, height);
    }

    [_iconView setFrame:CGRectMake(0, 0, width, height)];
}


- (UIView *) iconView
{
    return _iconView;
}

- (void) didUpdateReactSubviews
{
    [super didUpdateReactSubviews];
    if (_iconView){
        [_iconView setFrame:self.frame];
    }
}

- (void) didInsertInMap:(AIRGoogleMap *) map
{
    _realMarker = [AIRGMSMarker new];
    _realMarker.fakeMarker = self;
    _realMarker.tracksViewChanges = _tracksViewChanges;
    _realMarker.tracksInfoWindowChanges = _tracksInfoWindowChanges;

    [_realMarker setPosition:_coordinates];
    if (_iconView){
        [_realMarker setIconView:_iconView];
    }
    if (_rotation != 0){
        [_realMarker setRotation:_rotation];
    }
    if (_identifier){
        [_realMarker setIdentifier:_identifier];
    }
    if (_title){
        [_realMarker setTitle:_title];
    }
    if (_subtitle){
        [_realMarker setSnippet:_subtitle];
    }
    if (!CGPointEqualToPoint(_anchor, CGPointZero)){
        [_realMarker setGroundAnchor:_anchor];
    }
    if (!CGPointEqualToPoint(_calloutAnchor, CGPointZero)){
        [_realMarker setInfoWindowAnchor:_calloutAnchor];
    }
    if (_flat){
        [_realMarker setFlat:_flat];
    }
    if (_draggable){
        [_realMarker setDraggable:_draggable];
    }
    [_realMarker setTappable:_tappable];
    if (_pinColor){
        _realMarker.icon = [GMSMarker markerImageWithColor:_pinColor];
    }
    if (_opacity != 1.0){
        [_realMarker setOpacity:_opacity];
    }
    if (_onSelect){
        [_realMarker setOnSelect:_onSelect];
    }
    if (_onDeselect){
        [_realMarker setOnDeselect:_onDeselect];
    }
    if (_onPress){
        [_realMarker setOnPress:_onPress];
    }
    if (_zIndex){
        [_realMarker setZIndex:_zIndex];
    }
    // Fabric (RN new architecture) calls updateProps — and therefore setImageSrc:/
    // setIconSrc: — before the marker is added to the map, so _realMarker does not
    // exist yet and the icon assignment is a silent no-op. Replay the stored value
    // here now that _realMarker is live. pinColor is applied above first so that a
    // custom image always wins over it.
    // TODO: remove this block when upgrading to bridgeless RN and the old-arch
    // AIRGoogleMapMarker prop-setter pathway is retired.
    if (_imageSrc) {
        [self setImageSrc:_imageSrc];
    } else if (_iconSrc) {
        [self setIconSrc:_iconSrc];
    }
    // Size _iconView before attaching to the map: once, now, and once more on
    // the next run loop turn since Fabric lays out children asynchronously
    // and may not have finished yet (react-native-maps/react-native-maps#5966).
    [self layoutSubviews];
    dispatch_async(dispatch_get_main_queue(), ^{
        [self layoutSubviews];
    });
    [_realMarker setMap:map];

    // A marker can be reinserted into the map without a fresh iconView (e.g. a
    // zIndex change forces Fabric to remove+reinsert it) - GMSMarker only
    // rasterizes iconView content while attached to a map, so force a redraw
    // now that it is, in case the last snapshot (if any) was already stale.
    // #5966 doesn't cover this reattachment case; it only re-measures before a
    // marker's first attachment.
    if (_iconView && _realMarker.iconView) {
        [self redraw];
    }
}

- (void)iconViewInsertSubview:(UIView*)subview atIndex:(NSInteger)atIndex {
    if (!_iconView){
        _iconView = [[UIView alloc] init];
    }
    [_iconView insertSubview:subview atIndex:atIndex];
    // Size _iconView to fit its children before setting it on the marker
    // (react-native-maps/react-native-maps#5966).
    [self layoutSubviews];
    if (!_realMarker.iconView) {
        _realMarker.iconView = _iconView;
    }
    // Fabric lays out children asynchronously, so measure once more next run
    // loop (#5966). That alone was sufficient in #5966's own production report,
    // but keep retrying a few more turns as a safety net for slower devices
    // where even a second pass might still see a not-yet-laid-out child.
    dispatch_async(dispatch_get_main_queue(), ^{
        [self layoutSubviews];
        [self airMapsFitIconViewWithAttemptsRemaining:kAIRGoogleMapMarkerIconFitMaxAttempts];
    });
}

// See the comment on kAIRGoogleMapMarkerIconFitMaxAttempts above.
- (void)airMapsFitIconViewWithAttemptsRemaining:(NSInteger)attemptsRemaining {
    if (!_iconView) return;

    BOOL hasNonZeroSubview = NO;
    for (UIView *v in [_iconView subviews]) {
        if (v.bounds.size.width > 0 && v.bounds.size.height > 0) {
            hasNonZeroSubview = YES;
            break;
        }
    }

    if (hasNonZeroSubview || attemptsRemaining <= 0) {
        return;
    }

    __weak AIRGoogleMapMarker *weakSelf = self;
    dispatch_async(dispatch_get_main_queue(), ^{
        __strong AIRGoogleMapMarker *strongSelf = weakSelf;
        if (!strongSelf) return;
        [strongSelf layoutSubviews];
        [strongSelf airMapsFitIconViewWithAttemptsRemaining:attemptsRemaining - 1];
    });
}

- (void)insertReactSubview:(id<RCTComponent>)subview atIndex:(NSInteger)atIndex {
    if ([subview isKindOfClass:[AIRGoogleMapCallout class]]) {
        self.calloutView = (AIRGoogleMapCallout *)subview;
    } else { // a child view of the marker
        [self iconViewInsertSubview:(UIView*)subview atIndex:atIndex+1];
    }
    AIRDummyView *dummySubview = [[AIRDummyView alloc] initWithView:(UIView *)subview];
    [super insertReactSubview:(UIView*)dummySubview atIndex:atIndex];
}

- (void)removeReactSubview:(id<RCTComponent>)dummySubview {
    UIView *subview = [dummySubview isKindOfClass:[AIRDummyView class]] ? ((AIRDummyView *)dummySubview).view : (UIView *)dummySubview;

    if ([subview isKindOfClass:[AIRGoogleMapCallout class]]) {
        self.calloutView = nil;
    } else {
        [subview removeFromSuperview];
    }
    [super removeReactSubview:(UIView*)dummySubview];
}

- (void)showCalloutView {
    [_realMarker.map setSelectedMarker:_realMarker];
}

- (void)hideCalloutView {
    [_realMarker.map setSelectedMarker:Nil];
}

- (void)redraw {
    if (!_realMarker.iconView) return;

    BOOL oldValue = _realMarker.tracksViewChanges;

    if (oldValue == YES)
    {
        // Immediate refresh, like right now. Not waiting for next frame.
        UIView *view = _realMarker.iconView;
        _realMarker.iconView = nil;
        _realMarker.iconView = view;
    }
    else
    {
        // Refresh according to docs
        _realMarker.tracksViewChanges = YES;
        _realMarker.tracksViewChanges = NO;
    }
}

- (UIView *)markerInfoContents {
    if (self.calloutView && !self.calloutView.tooltip) {
        return self.calloutView;
    }
    return nil;
}

- (UIView *)markerInfoWindow {
    if (self.calloutView && self.calloutView.tooltip) {
        return self.calloutView;
    }
    return nil;
}

- (void)didTapInfoWindowOfMarker:(AIRGMSMarker *)marker point:(CGPoint)point frame:(CGRect)frame {
    if (self.calloutView && self.calloutView.onPress) {
        //todo: why not 'callout-press' ?
        id event = @{
            @"action": @"marker-overlay-press",
            @"id": self.identifier ?: @"unknown",
            @"point": @{
                @"x": @(point.x),
                @"y": @(point.y),
            },
            @"frame": @{
                @"x": @(frame.origin.x),
                @"y": @(frame.origin.y),
                @"width": @(frame.size.width),
                @"height": @(frame.size.height),
            }
        };
        self.calloutView.onPress(event);
    }
}

- (void)didTapInfoWindowOfMarker:(AIRGMSMarker *)marker {
    [self didTapInfoWindowOfMarker:marker point:CGPointMake(-1, -1) frame:CGRectZero];
}

- (void)didTapInfoWindowOfMarker:(AIRGMSMarker *)marker subview:(AIRGoogleMapCalloutSubview*)subview point:(CGPoint)point frame:(CGRect)frame {
    if (subview && subview.onPress) {
        //todo: why not 'callout-inside-press' ?
        id event = @{
            @"action": @"marker-inside-overlay-press",
            @"id": self.identifier ?: @"unknown",
            @"point": @{
                @"x": @(point.x),
                @"y": @(point.y),
            },
            @"frame": @{
                @"x": @(frame.origin.x),
                @"y": @(frame.origin.y),
                @"width": @(frame.size.width),
                @"height": @(frame.size.height),
            }
        };
        subview.onPress(event);
    } else {
        [self didTapInfoWindowOfMarker:marker point:point frame:frame];
    }
}

- (void)didBeginDraggingMarker:(AIRGMSMarker *)marker {
    if (!self.onDragStart) return;
    self.onDragStart([self makeEventData]);
}

- (void)didEndDraggingMarker:(AIRGMSMarker *)marker {
    if (!self.onDragEnd) return;
    self.onDragEnd([self makeEventData]);
}

- (void)didDragMarker:(AIRGMSMarker *)marker {
    if (!self.onDrag) return;
    self.onDrag([self makeEventData]);
}

- (void)setCoordinate:(CLLocationCoordinate2D)coordinate {
    _realMarker.position = coordinate;
    _coordinates = coordinate;
}

- (CLLocationCoordinate2D)coordinate {
    return _realMarker.position;
}

- (void)setRotation:(CLLocationDegrees)rotation {
    _realMarker.rotation = rotation;
    _rotation = rotation;
}

- (CLLocationDegrees)rotation {
    return _realMarker.rotation;
}

- (void)setIdentifier:(NSString *)identifier {
    _realMarker.identifier = identifier;
    _identifier = identifier;
}

- (NSString *)identifier {
    return _realMarker.identifier;
}

- (void)setOnPress:(RCTBubblingEventBlock)onPress {
    _realMarker.onPress = onPress;
    _onPress = onPress;
}

- (RCTBubblingEventBlock)onPress {
    return _realMarker.onPress;
}

- (void)setOnSelect:(RCTDirectEventBlock)onSelect {
    _realMarker.onSelect = onSelect;
    _onSelect = onSelect;
}

- (RCTDirectEventBlock)onSelect {
    return _realMarker.onSelect;
}

- (void)setOnDeselect:(RCTDirectEventBlock)onDeselect {
    _realMarker.onDeselect = onDeselect;
    _onDeselect = onDeselect;
}

- (RCTDirectEventBlock)onDeselect {
    return _realMarker.onDeselect;
}

- (void)setOpacity:(double)opacity
{
    _realMarker.opacity = opacity;
    _opacity = opacity;
}

// Sets _realMarker.icon directly. Falls back to a solid blue pin when image is
// nil or invalid so that a rendering failure is visually obvious during development.
- (void)setIcon:(UIImage*)image {
    CGImageRef cgref = [image CGImage];
    CIImage *cim = [image CIImage];
    if (cim == nil && cgref == NULL) {
        _realMarker.icon = [GMSMarker markerImageWithColor:UIColor.blueColor];
    } else {
        _realMarker.icon = image;
    }
}

// Handles PBSC's `useCfMarkerJSON:` and inline `<svg` URI schemes via GlobalVars.
// Passing nil clears the icon (e.g. when the `image` prop is removed).
// _imageSrc is stored so didInsertInMap: can replay the assignment after _realMarker
// is created — necessary because Fabric's updateProps fires before the marker is
// added to the map. When RN removes the Paper/Fabric split (bridgeless-only), the
// didInsertInMap: replay becomes the only call site and this store can be removed.
- (void)setImageSrc:(NSString *)imageSrc {
    _imageSrc = imageSrc;
    if (!imageSrc) {
        _realMarker.icon = nil;
        return;
    }
    UIImage *image = [[GlobalVars sharedInstance] getSharedUIImage:imageSrc];
    [self setIcon:image];
}

// Loads a remote/bundled image via RN's ImageLoader and sets it as the marker icon.
// [RCTBridge currentBridge] is a pre-bridgeless compatibility shim. When this project
// migrates to the bridgeless RN runtime (RN 0.80+), replace with an image-loader
// reference injected at construction time (e.g. via RCTImageLoader from TurboModules).
- (void)setIconSrc:(NSString *)iconSrc
{
    _iconSrc = iconSrc;

    if (_reloadImageCancellationBlock) {
        _reloadImageCancellationBlock();
        _reloadImageCancellationBlock = nil;
    }

    if (!_realMarker.icon) {
        // Immediately set an empty image to avoid a flash of the default red pin
        // while the async load is in-flight.
        // See: https://github.com/react-native-maps/react-native-maps/issues/3657
        _realMarker.icon = [[UIImage alloc] init];
    }

    _reloadImageCancellationBlock =
    [[[RCTBridge currentBridge] moduleForName:@"ImageLoader"] loadImageWithURLRequest:[RCTConvert NSURLRequest:_iconSrc]
                                                               size:self.bounds.size
                                                              scale:RCTScreenScale()
                                                            clipped:YES
                                                         resizeMode:RCTResizeModeCenter
                                                      progressBlock:nil
                                                   partialLoadBlock:nil
                                                    completionBlock:^(NSError *error, UIImage *image) {
        if (error) {
            NSLog(@"[AIRGoogleMapMarker] iconSrc load error: %@", error);
        }
        dispatch_async(dispatch_get_main_queue(), ^{
            self->_realMarker.icon = image;
        });
    }];
}

- (void)setTitle:(NSString *)title {
    _realMarker.title = [title copy];
    _title = title;
}

- (NSString *)title {
    return _realMarker.title;
}

- (void)setSubtitle:(NSString *)subtitle {
    _realMarker.snippet = subtitle;
    _subtitle = subtitle;
}

- (NSString *)subtitle {
    return _realMarker.snippet;
}

- (void)setPinColor:(UIColor *)pinColor {
    _pinColor = pinColor;
    _realMarker.icon = [GMSMarker markerImageWithColor:pinColor];
}

- (void)setAnchor:(CGPoint)anchor {
    _anchor = anchor;
    _realMarker.groundAnchor = anchor;
}

- (void)setCalloutAnchor:(CGPoint)calloutAnchor {
    _calloutAnchor = calloutAnchor;
    _realMarker.infoWindowAnchor = calloutAnchor;
}


- (void)setZIndex:(NSInteger)zIndex
{
    _zIndex = zIndex;
    _realMarker.zIndex = (int)zIndex;
}

- (void)setDraggable:(BOOL)draggable {
    _realMarker.draggable = draggable;
    _draggable = draggable;
}

- (BOOL)draggable {
    return _realMarker.draggable;
}

- (void)setTappable:(BOOL)tappable {
    _realMarker.tappable = tappable;
    _tappable = tappable;
}

- (BOOL)tappable {
    return _realMarker.tappable;
}

- (void)setFlat:(BOOL)flat {
    _realMarker.flat = flat;
    _flat = flat;
}

- (BOOL)flat {
    return _realMarker.flat;
}

- (void)setTracksViewChanges:(BOOL)tracksViewChanges {
    _tracksViewChanges = tracksViewChanges;
    _realMarker.tracksViewChanges = tracksViewChanges;
}

- (BOOL)tracksViewChanges {
    return _realMarker.tracksViewChanges;
}

- (void)setTracksInfoWindowChanges:(BOOL)tracksInfoWindowChanges {
    _tracksInfoWindowChanges = tracksInfoWindowChanges;
    _realMarker.tracksInfoWindowChanges = tracksInfoWindowChanges;
}

- (BOOL)tracksInfoWindowChanges {
    return _realMarker.tracksInfoWindowChanges;
}


- (id)makeEventData:(NSString *)action {
    CLLocationCoordinate2D coordinate = self.realMarker.position;
    CGPoint position = [self.realMarker.map.projection pointForCoordinate:coordinate];

    return @{
        @"id": self.identifier ?: @"unknown",
        @"position": @{
            @"x": @(position.x),
            @"y": @(position.y),
        },
        @"coordinate": @{
            @"latitude": @(coordinate.latitude),
            @"longitude": @(coordinate.longitude),
        },
        @"action": action,
    };
}

- (id)makeEventData {
    return [self makeEventData:@"unknown"];
}

@end

#endif
