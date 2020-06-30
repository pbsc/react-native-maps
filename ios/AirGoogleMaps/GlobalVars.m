//
//  GlobalVars.m
//
//  Created by Eric Kim on 2017-04-04.
//  Copyright © 2017 Apply Digital. All rights reserved.
//

#import "GlobalVars.h"
#import <SVGKit/SVGKit.h>

@implementation GlobalVars

@synthesize dict = _dict;

+ (GlobalVars *)sharedInstance {
  static dispatch_once_t onceToken;
  static GlobalVars *instance = nil;
  dispatch_once(&onceToken, ^{
    instance = [[GlobalVars alloc] init];
  });
  return instance;
}

- (UIImage *) getCFMarker:(NSString *)topOutline bottomOutline:(NSString *)bottomOutline topInner:(NSString *)topInner bottomInner:(NSString *)bottomInner level:(NSString *)level {
    float markerLevel = [level floatValue];

    if (markerLevel == 0) {
      // svg with level 0
      NSString *svgSrc = @"<svg xmlns='http://www.w3.org/2000/svg' width='25' height='68' viewBox='0 0 210 300'><g id='_100'><path fill='%2$@' d='M194.67,80.76C184.16,42.18,148,13.51,105,13.51S25.84,42.18,15.33,80.76a88.07,88.07,0,0,0-3.09,23.14c0,13.62,2.3,28.63,6.49,44.1a281.12,281.12,0,0,0,28.59,67.24c15.93,27.23,36,51.54,57.68,67.25,21.65-15.71,41.76-40,57.68-67.25A281.12,281.12,0,0,0,191.27,148c4.18-15.47,6.49-30.48,6.49-44.1A88.07,88.07,0,0,0,194.67,80.76Z'/><path fill='%1$@' d='M207.64,80.76C197.46,34.77,155.43,0,105,0S12.54,34.77,2.36,80.76A99.59,99.59,0,0,0,0,102.31C0,116.64,2.26,132.12,6.38,148c5.79,22.32,15.26,45.43,27.33,67.24,18.83,34,44,64.94,71.29,84.76,27.32-19.82,52.46-50.71,71.29-84.76,12.07-21.81,21.54-44.92,27.33-67.24,4.12-15.88,6.38-31.36,6.38-45.69A99.59,99.59,0,0,0,207.64,80.76Zm-45,134.48c-15.92,27.23-36,51.54-57.68,67.25-21.65-15.71-41.75-40-57.68-67.25A281.12,281.12,0,0,1,18.73,148c-4.19-15.47-6.49-30.48-6.49-44.1a88.07,88.07,0,0,1,3.09-23.14C25.84,42.18,62,13.51,105,13.51s79.16,28.67,89.67,67.25a88.07,88.07,0,0,1,3.09,23.14c0,13.62-2.31,28.63-6.49,44.1A281.12,281.12,0,0,1,162.68,215.24Z'/></g></svg>";
      NSString *svgString = [NSString stringWithFormat:svgSrc, topOutline, topInner];
      SVGKSource *source = [SVGKSourceString sourceFromContentsOfString:svgString];
      return [SVGKImage imageWithSource:source].UIImage;
    }

    if (markerLevel > 0 && markerLevel <= 37) {
      // svg with level 25
      NSString *svgSrc = @"<svg xmlns='http://www.w3.org/2000/svg' width='25' height='68' viewBox='0 0 210 300'><g id='_100' data-name='100'><path fill='%1$@' class='cls-1' d='M194.67,80.76C184.16,42.18,148,13.51,105,13.51S25.84,42.18,15.33,80.76a88.07,88.07,0,0,0-3.09,23.14c0,13.62,2.3,28.63,6.49,44.1a281.12,281.12,0,0,0,28.59,67.24H162.68A281.12,281.12,0,0,0,191.27,148c4.18-15.47,6.49-30.48,6.49-44.1A88.07,88.07,0,0,0,194.67,80.76Z'/><path fill='%2$@' class='cls-2' d='M105,282.49c21.65-15.71,41.76-40,57.68-67.25H47.32C63.25,242.47,83.35,266.78,105,282.49Z'/><path fill='%3$@' class='cls-3' d='M105,282.49c-21.65-15.71-41.75-40-57.68-67.25H33.71c18.83,34,44,64.94,71.29,84.76,27.32-19.82,52.46-50.71,71.29-84.76H162.68C146.76,242.47,126.65,266.78,105,282.49Z'/><path fill='%4$@' d='M207.64,80.76C197.46,34.77,155.43,0,105,0S12.54,34.77,2.36,80.76A99.59,99.59,0,0,0,0,102.31C0,116.64,2.26,132.12,6.38,148c5.79,22.32,15.26,45.43,27.33,67.24H47.32A281.12,281.12,0,0,1,18.73,148c-4.19-15.47-6.49-30.48-6.49-44.1a88.07,88.07,0,0,1,3.09-23.14C25.84,42.18,62,13.51,105,13.51s79.16,28.67,89.67,67.25a88.07,88.07,0,0,1,3.09,23.14c0,13.62-2.31,28.63-6.49,44.1a281.12,281.12,0,0,1-28.59,67.24h13.61c12.07-21.81,21.54-44.92,27.33-67.24,4.12-15.88,6.38-31.36,6.38-45.69A99.59,99.59,0,0,0,207.64,80.76Z'/></g></svg>";
        NSString *svgString = [NSString stringWithFormat:svgSrc, topInner, bottomInner, bottomOutline, topOutline];
        SVGKSource *source = [SVGKSourceString sourceFromContentsOfString:svgString];
        return [SVGKImage imageWithSource:source].UIImage;
        
    }

    if (markerLevel > 37 && markerLevel <= 62) {
      // svg with level 50
      NSString *svgSrc = @"<svg xmlns='http://www.w3.org/2000/svg' width='25' height='68' viewBox='0 0 210 300'><g id='_100' data-name='100'><path class='cls-1' fill='%1$@' d='M194.67,81c-10.43-38.6-46.48-67.34-89.52-67.44S25.93,42,15.33,80.56a88.09,88.09,0,0,0-3.14,23.14c0,13.62,2.24,28.63,6.39,44.11l172.55.38c4.21-15.46,6.55-30.47,6.58-44.09A87.76,87.76,0,0,0,194.67,81Z'/><path class='cls-2' fill='%2$@' d='M18.73,148a281.12,281.12,0,0,0,28.59,67.24c15.93,27.23,36,51.54,57.68,67.25,21.65-15.71,41.76-40,57.68-67.25A281.12,281.12,0,0,0,191.27,148Z'/><path class='cls-3' fill='%3$@' d='M207.64,80.76C197.46,34.77,155.43,0,105,0S12.54,34.77,2.36,80.76A99.59,99.59,0,0,0,0,102.31C0,116.64,2.26,132.12,6.38,148H18.73c-4.19-15.47-6.49-30.48-6.49-44.1a88.07,88.07,0,0,1,3.09-23.14C25.84,42.18,62,13.51,105,13.51s79.16,28.67,89.67,67.25a88.07,88.07,0,0,1,3.09,23.14c0,13.62-2.31,28.63-6.49,44.1h12.35c4.12-15.88,6.38-31.36,6.38-45.69A99.59,99.59,0,0,0,207.64,80.76Z'/><path fill='%4$@' class='cls-4' d='M191.27,148a281.12,281.12,0,0,1-28.59,67.24c-15.92,27.23-36,51.54-57.68,67.25-21.65-15.71-41.75-40-57.68-67.25A281.12,281.12,0,0,1,18.73,148H6.38c5.79,22.32,15.26,45.43,27.33,67.24,18.83,34,44,64.94,71.29,84.76,27.32-19.82,52.46-50.71,71.29-84.76,12.07-21.81,21.54-44.92,27.33-67.24Z'/></g></svg>";
      NSString *svgString = [NSString stringWithFormat:svgSrc, topInner, bottomInner, topOutline, bottomOutline];
      SVGKSource *source = [SVGKSourceString sourceFromContentsOfString:svgString];
      return [SVGKImage imageWithSource:source].UIImage;
    }

    if (markerLevel > 62 && markerLevel <= 88) {
      // svg with level 75
      NSString *svgSrc = @"<svg xmlns='http://www.w3.org/2000/svg' width='25' height='68' viewBox='0 0 210 300'><g id='_100' data-name='100'><path class='cls-1' fill='%1$@' d='M105,13.51c-43,0-79.16,28.67-89.67,67.25H194.67C184.16,42.18,148,13.51,105,13.51Z'/><path fill='%2$@' class='cls-2' d='M194.67,80.76H15.33a88.07,88.07,0,0,0-3.09,23.14c0,13.62,2.3,28.63,6.49,44.1a281.12,281.12,0,0,0,28.59,67.24c15.93,27.23,36,51.54,57.68,67.25,21.65-15.71,41.76-40,57.68-67.25A281.12,281.12,0,0,0,191.27,148c4.18-15.47,6.49-30.48,6.49-44.1A88.07,88.07,0,0,0,194.67,80.76Z'/><path fill='%3$@' class='cls-3' d='M105,13.51c43,0,79.16,28.67,89.67,67.25h13C197.46,34.77,155.43,0,105,0S12.54,34.77,2.36,80.76h13C25.84,42.18,62,13.51,105,13.51Z'/><path fill='%4$@' class='cls-4' d='M207.64,80.76h-13a88.07,88.07,0,0,1,3.09,23.14c0,13.62-2.31,28.63-6.49,44.1a281.12,281.12,0,0,1-28.59,67.24c-15.92,27.23-36,51.54-57.68,67.25-21.65-15.71-41.75-40-57.68-67.25A281.12,281.12,0,0,1,18.73,148c-4.19-15.47-6.49-30.48-6.49-44.1a88.07,88.07,0,0,1,3.09-23.14h-13A99.59,99.59,0,0,0,0,102.31C0,116.64,2.26,132.12,6.38,148c5.79,22.32,15.26,45.43,27.33,67.24,18.83,34,44,64.94,71.29,84.76,27.32-19.82,52.46-50.71,71.29-84.76,12.07-21.81,21.54-44.92,27.33-67.24,4.12-15.88,6.38-31.36,6.38-45.69A99.59,99.59,0,0,0,207.64,80.76Z'/></g></svg>";
      NSString *svgString = [NSString stringWithFormat:svgSrc, topInner, bottomInner, topOutline, bottomOutline];
      SVGKSource *source = [SVGKSourceString sourceFromContentsOfString:svgString];
      return [SVGKImage imageWithSource:source].UIImage;
    }
    // svg with level 100
    NSString *svgSrc = @"<svg xmlns='http://www.w3.org/2000/svg' width='25' height='68' viewBox='0 0 210 300'><g id='_100' data-name='100'><path class='cls-1' fill='%1$@' d='M194.67,80.76C184.16,42.18,148,13.51,105,13.51S25.84,42.18,15.33,80.76a88.07,88.07,0,0,0-3.09,23.14c0,13.62,2.3,28.63,6.49,44.1a281.12,281.12,0,0,0,28.59,67.24c15.93,27.23,36,51.54,57.68,67.25,21.65-15.71,41.76-40,57.68-67.25A281.12,281.12,0,0,0,191.27,148c4.18-15.47,6.49-30.48,6.49-44.1A88.07,88.07,0,0,0,194.67,80.76Z'/><path fill='%2$@' class='cls-2' d='M207.64,80.76C197.46,34.77,155.43,0,105,0S12.54,34.77,2.36,80.76A99.59,99.59,0,0,0,0,102.31C0,116.64,2.26,132.12,6.38,148c5.79,22.32,15.26,45.43,27.33,67.24,18.83,34,44,64.94,71.29,84.76,27.32-19.82,52.46-50.71,71.29-84.76,12.07-21.81,21.54-44.92,27.33-67.24,4.12-15.88,6.38-31.36,6.38-45.69A99.59,99.59,0,0,0,207.64,80.76ZM191.27,148a281.12,281.12,0,0,1-28.59,67.24c-15.92,27.23-36,51.54-57.68,67.25-21.65-15.71-41.75-40-57.68-67.25A281.12,281.12,0,0,1,18.73,148c-4.19-15.47-6.49-30.48-6.49-44.1a88.07,88.07,0,0,1,3.09-23.14C25.84,42.18,62,13.51,105,13.51s79.16,28.67,89.67,67.25a88.07,88.07,0,0,1,3.09,23.14C197.76,117.52,195.45,132.53,191.27,148Z'/></g></svg>";
    NSString *svgString = [NSString stringWithFormat:svgSrc, bottomInner, bottomOutline];
    SVGKSource *source = [SVGKSourceString sourceFromContentsOfString:svgString];
    return [SVGKImage imageWithSource:source].UIImage;
}

- (UIImage *)getSharedUIImage:(NSString *)imageSrc {

  UIImage* cachedImage = dict[imageSrc];

  CGImageRef cgref = [cachedImage CGImage];
  CIImage *cim = [cachedImage CIImage];

  if (cim == nil && cgref == NULL) {
    UIImage *newImage;
    if ([imageSrc hasPrefix:@"http://"] || [imageSrc hasPrefix:@"https://"]){
      NSURL *url = [NSURL URLWithString:imageSrc];
      NSData *data = [NSData dataWithContentsOfURL:url];
      newImage = [UIImage imageWithData:data scale:[UIScreen mainScreen].scale];
    } else if([imageSrc hasPrefix:@"<svg"]) {
      SVGKSource *source = [SVGKSourceString sourceFromContentsOfString:imageSrc];
      SVGKImage *img = [SVGKImage imageWithSource:source];
      newImage = img.UIImage;
    } else if([imageSrc hasPrefix:@"useCfMarker"]) {
      NSString *jsonString = [imageSrc componentsSeparatedByString:@"JSON:"][1];
      NSData* jsonData = [jsonString dataUsingEncoding:NSUTF8StringEncoding];
      NSError *error;
      id jsonObject = [NSJSONSerialization JSONObjectWithData:jsonData options:0 error:&error];
      if (error) {
        NSLog(@"Error parsing JSON: %@", error);
      } else{
        NSDictionary *jsonDictionary = (NSDictionary *)jsonObject;
        NSString *topOutline = jsonDictionary[@"topOutline"];
        NSString *bottomOutline = jsonDictionary[@"bottomOutline"];
        NSString *topInner = jsonDictionary[@"topInner"];
        NSString *bottomInner = jsonDictionary[@"bottomInner"];
        NSString *level = jsonDictionary[@"level"];
          newImage = [self getCFMarker:topOutline bottomOutline:bottomOutline topInner:topInner bottomInner:bottomInner level:level];
      }
    }
    dict[imageSrc] = newImage;
    return newImage;
  } else {
    return cachedImage;
  }
}

- (id)init {
  self = [super init];
  if (self) {
    dict = [[NSMutableDictionary alloc] init];
  }
  return self;
}

@end
