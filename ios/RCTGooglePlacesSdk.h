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
