#! /usr/bin/env python

import csv
import datetime
import logging
import sys
import re

MAX_REQUESTS = 3
CLUBS_REPEAT = ['Fitness Club', 'Art and Craft', 'ICT Club', 'French',
                'Needlecraft', 'iMovie Club', 'Cookery', 'Spanish']
BOTH_TERM_CLUBS = ['Hockey', 'Netball']

DEFAULT_CLUB_LIMIT = 30
CLUB_LIMITS = {}


class Club:
    def __init__(self, description):
        self.description = description
        match = re.match("([^(]+) \(([^)]+)\) - (.+)", description)
        if match:
            self.name = match.group(1).replace("'", "")
            self.day = match.group(2).strip()
            self.teacher = match.group(3)
        else:
            logging.error("Unmatched description: " + description)

    def __repr__(self):
        return self.name + ' (' + self.day + ')'

    def __eq__(self, other):
        return self.name == other.name and self.day == other.day

    def __hash__(self):
        return hash(self.name + self.day)


class Term:
    def __init__(self, term_id, fields):
        self.id = term_id
        self.requests = [Club(club) for club in filter(lambda x: len(x) > 0, fields)]
        self.allocations = []

    def __repr__(self):
        return str(self.id) + ' ' + str(self.allocations)

    def allocated(self):
        return " and ".join([str(x) for x in self.allocations])


class Response:
    def __init__(self, fields):
        self.submitted = datetime.datetime.strptime(fields[0].upper()[:-4], "%Y/%m/%d %I:%M:%S %p")
        self.name = fields[1].strip().lower()
        self.group = fields[2]
        self.year = int(self.group[1:2])
        self.terms = []
        count = (len(fields) - 3)/MAX_REQUESTS
        for i in range(count):
            self.terms.append(Term(i + 1, fields[MAX_REQUESTS*i+3:MAX_REQUESTS*i+3+MAX_REQUESTS]))

    def __repr__(self):
        return self.name.title() + ' (' + self.group + ')'

    def __cmp__(self, other):
        if self.year == other.year:
            return cmp(self.submitted, other.submitted)
        else:
            return -cmp(self.year, other.year)

    def __eq__(self, other):
        return self.name == other.name and self.group == other.group

    def __hash__(self):
        return hash(self.name + self.group)

    def allocate(self, term, club):
        repeatable = club.name not in CLUBS_REPEAT \
                     or club.name not in [y.name for x in self.terms for y in x.allocations]
        busy = filter(lambda z: z.day == club.day, term.allocations)
        logging.debug("Checking allocation repeatable: %s and busy: %s", repeatable, busy)
        if repeatable and not busy:
            term.allocations.append(club)
            return True
        return False


def parse_file(request_path, priority_path):
    responses = []
    lower_priorities = []
    with open(priority_path) as pr:
        for line in pr:
            lower_priorities.append(line.strip().lower())
    logging.info('Lower priorities: %s', lower_priorities)
    with open(request_path, 'rb') as fr:
        for response in csv.reader(fr):
            if response[0] != 'Timestamp':
                r = Response(response)
                if r.name in lower_priorities:
                    logging.info('Updating submitted to now for %s', r.name)
                    r.submitted = datetime.datetime.now()
                responses.append(r)
    return set(reversed(responses))


def allocate(requests):
    allocations = {}
    for i in range(MAX_REQUESTS):
        for request in list(requests):
            logging.info('Allocating %s %s', request.name, request.group)
            for term in request.terms:
                for club in list(term.requests):
                    if allocate_club(allocations, club, request, term):
                        logging.info('Allocated %s to %s %s', request.name, term.id, club)
                        if club.name in BOTH_TERM_CLUBS:
                            allocate_club(allocations, club, request, request.terms[1])
                        break
    return requests, allocations


def allocate_club(allocations, club, request, term):
    key = (term.id, club)
    logging.debug('Checking %s', key)
    allocated = False
    if key in allocations:
        count = len(allocations[key])
        limit = CLUB_LIMITS.get(club.name, DEFAULT_CLUB_LIMIT)
        logging.debug('Checking space %d < %d', count, limit)
        if count < limit and request.allocate(term, club):
            allocations[key].append(request)
            allocated = True
    elif request.allocate(term, club):
        allocations[key] = [request]
        allocated = True
    return allocated


def process_requests(request_file, priority_list):
    return allocate(sorted(parse_file(request_file, priority_list)))


def write_people(allocated_people):
    with open("pupils.csv", "wb") as pw:
        writer = csv.writer(pw)
        for person in allocated_people:
            writer.writerow([person.name.title(), person.group] + [term.allocated() for term in person.terms])


def write_clubs(allocated_clubs):
    with open("clubs.csv", "wb") as cw:
        writer = csv.writer(cw)
        keys = allocated_clubs.keys()
        writer.writerow(keys)
        for row in map(None, *allocated_clubs.values()):
            writer.writerow(row)


if __name__ == "__main__":
    logging.basicConfig(filename='main.log', level=logging.DEBUG)
    (people, clubs) = process_requests(sys.argv[1], sys.argv[2])
    write_people(people)
    write_clubs(clubs)
    for c in clubs:
        print c, ' = ', len(clubs[c]), "FULL" if len(clubs[c]) == CLUB_LIMITS.get(c[1].name) else ""
