import { NativeModules, Platform } from 'react-native';
import type { 
  GooglePlacesInterface,
  AutocompleteOptions,
} from './types';

const LINKING_ERROR =
  `The package 'react-native-places-sdk' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

// Use TurboModules when available for New Architecture
let GooglePlacesModule: any;

// Check if we're running on New Architecture
const isNewArchitectureEnabled = (): boolean => {
  try {
    // Check for specific New Architecture indicators
    // On React Native 0.70+, __turboModuleProxy and TurboModuleRegistry are available
    // @ts-ignore - we're just checking if these exist at runtime
    return !!(global.__turboModuleProxy || require('react-native').TurboModuleRegistry);
  } catch (e) {
    return false;
  }
};

if (isNewArchitectureEnabled()) {
  // Import the TurboModule implementation directly
  try {
    // @ts-ignore - dynamic import for New Architecture
    GooglePlacesModule = require('./NativeGooglePlacesSdk').default;
  } catch (e) {
    // Fallback to legacy architecture if import fails
    GooglePlacesModule = NativeModules.GooglePlacesSdk;
  }
} else {
  // Use legacy module for old architecture
  GooglePlacesModule = NativeModules.GooglePlacesSdk
    ? NativeModules.GooglePlacesSdk
    : new Proxy(
        {},
        {
          get() {
            throw new Error(LINKING_ERROR);
          },
        }
      );
}

const GooglePlaces: GooglePlacesInterface = {
  initialize(apiKey: string): Promise<void> {
    return GooglePlacesModule.initialize(apiKey);
  },

  getAPIKey(): Promise<string | null | undefined> {
    return GooglePlacesModule.getAPIKey();
  },

  autoComplete(
    query: string,
    options?: AutocompleteOptions
  ): Promise<Object> {
    return GooglePlacesModule.autoComplete(query, options || {});
  },

  fetchPlace(
    placeId: string,
    fields?: string[]
  ): Promise<Object> {
    return GooglePlacesModule.fetchPlace(placeId, fields || []);
  }
};

export default GooglePlaces;
export * from './types';
