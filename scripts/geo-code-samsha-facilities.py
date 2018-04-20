import json
import multiprocessing as mp
import os

import googlemaps


def file_reader_worker(in_filename, num_q_consumers, q):
    counter = 0
    with open(in_filename, 'r') as json_file:
        for line in json_file:
            q.put(json.loads(line))
            counter = counter + 1

    for i in range(0, num_q_consumers):
        q.put(None)

    print("file_reader_worker finished reading JSON log")
    return counter


def geocode_worker(client: googlemaps.Client, read_q, write_q):
    while 1:
        item = read_q.get()
        if item is None:
            read_q.put(None)
            break

        try:
            geocoded_result = geocode_facility(item, client)
            write_q.put(geocoded_result)
        except googlemaps.exceptions.HTTPError as err:
            print("A HTTP error occurred while geocoding facility: ", err)
            write_q.put(item)
        except googlemaps.exceptions.Timeout as err:
            print("An Timeout occurred while geocoding facility: ", err)
            write_q.put(item)
        except googlemaps.exceptions.TransportError as err:
            print("A TransportError occurred while geocoding facility: ", err)
            write_q.put(item)
        except:
            print("An unexpected occurred while geocoding faciity")
            write_q.put(item)

    print("geocode_worker finished geocoding")


def file_writer_worker(out_filename, q):
    counter = 0
    with open(out_filename, 'w') as json_file:
        while 1:
            item = q.get()
            if item is None:
                break
            json_file.write(json.dumps(item))
            json_file.write("\n")
            counter = counter + 1
    print(f"file_writer_worker finished writing to log file: {counter} records written")


def format_street_address(facility) -> str:
    street1 = facility['street1']
    street2 = facility['street2']
    city = facility['city']
    state = facility['state']
    zipcode = facility['zip']

    street_address = street1

    if street2 is not None and len(street2) > 0:
        street_address = street_address + ", " + street2

    return street_address + ", " + city + ", " + state + " " + zipcode


def geocode_facility(geo_facility_dict: dict, client: googlemaps.Client) -> dict:
    street_address = format_street_address(geo_facility_dict)
    print(f"geocoding: {street_address}")
    try:
        geocode_result = client.geocode(street_address)
    except googlemaps.exceptions.Timeout:
        geocode_result = client.geocode(street_address)

    if len(geocode_result) == 1:
        geo_facility_dict['formatted_address'] = geocode_result[0]['formatted_address']
        geo_facility_dict['google_place_id'] = geocode_result[0]['place_id']
        geo_facility_dict['google_location'] = {
            'lat': geocode_result[0]['geometry']['location']['lat'],
            'lon': geocode_result[0]['geometry']['location']['lng']
        }
    else:
        geo_facility_dict['formatted_address'] = ''
        geo_facility_dict['google_place_id'] = ''
        geo_facility_dict['google_location'] = {}

    geo_facility_dict['location'] = {
        'lat': geo_facility_dict['latitude'],
        'lon': geo_facility_dict['longitude']
    }

    return geo_facility_dict


def main():
    FACILITY_JSON_FILE = os.environ['FACILITY_JSON_FILE']
    FACILITY_JSON_GEOCODED_FILE = FACILITY_JSON_FILE + ".geos.json"
    GOOGLE_API_KEY = os.environ['GOOGLE_API_KEY']

    gmaps = googlemaps.Client(key=GOOGLE_API_KEY)

    manager = mp.Manager()
    infile_q = manager.Queue()
    outfile_q = manager.Queue()

    pool = mp.Pool(mp.cpu_count() + 2)
    geocoder_jobs = [pool.apply_async(geocode_worker, (gmaps, infile_q, outfile_q)),
                     pool.apply_async(geocode_worker, (gmaps, infile_q, outfile_q)),
                     pool.apply_async(geocode_worker, (gmaps, infile_q, outfile_q)),
                     pool.apply_async(geocode_worker, (gmaps, infile_q, outfile_q)),
                     pool.apply_async(geocode_worker, (gmaps, infile_q, outfile_q)),
                     pool.apply_async(geocode_worker, (gmaps, infile_q, outfile_q)),
                     pool.apply_async(geocode_worker, (gmaps, infile_q, outfile_q)),
                     pool.apply_async(geocode_worker, (gmaps, infile_q, outfile_q))]

    file_jobs = [pool.apply_async(file_reader_worker, (FACILITY_JSON_FILE, 8, infile_q)),
                 pool.apply_async(file_writer_worker, (FACILITY_JSON_GEOCODED_FILE, outfile_q))]

    for job in geocoder_jobs:
        job.get()

    print("All geocoder jobs complete - notifying other jobs that work is complete")
    outfile_q.put(None)

    for job in file_jobs:
        job.get()

    pool.close()


if __name__ == "__main__":
    main()
