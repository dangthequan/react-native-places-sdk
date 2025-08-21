export interface PlaceAutocompleteResult {
  place_id: string;
  description: string;
  types: string[];
}

export interface PlaceDetails {
  placeId: string;
  name: string;
  address: string;
  coordinate?: {
    latitude: number;
    longitude: number;
  };
  phoneNumber?: string;
  website?: string;
  rating?: number;
  priceLevel?: number;
  types?: string[];
  photos?: string[];
  openingHours?: {
    isOpen: boolean;
    weekdayText?: string[];
  };
}

export interface AutocompleteOptions {
  countries?: string[];
  location?: {
    latitude: number;
    longitude: number;
  };
  radius?: number;
  types?: string[];
  strictbounds?: boolean;
}

export interface NearbySearchOptions {
  location: {
    latitude: number;
    longitude: number;
  };
  radius?: number;
  rankby?: 'prominence' | 'distance';
  keyword?: string;
  type?: string;
  minprice?: number;
  maxprice?: number;
  opennow?: boolean;
}

export interface TextSearchOptions {
  query: string;
  location?: {
    latitude: number;
    longitude: number;
  };
  radius?: number;
  type?: string;
  minprice?: number;
  maxprice?: number;
  opennow?: boolean;
}

export interface SearchResult {
  placeId: string;
  name: string;
  address?: string;
  coordinate?: {
    latitude: number;
    longitude: number;
  };
  businessStatus?: string;
  types?: string[];
  rating?: number;
  userRatingsTotal?: number;
  priceLevel?: number;
  vicinity?: string;
  photos?: string[];
  openNow?: boolean;
  iconUrl?: string;
}

export interface GooglePlacesInterface {
  /**
   * Initialize the Google Places SDK with your API key
   * @param apiKey Your Google API key with Places API enabled
   */
  initialize(apiKey: string): Promise<void>;

  /**
   * Get the Google Places API key from Info.plist or strings.xml
   */
  getAPIKey(): Promise<string | null | undefined>;

  /**
   * Search for places with a given query string
   * @param query The search query
   * @param options Optional parameters to filter results
   */
  autoComplete(
    query: string,
    options?: AutocompleteOptions
  ): Promise<Object>;

  /**
   * Get details for a specific place
   * @param placeId The place ID to fetch details for
   * @param fields Optional array of fields to include
   */
  fetchPlace(
    placeId: string,
    fields?: string[]
  ): Promise<Object>;
}
