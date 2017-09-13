#! /usr/bin/env python

import csv
import datetime
import random
import sys

CLASSES = ['P4A', 'P4B', 'P5A', 'P5B', 'P6A', 'P6B', 'P7A', 'P7B']
CLUBS = ['Football (Monday) - Teacher A',
         'Netball (Tuesday) - Teacher B',
         'Hockey (Monday) - Teacher C',
         'Hockey (Tuesday) - Teacher D',
         'Fitness Club (Monday) - Teacher E',
         'Spanish (Friday) - Teacher F',
         'French (Tuesday) - Teacher G',
         'Art and Craft (Monday) - Teacher H',
         'ICT Club (Tuesday) - Teacher I',
         'Art and Craft (Friday) - Teacher J',
         'Needlecraft (Monday) - Teacher K',
         'iMovie Club (Friday) - Teacher L',
         'Art and Craft (Friday) - Teacher M',
         'Scripture Union (Tuesday) - Teacher N']


def generate_row(index):
    data = []
    time = datetime.datetime.now() + datetime.timedelta(minutes=index)
    data.append(time.strftime("%Y/%m/%d %I:%M:%S %p") + " CET")
    data.append('Person ' + str(index))
    data.append(random.choice(CLASSES))
    data.append(random.choice(CLUBS))
    data.append(random.choice(CLUBS))
    data.append(random.choice(CLUBS))
    data.append(random.choice(CLUBS))
    data.append(random.choice(CLUBS))
    data.append(random.choice(CLUBS))
    data.append(random.choice(CLUBS))
    data.append(random.choice(CLUBS))
    data.append(random.choice(CLUBS))
    return data


if __name__ == "__main__":
    with open('data.csv', 'wb') as fw:
        writer = csv.writer(fw)
        writer.writerow(['Timestamp','Full Name','Class','First Choice','Second Choice','Third Choice','First Choice','Second Choice','Third Choice','First Choice','Second Choice','Third Choice'])
        for i in range(int(sys.argv[1])):
            writer.writerow(generate_row(i))