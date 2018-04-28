import {GeoPoint} from './geo-point';

export class TreatmentFacility {
  id: number;
  category_codes: string[];
  category_names: string[];
  city: string;
  country: string;
  formatted_address: string;
  google_location: string;
  google_place_id: string;
  host: string;
  intake1: string;
  intake2: string;
  intake_prompt: string;
  last_update: string;
  location: GeoPoint;
  latitude: number;
  longitude: number;
  name1: string;
  name2: string;
  path: string;
  phone: string;
  service_codes: string[];
  service_names: string[];
  state: string;
  street1: string;
  street2: string;
  type_facility: string;
  website: string;
  zip: string;
  zip4: string;
}
