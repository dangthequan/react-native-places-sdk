/**
 * @flow
 * @format
 */

import type { TurboModule } from 'react-native/Libraries/TurboModule/RCTExport';
import * as TurboModuleRegistry from 'react-native/Libraries/TurboModule/TurboModuleRegistry';

export interface Spec extends TurboModule {
  initialize(apiKey: string): Promise<void>;
  getAPIKey(): Promise<string | null | undefined>;
  autoComplete(query: string, options: Object): Promise<Object>;
  fetchPlace(placeId: string, fields: Array<string>): Promise<Object>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('GooglePlacesSdk');
