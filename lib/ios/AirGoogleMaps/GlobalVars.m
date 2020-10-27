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

- (UIImage *) getCFMarker:(NSString *)topOutline bottomOutline:(NSString *)bottomOutline topInner:(NSString *)topInner bottomInner:(NSString *)bottomInner level:(NSString *)level promotedColor:(NSString *) promotedColor {
    float markerLevel = [level floatValue];

    if (markerLevel == 0) {
      // svg with level 0
      NSString *svgSrc = @"<svg xmlns='http://www.w3.org/2000/svg' width='25' height='68' viewBox='0 0 210 300'><path fill='%1$@' d='M186.873,99.819C177.278,64.59,144.263,38.412,105,38.412S32.722,64.59,23.127,99.819          c-1.876,6.884-2.824,13.991-2.824,21.126c0,12.437,2.102,26.143,5.929,40.266c5.992,21.511,14.767,42.153,26.103,61.396          c14.546,24.859,32.87,47.06,52.666,61.402c19.766-14.343,38.132-36.521,52.67-61.402c11.332-19.242,20.103-39.885,26.095-61.396          c3.819-14.123,5.932-27.829,5.932-40.266C189.696,113.811,188.753,106.703,186.873,99.819z'/><path fill='%2$@' d='M198.715,99.819C189.427,57.826,151.048,26.081,105,26.081c-46.045,0-84.424,31.746-93.719,73.739          c-1.431,6.46-2.153,13.059-2.153,19.677c0,13.08,2.063,27.218,5.827,41.715c5.284,20.38,13.932,41.479,24.953,61.396          C57.102,253.652,80.081,281.904,105,300c24.941-18.096,47.898-46.303,65.088-77.394c11.025-19.916,19.669-41.016,24.956-61.396          c3.761-14.497,5.828-28.635,5.828-41.715C200.872,112.878,200.152,106.279,198.715,99.819z M157.625,222.606          c-14.531,24.859-32.866,47.06-52.663,61.402c-19.766-14.343-38.12-36.521-52.665-61.402          c-11.321-19.242-20.085-39.885-26.065-61.396c-3.827-14.123-5.929-27.829-5.929-40.266c0-7.135,0.948-14.242,2.824-21.126          C32.722,64.59,65.737,38.412,105,38.412s72.278,26.178,81.873,61.407c1.88,6.884,2.823,13.991,2.823,21.126          c0,12.437-2.112,26.143-5.932,40.266c-5.992,21.511-14.763,42.153-26.095,61.396H157.625z'/>";
        
        
      if (promotedColor) {
          svgSrc = [svgSrc stringByAppendingString: @"<path fill='%3$@' d='M153.79,0c26.118,0,47.299,21.178,47.299,47.303c0,26.127-21.181,47.302-47.299,47.302        c-26.126,0-47.308-21.175-47.308-47.302C106.482,21.178,127.664,0,153.79,0z'/><path fill='#FFFFFF' d='M146.809,69.018c0-4.655,3.102-7.756,7.752-7.756c4.652,0,7.753,3.101,7.753,7.756 c0,4.651-3.101,7.752-7.753,7.752C149.91,76.77,146.809,73.669,146.809,69.018z M149.139,54.284l-2.33-36.447h13.184l-1.551,36.447 H149.139z'/>"];
      }
        

      svgSrc = [svgSrc stringByAppendingString:@"</svg>"];
      
      NSString *svgString = [svgSrc stringByReplacingOccurrencesOfString:@"%1$@" withString:topInner];
      svgString = [svgString stringByReplacingOccurrencesOfString:@"%2$@" withString:topOutline];
      svgString = promotedColor ? [svgString stringByReplacingOccurrencesOfString:@"%3$@" withString:promotedColor] : svgString;
      
      SVGKSource *source = [SVGKSourceString sourceFromContentsOfString:svgString];
      return [SVGKImage imageWithSource:source].UIImage;
    }

    if (markerLevel > 0 && markerLevel <= 37) {
      // svg with level 25
      NSString *svgSrc = @"<svg xmlns='http://www.w3.org/2000/svg' width='25' height='68' viewBox='0 0 210 300'><path fill='%1$@' d='M186.873,99.819C177.278,64.59,144.263,38.412,105,38.412S32.722,64.59,23.127,99.819          c-1.876,6.884-2.824,13.991-2.824,21.126c0,12.437,2.102,26.143,5.929,40.266c5.992,21.511,14.767,42.153,26.103,61.396H157.67 c11.332-19.242,20.103-39.885,26.095-61.396c3.819-14.123,5.932-27.829,5.932-40.266 C189.696,113.811,188.753,106.703,186.873,99.819z'/><path fill='%2$@' d='M105,284.009c19.766-14.343,38.132-36.521,52.67-61.402H52.334C66.88,247.466,85.233,269.666,105,284.009z'/><path fill='%3$@' d='M105,284.009c-19.767-14.343-38.12-36.521-52.666-61.402H39.908C57.102,253.652,80.081,281.904,105,300 c24.941-18.096,47.898-46.303,65.088-77.394H157.67C143.132,247.466,124.766,269.666,105,284.009z'/> <path fill='%4$@' d='M198.715,99.819C189.427,57.826,151.048,26.081,105,26.081c-46.045,0-84.424,31.746-93.719,73.739 c-1.431,6.46-2.153,13.059-2.153,19.677c0,13.08,2.063,27.218,5.827,41.715c5.284,20.38,13.932,41.479,24.953,61.396h12.426 c-11.336-19.242-20.11-39.885-26.103-61.396c-3.827-14.123-5.929-27.829-5.929-40.266c0-7.135,0.948-14.242,2.824-21.126 C32.722,64.59,65.737,38.412,105,38.412s72.278,26.178,81.873,61.407c1.88,6.884,2.823,13.991,2.823,21.126 c0,12.437-2.112,26.143-5.932,40.266c-5.992,21.511-14.763,42.153-26.095,61.396h12.418c11.025-19.916,19.669-41.016,24.956-61.396 c3.761-14.497,5.828-28.635,5.828-41.715C200.872,112.878,200.152,106.279,198.715,99.819z'/>";
        
      if (promotedColor) {
          svgSrc = [svgSrc stringByAppendingString: @"<path fill='%5$@' d='M153.79,0c26.118,0,47.299,21.178,47.299,47.303c0,26.127-21.181,47.302-47.299,47.302 c-26.126,0-47.308-21.175-47.308-47.302C106.482,21.178,127.664,0,153.79,0z'/><path fill='#FFFFFF' d='M146.809,69.018c0-4.655,3.102-7.756,7.752-7.756c4.652,0,7.753,3.101,7.753,7.756 c0,4.651-3.101,7.752-7.753,7.752C149.91,76.77,146.809,73.669,146.809,69.018z M149.139,54.284l-2.33-36.447h13.184l-1.551,36.447 H149.139z'/>"];
      }
      
      svgSrc = [svgSrc stringByAppendingString:@"</svg>"];
      
      
      NSString *svgString = [svgSrc stringByReplacingOccurrencesOfString:@"%1$@" withString:topInner];
      svgString = [svgString stringByReplacingOccurrencesOfString:@"%2$@" withString:bottomInner];
      svgString = [svgString stringByReplacingOccurrencesOfString:@"%3$@" withString:bottomOutline];
      svgString = [svgString stringByReplacingOccurrencesOfString:@"%4$@" withString:topOutline];
      svgString = promotedColor ? [svgString stringByReplacingOccurrencesOfString:@"%5$@" withString:promotedColor] : svgString;
      
      SVGKSource *source = [SVGKSourceString sourceFromContentsOfString:svgString];
      return [SVGKImage imageWithSource:source].UIImage;
        
    }

    if (markerLevel > 37 && markerLevel <= 62) {
      // svg with level 50
      NSString *svgSrc = @"<svg xmlns='http://www.w3.org/2000/svg' width='25' height='68' viewBox='0 0 210 300'><path fill='%1$@' d='M186.873,100.037c-9.52-35.244-42.438-61.486-81.734-61.576c-39.3-0.094-72.335,25.969-82.012,61.175 c-1.892,6.884-2.854,13.987-2.869,21.13c0,12.433,2.045,26.139,5.835,40.273l157.552,0.348c3.843-14.119,5.978-27.821,6.007-40.258 C189.667,114.006,188.738,106.912,186.873,100.037z'/><path fill='%2$@' d='M26.231,161.211c5.992,21.511,14.767,42.153,26.103,61.396c14.546,24.859,32.87,47.06,52.666,61.402 c19.766-14.343,38.132-36.521,52.67-61.402c11.332-19.242,20.103-39.885,26.095-61.396H26.231z'/><path fill='%3$@' d='M198.715,99.819C189.427,57.826,151.048,26.081,105,26.081c-46.045,0-84.424,31.746-93.719,73.739 c-1.431,6.46-2.153,13.059-2.153,19.677c0,13.08,2.063,27.218,5.827,41.715h11.276c-3.827-14.123-5.929-27.829-5.929-40.266 c0-7.135,0.948-14.242,2.824-21.126C32.722,64.59,65.737,38.412,105,38.412s72.278,26.178,81.873,61.407 c1.88,6.884,2.823,13.991,2.823,21.126c0,12.437-2.112,26.143-5.932,40.266h11.279c3.761-14.497,5.828-28.635,5.828-41.715 C200.872,112.878,200.152,106.279,198.715,99.819z'/><path fill='%4$@' d='M183.765,161.211c-5.992,21.511-14.763,42.153-26.095,61.396c-14.538,24.859-32.874,47.06-52.67,61.402 c-19.767-14.343-38.12-36.521-52.666-61.402c-11.336-19.242-20.11-39.885-26.103-61.396H14.955 c5.284,20.38,13.932,41.479,24.953,61.396C57.102,253.652,80.081,281.904,105,300c24.941-18.096,47.898-46.303,65.088-77.394 c11.025-19.916,19.669-41.016,24.956-61.396H183.765z'/>";
        
      if (promotedColor) {
          svgSrc = [svgSrc stringByAppendingString: @"<path fill='%5$@' d='M153.79,0c26.118,0,47.299,21.178,47.299,47.303c0,26.127-21.181,47.302-47.299,47.302 c-26.126,0-47.308-21.175-47.308-47.302C106.482,21.178,127.664,0,153.79,0z'/><path fill='#FFFFFF' d='M146.809,69.018c0-4.655,3.102-7.756,7.752-7.756c4.652,0,7.753,3.101,7.753,7.756 c0,4.651-3.101,7.752-7.753,7.752C149.91,76.77,146.809,73.669,146.809,69.018z M149.139,54.284l-2.33-36.447h13.184l-1.551,36.447 H149.139z'/>"];
      }
        
      svgSrc = [svgSrc stringByAppendingString:@"</svg>"];
          
      NSString *svgString = [svgSrc stringByReplacingOccurrencesOfString:@"%1$@" withString:topInner];
      svgString = [svgString stringByReplacingOccurrencesOfString:@"%2$@" withString:bottomInner];
      svgString = [svgString stringByReplacingOccurrencesOfString:@"%3$@" withString:topOutline];
      svgString = [svgString stringByReplacingOccurrencesOfString:@"%4$@" withString:bottomOutline];
      svgString = promotedColor ? [svgString stringByReplacingOccurrencesOfString:@"%5$@" withString:promotedColor] : svgString;
      SVGKSource *source = [SVGKSourceString sourceFromContentsOfString:svgString];
      return [SVGKImage imageWithSource:source].UIImage;
    }

    if (markerLevel > 62 && markerLevel <= 88) {
      // svg with level 75
      NSString *svgSrc = @"<svg xmlns='http://www.w3.org/2000/svg' width='25' height='68' viewBox='0 0 210 300'><path fill='%1$@' d='M104.998,38.416c-39.262,0-72.276,26.179-81.87,61.405h163.745 C177.273,64.595,144.258,38.416,104.998,38.416z'/><path fill='%2$@' d='M186.873,99.821H23.128c-1.877,6.885-2.824,13.99-2.824,21.125c0,12.438,2.103,26.145,5.93,40.267    c5.991,21.512,14.765,42.15,26.101,61.392c14.545,24.86,32.868,47.062,52.664,61.403c19.765-14.341,38.133-36.519,52.67-61.403          c11.336-19.241,20.105-39.88,26.096-61.392c3.816-14.122,5.93-27.828,5.93-40.267C189.693,113.812,188.75,106.706,186.873,99.821z'/><path fill='%3$@' d='M104.998,38.416c39.26,0,72.275,26.179,81.875,61.405h11.873c-9.32-41.991-47.699-73.738-93.748-73.738          c-46.042,0-84.421,31.747-93.715,73.738h11.871C32.722,64.595,65.736,38.416,104.998,38.416z'/><path fill='%4$@' d='M198.715,99.821h-11.867c1.871,6.885,2.816,13.99,2.816,21.125c0,12.438-2.109,26.145-5.924,40.267          c-5.99,21.512-14.768,42.15-26.104,61.392c-14.535,24.86-32.874,47.062-52.664,61.403c-19.771-14.341-38.118-36.519-52.664-61.403          c-11.326-19.241-20.094-39.88-26.075-61.392c-3.827-14.122-5.93-27.828-5.93-40.267c0-7.135,0.947-14.24,2.824-21.125H11.256          c-1.424,6.461-2.133,13.059-2.127,19.677c0,13.079,2.063,27.219,5.827,41.715c5.284,20.381,13.933,41.48,24.955,61.392          C57.103,253.65,80.079,281.905,104.998,300c24.944-18.095,47.896-46.301,65.09-77.396c11.025-19.911,19.668-41.011,24.951-61.392          c3.76-14.496,5.832-28.636,5.832-41.715C200.871,112.88,200.152,106.282,198.715,99.821z'/>";
      
      if (promotedColor) {
        svgSrc = [svgSrc stringByAppendingString: @"<path fill='%5$@' d='M153.79,0c26.118,0,47.299,21.178,47.299,47.303c0,26.127-21.181,47.302-47.299,47.302 c-26.126,0-47.308-21.175-47.308-47.302C106.482,21.178,127.664,0,153.79,0z'/><path fill='#FFFFFF' d='M146.809,69.018c0-4.655,3.102-7.756,7.752-7.756c4.652,0,7.753,3.101,7.753,7.756 c0,4.651-3.101,7.752-7.753,7.752C149.91,76.77,146.809,73.669,146.809,69.018z M149.139,54.284l-2.33-36.447h13.184l-1.551,36.447 H149.139z'/>"];
      }
          
      svgSrc = [svgSrc stringByAppendingString:@"</svg>"];
          
      NSString *svgString = [svgSrc stringByReplacingOccurrencesOfString:@"%1$@" withString:topInner];
      svgString = [svgString stringByReplacingOccurrencesOfString:@"%2$@" withString:bottomInner];
      svgString = [svgString stringByReplacingOccurrencesOfString:@"%3$@" withString:topOutline];
      svgString = [svgString stringByReplacingOccurrencesOfString:@"%4$@" withString:bottomOutline];
      svgString = promotedColor ? [svgString stringByReplacingOccurrencesOfString:@"%5$@" withString:promotedColor] : svgString;
        
      SVGKSource *source = [SVGKSourceString sourceFromContentsOfString:svgString];
      return [SVGKImage imageWithSource:source].UIImage;
    }
    // svg with level 100
    NSString *svgSrc = @"<svg xmlns='http://www.w3.org/2000/svg' width='25' height='68' viewBox='0 0 210 300'><path fill='%1$@' d='M186.875,99.815c-9.595-35.23-42.611-61.408-81.874-61.408s-72.28,26.178-81.875,61.408      c-1.877,6.885-2.824,13.992-2.824,21.126c0,12.438,2.102,26.144,5.93,40.267c5.991,21.512,14.766,42.155,26.103,61.396      c14.546,24.86,32.87,47.061,52.667,61.404c19.766-14.344,38.132-36.521,52.67-61.404c11.332-19.241,20.104-39.885,26.096-61.396      c3.82-14.123,5.932-27.829,5.932-40.267C189.698,113.808,188.755,106.7,186.875,99.815z'/><path fill='%2$@' d='M198.717,99.815c-9.288-41.994-47.666-73.74-93.716-73.74c-46.047,0-84.425,31.746-93.721,73.74      c-1.43,6.461-2.153,13.06-2.153,19.677c0,13.081,2.064,27.219,5.827,41.716c5.284,20.381,13.931,41.479,24.954,61.396      c17.193,31.046,40.173,59.3,65.093,77.396c24.941-18.096,47.899-46.304,65.088-77.396c11.026-19.917,19.669-41.016,24.958-61.396      c3.76-14.497,5.826-28.635,5.826-41.716C200.873,112.875,200.154,106.276,198.717,99.815z M183.767,161.208      c-5.992,21.512-14.764,42.155-26.096,61.396c-14.538,24.86-32.874,47.061-52.67,61.404c-19.767-14.344-38.121-36.521-52.667-61.404      c-11.337-19.241-20.111-39.885-26.103-61.396c-3.828-14.123-5.93-27.829-5.93-40.267c0-7.134,0.947-14.241,2.824-21.126      c9.595-35.23,42.612-61.408,81.875-61.408s72.279,26.178,81.874,61.408c1.88,6.885,2.823,13.992,2.823,21.126 C189.698,133.379,187.587,147.085,183.767,161.208z'/>";
    
    if (promotedColor) {
      svgSrc = [svgSrc stringByAppendingString: @"<path fill='%3$@' d='M153.79,0c26.118,0,47.299,21.178,47.299,47.303c0,26.127-21.181,47.302-47.299,47.302 c-26.126,0-47.308-21.175-47.308-47.302C106.482,21.178,127.664,0,153.79,0z'/><path fill='#FFFFFF' d='M146.809,69.018c0-4.655,3.102-7.756,7.752-7.756c4.652,0,7.753,3.101,7.753,7.756 c0,4.651-3.101,7.752-7.753,7.752C149.91,76.77,146.809,73.669,146.809,69.018z M149.139,54.284l-2.33-36.447h13.184l-1.551,36.447 H149.139z'/>"];
    }
        
    svgSrc = [svgSrc stringByAppendingString:@"</svg>"];
        
    NSString *svgString = [svgSrc stringByReplacingOccurrencesOfString:@"%1$@" withString:bottomInner];
    svgString = [svgString stringByReplacingOccurrencesOfString:@"%2$@" withString:bottomOutline];
    svgString = promotedColor ? [svgString stringByReplacingOccurrencesOfString:@"%3$@" withString:promotedColor] : svgString;
      
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
        NSDictionary *promoted = (NSDictionary *)jsonDictionary[@"promoted"];
        NSString *promotedColor = promoted[@"color"];
        BOOL isPromoted = [promoted[@"isPromoted"] boolValue];
        newImage = [self getCFMarker:topOutline bottomOutline:bottomOutline topInner:topInner bottomInner:bottomInner level:level promotedColor:(isPromoted ? promotedColor : nil)];
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
