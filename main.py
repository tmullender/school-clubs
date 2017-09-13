#! /usr/bin/env python

import csv
import datetime
import sys
import re

MAX_REQUESTS = 3
REPEAT_CLUBS = ['Fitness Club', 'Art and Craft', 'ICT Club', 'French',
                'Needlecraft', 'iMovie Club', 'Cookery', 'Spanish']

DEFAULT_CLUB_LIMIT = 20
CLUB_LIMITS = {}


class Club:
    def __init__(self, description):
        self.description = description
        match = re.match("([^(]+) \(([^)]+)\) - (.+)", description)
        self.name = match.group(1).replace("'", "")
        self.day = match.group(2)
        self.teacher = match.group(3)

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
        self.name = fields[1].lower()
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
        if (club.name not in REPEAT_CLUBS or club.name not in [y.name for x in self.terms for y in x.allocations]) \
                and not filter(lambda z: z.day == club.day, term.allocations):
            term.allocations.append(club)
            return True
        return False


def parse_file(path):
    responses = set()
    with open(path, 'rb') as fr:
        for response in csv.reader(fr):
            if response[0] != 'Timestamp':
                responses.add(Response(response))
    return responses


def allocate(requests):
    allocations = {}
    for i in range(MAX_REQUESTS):
        for request in list(requests):
            for term in request.terms:
                for club in list(term.requests):
                    key = (term.id, club)
                    if key in allocations:
                        count = len(allocations[key])
                        if count < CLUB_LIMITS.get(club.name, DEFAULT_CLUB_LIMIT) \
                                and request.allocate(term, club):
                            allocations[key].append(request)
                            break
                    elif request.allocate(term, club):
                        allocations[key] = [request]
                        break
    return requests, allocations


def process_requests(file_path):
    return allocate(sorted(parse_file(file_path)))


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
    (people, clubs) = process_requests(sys.argv[1])
    write_people(people)
    write_clubs(clubs)
