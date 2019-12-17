import mechanicalsoup
import uuid
import re
import json
import time

PROFILE_URI_RE = re.compile('^/resources/addiction-industry-directory/(\d+)/.*$')
NAATP_FQDN = 'https://www.naatp.org'
NAATP_URL = f'{NAATP_FQDN}/resources/addiction-industry-directory'


def get_field_content(div) -> str:
    if div is None:
        return ""
    span = div.find("span", {'class', "field-content"})
    if span is not None:
        return span.string


def get_description_content(div) -> str:
    if div is None:
        return ""
    span = div.find("span", {'class', "field-content"})
    if span is not None:
        return span.get_text()


def get_link_href(link) -> str:
    if link is None:
        return ""
    if link is not None:
        if link.attrs['href'] == None:
            return link.get_text()
        else:
            return link.attrs['href']


def get_url_content(div) -> str:
    if div is None:
        return ""
    span = div.find("span", {'class', "field-content"})
    if span is not None:
        return get_link_href(span.find("a"))


def get_email_content(div) -> str:
    if div is None:
        return ""
    span = div.find("span", {'class', "field-content"})
    if span is not None:
        link = span.find("a")
        if link is not None:
            return link.string


def fetch_facility_details(browser, url) -> dict:
    page = browser.get(url)

    result = {'mailing_address':
                  get_description_content(page.soup.find("div", {'class', 'views-label views-label-postal-code'})),
              'phone': get_field_content(page.soup.find("div", {'class', "views-field views-field-phone"})),
              'email': get_field_content(page.soup.find("div", {'class', "views-field views-field-email"})),
              'website_url': get_url_content(page.soup.find("div", {'class', "views-field views-field-url"})),
              'ceo': get_field_content(page.soup.find("div", {'class', "views-field views-field-ceo-77"})),
              'ceo_email': get_email_content(page.soup.find("div", {'class', "views-field views-field-ceo-email-80"})),
              'ceo_phone': get_field_content(page.soup.find("div", {'class', "views-field views-field-ceo-phone-78"})),
              'admissions_contact': get_field_content(
                  page.soup.find("div", {'class', "views-field views-field-admissions-contact-69"})),
              'admissions_email': get_field_content(
                  page.soup.find("div", {'class', "views-field views-field-admissions-email-72"})),
              'admissions_phone': get_field_content(
                  page.soup.find("div", {'class', "views-field views-field-admissions-phone-70"})),
              'marketing_contact': get_field_content(
                  page.soup.find("div", {'class', "views-field views-field-marketing-contact-73"})),
              "marketing_email": get_field_content(
                  page.soup.find("div", {'class', "views-field views-field-marketing-email-76"})),
              "marketing_phone": get_field_content(
                  page.soup.find("div", {'class', "views-field views-field-marketing-phone-74"})),
              "organization_description": get_description_content(
                  page.soup.find("div", {'class', "views-field views-field-organization-description-11"})),
              "mission_statement": get_description_content(
                  page.soup.find("div", {'class', "views-field views-field-mission-statement-21"})),
              "licensed": get_field_content(page.soup.find("div", {'class', "views-field views-field-licensed-31"})),
              'licensing_body': get_description_content(
                  page.soup.find("div", {'class', "views-field views-field-licensing-body-32"})),
              'accreditation': get_description_content(
                  page.soup.find("div", {'class', "views-field views-field-accreditation-35"})),
              'year_founded': get_field_content(
                  page.soup.find("div", {'class', "views-field views-field-year-founded-22"})),
              'bilingual_services': get_description_content(
                  page.soup.find("div", {'class', "views-field views-field-bilingual-services-14"})),
              'levels_of_treatment_care': get_description_content(
                  page.soup.find("div", {'class', "views-field views-field-levels-of-treatment-care-25"})),
              'specialty_programs': get_description_content(
                  page.soup.find("div", {'class', "views-field views-field-specialty-programs-30"})),
              'length_of_stay': get_description_content(
                  page.soup.find("div", {'class', "views-field views-field-length-of-stay-26"})),
              'number_of_beds': get_description_content(
                  page.soup.find("div", {'class', "views-field views-field-number-of-beds-23"})),
              'payment_assistance_available': get_description_content(
                  page.soup.find("div", {'class', "views-field views-field-payment-assistance-available-28"})),

              'type_of_payment_assistance_available': get_description_content(
                  page.soup.find("div", {'class', "views-field views-field-type-of-payment-assistance-avail-29"}))}
    # phone

    return result


def fetch_treatment_facility_record_id(href):
    result = PROFILE_URI_RE.match(href)
    if result is not None:
        return int(result[1])
    return None


def fetch_treatment_facilities(directory_page) -> list:
    view_content_div = directory_page.soup.find("div", {"class": "view-content"})
    tbody = view_content_div.find("tbody", {})

    treatment_facilities = []
    for tr in tbody.select('tr'):
        treatment_facilities.append(create_treatment_facility_record(tr))

    return treatment_facilities


def navigate_treatment_directory(browser, page_url):
    time.sleep(5)
    directory_page = browser.get(page_url)

    treatment_facilities = fetch_treatment_facilities(directory_page)

    next_page_link = directory_page.soup.find("a", {"title": "Go to next page"})
    if next_page_link is not None:
        next_page_href = next_page_link.attrs['href']
        return {
            'results': treatment_facilities,
            'next_page': f'{NAATP_FQDN}{next_page_href}'
        }
    return {
        'results': treatment_facilities,
        'next_page': None
    }


def generator_to_list(generator) -> list:
    retval = []
    for item in generator:
        retval.append(item)
    return retval


def create_treatment_facility_record(tr) -> dict:
    facility_record = {}
    displayname_td = tr.find('td', {'class': 'views-field views-field-display-name'})
    profile_uri = displayname_td.find('a')
    profile_href = profile_uri.attrs['href']

    location_td = tr.find('td', {'class': 'views-field views-field-state-province'})
    levels_of_care = tr.find('td', {'class': 'views-field views-field-levels-of-treatment-care-25'})

    if profile_uri is not None:
        facility_record['display_name'] = profile_uri.string
    else:
        facility_record['display_name'] = ''

    if location_td is not None and len(location_td.contents) >= 2:
        facility_record['city_state'] = str(location_td.contents[0]).strip()
        facility_record['country'] = str(location_td.contents[1].string).lstrip(",").strip()
    elif location_td is not None and len(location_td.contents) == 1:
        facility_record['city_state'] = str(location_td.contents[0]).strip()
        facility_record['country'] = ""
    elif location_td is None or len(location_td.contents) <= 0:
        facility_record['city_state'] = ""
        facility_record['country'] = ""

    if levels_of_care is not None:
        facility_record['levels_of_care'] = str(levels_of_care.string).strip().split(",")
    else:
        facility_record['levels_of_care'] = []


    facility_record['id'] = fetch_treatment_facility_record_id(profile_href)
    facility_record['profile_uri'] = f'{NAATP_FQDN}{profile_href}'

    if profile_uri is not None and "class" in profile_uri.attrs:
        if 'accredited-Yes' in profile_uri.attrs['class']:
            facility_record['accredited'] = 'yes'
        else:
            facility_record['accredited'] = 'no'
    else:
        facility_record['accredited'] = 'unknown'

    return facility_record


if __name__ == '__main__':
    # create browser object
    browser = mechanicalsoup.Browser()

    directory_page_url = NAATP_URL
    treatment_facilities = []
    while True:
        last_directory_page_url = directory_page_url

        print(f"Fetching directory page {directory_page_url}")
        results = navigate_treatment_directory(browser, directory_page_url)
        treatment_facilities = treatment_facilities + results['results']
        directory_page_url = results['next_page']

        time.sleep(.750)
        if directory_page_url is None:
            break

    results = []
    total_items = len(treatment_facilities)
    for idx, item in enumerate(treatment_facilities):
        page_to_fetch = item['profile_uri']
        print(f"Processing facility [{idx} of {total_items}] -> GET {page_to_fetch}")
        details = fetch_facility_details(browser, item['profile_uri'])
        merged = {**item, **details}
        results.append(merged)
        time.sleep(1.250)

        if idx > 30 and idx % 10 == 0:
            with open("naatp_providers-detail.json", 'w') as outfile:
                json.dump(results, indent=2, fp=outfile)

    with open("naatp_providers-detail.json", 'w') as outfile:
        json.dump(results, indent=2, fp=outfile)




