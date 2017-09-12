#! /usr/bin/env python

import csv
import datetime
import sys
import re

REPEAT_CLUBS = ['Fitness Club', 'Art and Craft', 'ICT Club', 'French',
                'Needlecraft', 'iMovie Club', 'Cookery', 'Spanish']

CLUB_LIMITS = {'Fitness Club': 12,
               }


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


class Response:
    def __init__(self, fields):
        self.submitted = datetime.datetime.strptime(fields[0].upper()[:-4], "%Y/%m/%d %I:%M:%S %p")
        self.name = fields[1].lower()
        self.group = fields[2]
        self.year = int(self.group[1:2])
        self.requests_one = [Club(club) for club in filter(lambda x: len(x) > 0, fields[3:6])]
        self.requests_two = [Club(club) for club in filter(lambda x: len(x) > 0, fields[6:9])]
        self.allocations_one = []
        self.allocations_two = []

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

    def allocate_one(self, club):
        if (club.name not in REPEAT_CLUBS or club not in self.allocations_two) \
                and not filter(lambda x: x.day == club.day, self.allocations_one):
            self.allocations_one.append(club)
            return True
        return False

    def allocate_two(self, club):
        if (club.name not in REPEAT_CLUBS or club not in self.allocations_one) \
                and not filter(lambda x: x.day == club.day, self.allocations_two):
            self.allocations_two.append(club)
            return True
        return False


def parse_file(path):
    responses = set()
    with open(path, 'rb') as fr:
        for response in csv.reader(fr):
            if response[0] != 'Timestamp':
                responses.add(Response(response))
    return responses


def allocate_one(requests):
    allocations = {}
    for i in range(3):
        for request in list(requests):
            for club in list(request.requests_one):
                if club in allocations:
                    count = len(allocations[club])
                    if count < CLUB_LIMITS.get(club.name, 12) and request.allocate_one(club):
                        allocations[club].append(request)
                        break
                elif request.allocate_one(club):
                    allocations[club] = [request]
                    break
    return allocations


def allocate_two(requests):
    allocations = {}
    for i in range(3):
        for request in list(requests):
            for club in list(request.requests_two):
                if club in allocations:
                    count = len(allocations[club])
                    if count < CLUB_LIMITS.get(club.name, 12) and request.allocate_two(club):
                        allocations[club].append(request)
                        break
                elif request.allocate_two(club):
                    allocations[club] = [request]
                    break
    return allocations


if __name__ == "__main__":
    requested = sorted(parse_file(sys.argv[1]))
    result = allocate_one(requested)
    print 'First Term'
    for allocation in sorted(result.keys()):
        print allocation, '=', result[allocation]
    result = allocate_two(requested)
    print 'Second Term'
    for allocation in sorted(result.keys()):
        print allocation, '=', result[allocation]
    for result in requested:
        print result.name.title(), result.allocations_one, result.allocations_two
