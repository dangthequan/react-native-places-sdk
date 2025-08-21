#import "GooglePlacesSdk.h"
#import <React/RCTConvert.h>
#import <GooglePlaces/GooglePlaces.h>

@implementation RCTGooglePlacesSdk

RCT_EXPORT_MODULE(GooglePlacesSdk);
+(NSDictionary *) placeProperties {
    return @{
        @"name": GMSPlacePropertyName,
        @"formatted_address": GMSPlacePropertyFormattedAddress,
        @"coordinate": GMSPlacePropertyCoordinate,
        @"phone_number": GMSPlacePropertyPhoneNumber,
        @"website": GMSPlacePropertyWebsite,
        @"rating": GMSPlacePropertyRating,
        @"price_level": GMSPlacePropertyPriceLevel,
        @"types": GMSPlacePropertyTypes,
        @"photos": GMSPlacePropertyPhotos,
        @"opening_hours": GMSPlacePropertyOpeningHours,
        @"icon": GMSPlacePropertyIconImageURL
    };
}

RCT_EXPORT_METHOD(initialize:(NSString *)apiKey
                 withResolver:(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)
{
    dispatch_async(dispatch_get_main_queue(), ^{
        @try {
            [GMSPlacesClient provideAPIKey:apiKey];
            resolve(nil);
        } @catch (NSException *exception) {
            reject(@"initialize_error", exception.reason, nil);
        }
    });
}

RCT_REMAP_METHOD(getAPIKey, resolver:(RCTPromiseResolveBlock)resolve withRejecter:(RCTPromiseRejectBlock)reject)
{
  NSString *apiKey = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"GoogleMapsApiKey"];
  resolve(apiKey);
}

RCT_EXPORT_METHOD(autoComplete:(NSString *)query
                 withOptions:(NSDictionary *)options
                 withResolver:(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)
{
    dispatch_async(dispatch_get_main_queue(), ^{
        NSMutableDictionary *response = [NSMutableDictionary dictionary];
        GMSPlacesClient *placesClient = [GMSPlacesClient sharedClient];
        if (!placesClient) {
            [response setValue:@(400) forKey:@"status"];
            [response setValue:@"Places client is not initialized" forKey:@"message"];
            resolve(response);
            return;
        }
        
        if (!query) {
            [response setValue:@(400) forKey:@"status"];
            [response setValue:@"Missing query parameter" forKey:@"message"];
            resolve(response);
            return;
        }
        
        // Create the autocomplete request
        GMSAutocompleteRequest *request = [[GMSAutocompleteRequest alloc] initWithQuery:query];
        
        // Create and configure filter if needed
        GMSAutocompleteFilter *filter = [[GMSAutocompleteFilter alloc] init];
        
        if (options) {
            // Handle country restrictions
            if (options[@"countries"]) {
                NSArray *countries = options[@"countries"];
                filter.countries = countries;
            }
            
            // Handle type restrictions
            if (options[@"types"]) {
                NSArray *types = options[@"types"];
                filter.types = types;
            }
            
            // Handle region code
            if (options[@"region_code"]) {
                NSString *regionCode = options[@"region_code"];
                filter.regionCode = regionCode;
            }
            
            // Handle location bias
            if (options[@"location"] && options[@"radius"]) {
                NSDictionary *location = options[@"location"];
                double latitude = [RCTConvert double:location[@"latitude"]];
                double longitude = [RCTConvert double:location[@"longitude"]];
                double radius = [RCTConvert double:options[@"radius"]];
                
                CLLocationCoordinate2D center = CLLocationCoordinate2DMake(latitude, longitude);
                
                filter.locationBias = GMSPlaceCircularLocationOption(center, radius);
            }
        }

        // Assign filter if any filter options are set
        request.filter = filter;
        
        [placesClient fetchAutocompleteSuggestionsFromRequest:request callback:^(NSArray<GMSAutocompleteSuggestion *> *results, NSError *error) {
            if (error != nil) {
                NSLog(@"GooglePlacesSdk >> autocomplete >> error: ", error.debugDescription);
                [response setValue:@(error.code) forKey:@"status"];
                [response setValue:error.localizedDescription forKey:@"message"];
                resolve(response);
                return;
            }
            
            NSMutableArray *places = [NSMutableArray array];
            if (results && [results count] > 0) {
                for (GMSAutocompleteSuggestion *suggestion in results) {
                    if (suggestion.placeSuggestion) {
                        NSMutableDictionary *place = [NSMutableDictionary dictionary];
                        place[@"place_id"] = suggestion.placeSuggestion.placeID;
                        place[@"description"] = suggestion.placeSuggestion.attributedFullText.string;
                        place[@"types"] = suggestion.placeSuggestion.types;
                        [places addObject:place];
                    }
                }
            }
            [response setValue:@(200) forKey:@"status"];
            [response setValue:places forKey:@"result"];
            
            resolve(response);
        }];
    });
}

RCT_EXPORT_METHOD(fetchPlace:(NSString *)placeId
                  withFields:(NSArray *)fields
                  withResolver:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)
{
    dispatch_async(dispatch_get_main_queue(), ^{
        NSMutableDictionary *response = [NSMutableDictionary dictionary];
        GMSPlacesClient *placesClient = [GMSPlacesClient sharedClient];
        if (!placesClient) {
            [response setValue:@(400) forKey:@"status"];
            [response setValue:@"Places client is not initialized" forKey:@"message"];
            resolve(response);
            return;
        }
        
        if (!placeId) {
            [response setValue:@(400) forKey:@"status"];
            [response setValue:@"Missing placeId parameter" forKey:@"message"];
            resolve(response);
            return;
        }
        
        NSMutableArray<NSString *> *placeFields = [NSMutableArray array];
        if (fields != nil && [fields count] > 0) {
            NSDictionary *propertiesMap = [RCTGooglePlacesSdk placeProperties];
            for (NSString *field in fields) {
                GMSPlaceProperty property = [propertiesMap objectForKey:field];
                if (property != nil) {
                    [placeFields addObject:property];
                }
            }
        }
        else {
            [placeFields addObjectsFromArray:@[GMSPlacePropertyName, GMSPlacePropertyFormattedAddress, GMSPlacePropertyCoordinate]];
        }
        
        GMSFetchPlaceRequest *request = [[GMSFetchPlaceRequest alloc] initWithPlaceID:placeId placeProperties:placeFields sessionToken:nil];
        
        [placesClient fetchPlaceWithRequest:request callback:^(GMSPlace * _Nullable place, NSError * _Nullable error) {
            if (error != nil) {
                NSLog(@"GooglePlacesSdk >> autocomplete >> error: ", error.debugDescription);
                [response setValue:@(error.code) forKey:@"status"];
                [response setValue:error.localizedDescription forKey:@"message"];
                resolve(response);
                return;
            }
            
            NSDictionary *placeDetails = [self placeToDict:place];
            [response setValue:@(200) forKey:@"status"];
            [response setValue:placeDetails forKey:@"result"];
            resolve(response);
        }];
    });
}

RCT_EXPORT_METHOD(nearbySearch:(NSDictionary *)options
                 withResolver:(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)
{
    dispatch_async(dispatch_get_main_queue(), ^{
        NSMutableDictionary *response = [NSMutableDictionary dictionary];
        // Get the Places client
        GMSPlacesClient *placesClient = [GMSPlacesClient sharedClient];
        if (!placesClient) {
            [response setValue:@(400) forKey:@"status"];
            [response setValue:@"Places client is not initialized" forKey:@"message"];
            resolve(response);
            return;
        }
        
        // Validate required parameters
        if (!options[@"location"]) {
            [response setValue:@(400) forKey:@"status"];
            [response setValue:@"Missing location parameter" forKey:@"message"];
            resolve(response);
            return;
        }
        
        NSDictionary *location = options[@"location"];
        double latitude = [RCTConvert double:location[@"latitude"]];
        double longitude = [RCTConvert double:location[@"longitude"]];
        CLLocationCoordinate2D coordinate = CLLocationCoordinate2DMake(latitude, longitude);
        
        // Default radius is 1500 meters if not specified
        double radius = 1500.0;
        if (options[@"radius"]) {
            radius = [RCTConvert double:options[@"radius"]];
        }
        
        // Create the location restriction (circular)
        id<GMSPlaceLocationRestriction> locationRestriction = GMSPlaceCircularLocationOption(coordinate, radius);
        
        // Determine which place properties to return
        NSMutableArray *placeProperties = [NSMutableArray array];
        if (options[@"fields"] && [options[@"fields"] isKindOfClass:[NSArray class]] && [options[@"fields"] count] > 0) {
            NSDictionary *propertiesMap = [RCTGooglePlacesSdk placeProperties];
            for (NSString *field in options[@"fields"]) {
                GMSPlaceProperty property = [propertiesMap objectForKey:field];
                if (property != nil) {
                    [placeProperties addObject:property];
                }
            }
        }
        else {
            [placeProperties addObjectsFromArray:@[GMSPlacePropertyName, GMSPlacePropertyCoordinate, GMSPlacePropertyFormattedAddress]];
        }
        
        // Create the request
        GMSPlaceSearchNearbyRequest *request = [[GMSPlaceSearchNearbyRequest alloc]
                                                initWithLocationRestriction:locationRestriction
                                                placeProperties:placeProperties];
        
        // Set includedTypes if specified
        if (options[@"types"] && [options[@"types"] isKindOfClass:[NSArray class]]) {
            request.includedTypes = [NSMutableArray arrayWithArray:options[@"types"]];
        } else if (options[@"type"] && [options[@"type"] isKindOfClass:[NSString class]]) {
            request.includedTypes = [NSMutableArray arrayWithObject:options[@"type"]];
        }
        
        // Add keyword if specified
        if (options[@"region_code"]) {
            request.regionCode = options[@"region_code"];
        }
        
        if (options[@"max_result_count"]) {
            request.maxResultCount = [RCTConvert NSInteger:options[@"max_result_count"]];
        }
        
        [placesClient searchNearbyWithRequest:request
                                     callback:^(NSArray<GMSPlace *> *_Nullable places, NSError *_Nullable error) {
            if (error) {
                [response setValue:@(error.code) forKey:@"status"];
                [response setValue:error.localizedDescription forKey:@"message"];
                return;
            }
            
            NSMutableArray *results = [NSMutableArray array];
            if (places && [places count] > 0) {
                for (GMSPlace *place in places) {
                    [results addObject:[self placeToDict:place]];
                }
            }
            [response setValue: @(200) forKey:@"status"];
            [response setValue:results forKey:@"result"];
            resolve(response);
        }
        ];
    });
}

// Helper method to parse GMSPlace to a dictionary
- (NSDictionary *)placeToDict:(GMSPlace *)place {
  NSMutableDictionary *placeDict = [NSMutableDictionary dictionary];
  
  // Basic place information
  placeDict[@"place_id"] = place.placeID;
  placeDict[@"name"] = place.name;
  
  if (place.formattedAddress) {
    placeDict[@"formatted_address"] = place.formattedAddress;
  }
  
  if (place.coordinate.latitude != 0 || place.coordinate.longitude != 0) {
    placeDict[@"geometry"] = @{
        @"location" : @{
            @"lat": @(place.coordinate.latitude),
            @"lng": @(place.coordinate.longitude)
        }
    };
  }
  
  if (place.types.count > 0) {
    placeDict[@"types"] = place.types;
  }
  
  if (place.rating > 0) {
    placeDict[@"rating"] = @(place.rating);
  }
  
  if (place.userRatingsTotal > 0) {
    placeDict[@"user_ratings_total"] = @(place.userRatingsTotal);
  }
  
  if (place.priceLevel != kGMSPlacesPriceLevelUnknown) {
    placeDict[@"price_level"] = @(place.priceLevel);
  }
  
  if (place.phoneNumber) {
    placeDict[@"phone_number"] = place.phoneNumber;
  }
  
  if (place.website) {
    placeDict[@"website"] = place.website.absoluteString;
  }
    
    if (place.iconImageURL) {
        placeDict[@"icon"] = place.iconImageURL.absoluteString;
  }
  
  return placeDict;
}
@end
