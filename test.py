#! /usr/bin/env python

import sys
import main


def verify(people, clubs):
    for person in people:
        names = []
        for term in person.terms:
            days = set()
            for allocation in term.allocations:
                days.add(allocation.day)
                if allocation.name in main.CLUBS_REPEAT:
                    assert allocation.name not in names
                    names.append(allocation.name)
                assert person in clubs[(term.id, allocation)]
                assert ((allocation in term.requests) or (term.id == 2 and allocation.name in main.BOTH_TERM_CLUBS))
            assert len(days) == len(term.allocations)
    for club in clubs:
        allocated = clubs[club]
        assert len(allocated) <= main.CLUB_LIMITS.get(club, main.DEFAULT_CLUB_LIMIT)
        assert len(allocated) == len(set(allocated))


if __name__ == "__main__":
    (people, clubs) = main.process_requests(sys.argv[1], sys.argv[2])
    verify(people, clubs)