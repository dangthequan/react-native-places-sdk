# react-native-places-sdk

React Native wrapper for Google Places SDK for Android and iOS. Supports both the legacy architecture and New Architecture (Turbo Modules).

## Installation

```sh
npm install react-native-places-sdk
# or
yarn add react-native-places-sdk
```

## Requirements

### Development Environment
- Node.js >= 14.0.0
- npm >= 6.0.0 or Yarn

### iOS
- Google Places SDK for iOS
- iOS 15.0 or higher

### Android
- Google Places SDK for Android
- minSdkVersion 24

## Setup

### iOS
1. Add GooglePlaces SDK as a dependency in your Podfile:
```ruby
target 'YourApp' do
  # ...other dependencies...
  pod 'GooglePlaces', '~> 9.0.0'
  
  # Make sure this line is included to link the SDK properly
  pod 'react-native-places-sdk', :path => '../node_modules/react-native-places-sdk'
end
```

2. Run `pod install`

3. Get an API key from the [Google Cloud Console](https://console.cloud.google.com/).

4. Add your API key to your Info.plist:
```xml
<key>GooglePlacesAPIKey</key>
<string>YOUR_API_KEY</string>
```

5. For location-based features, add these permissions to your Info.plist:
```xml
<key>NSLocationWhenInUseUsageDescription</key>
<string>This app needs access to your location to show nearby places.</string>
<key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
<string>This app needs access to your location to show nearby places.</string>
```

6. If you're using the New Architecture (Turbo Modules), make sure to enable it in `ios/Podfile`:
```ruby
ENV['RCT_NEW_ARCH_ENABLED'] = '1'  # Set to '0' to disable
```

### Android
1. Add the Google Places dependency to your app/build.gradle:
```gradle
dependencies {
    // ...other dependencies...
    implementation 'com.google.android.libraries.places:places:2.6.0'
}
```

2. Get an API key from the [Google Cloud Console](https://console.cloud.google.com/).

3. Add your API key to your AndroidManifest.xml:
```xml
<application>
    <!-- ... other tags -->
    <meta-data
        android:name="com.google.android.geo.API_KEY"
        android:value="YOUR_API_KEY"/>
</application>
```

## Usage

```javascript
import GooglePlaces from 'react-native-places-sdk';

// Initialize with API key (alternative to Info.plist or AndroidManifest configuration)
await GooglePlaces.initialize('YOUR_API_KEY');

// Autocomplete search
const results = await GooglePlaces.autocomplete('New York');

// Place details
const placeDetails = await GooglePlaces.fetchPlace('PLACE_ID', ['name', 'address', 'coordinate']);


## Methods

- `initialize(apiKey)` - Initialize the SDK with your API key
- `autocomplete(query, [options])` - Search for places with given query
- `fetchPlace(placeId, [fields])` - Get details for a specific place

## Troubleshooting

### iOS Issues

#### 'RCTGooglePlacesSdk.h' file not found

If you encounter this error when building your iOS project, follow these steps:

1. Make sure you've run `pod install` in your iOS directory.
2. If the error persists, create a file named `RCTGooglePlacesSdk.h` in the `node_modules/react-native-places-sdk/ios/` directory with the following content:

```objective-c
#ifdef RCT_NEW_ARCH_ENABLED
#import <GooglePlacesSdk/GooglePlacesSdkSpec.h>

NS_ASSUME_NONNULL_BEGIN

@interface RCTGooglePlacesSdk : NSObject <NativeGooglePlacesSdkSpec>
@end

NS_ASSUME_NONNULL_END

#else

#import <React/RCTBridgeModule.h>

NS_ASSUME_NONNULL_BEGIN

@interface RCTGooglePlacesSdk : NSObject <RCTBridgeModule>
@end

NS_ASSUME_NONNULL_END

#endif
```

3. Clean your build folder: In Xcode, go to Product > Clean Build Folder (or press ⇧⌘K).
4. Rebuild your project.

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT
