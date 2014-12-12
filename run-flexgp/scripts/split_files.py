#!/usr/bin/python

import argparse
import time
import os
import sys
import time
import random
#import numpy as np
import sqlite3

get_col = lambda l,idx: map(lambda x:x[idx], l)
mean = lambda l: sum(l)/float(len(l))

def tracks_by_artist(tids, db):
    q = "SELECT track_id,artist_id,year FROM songs WHERE year>0 AND track_id=?"
    data = []
    with sqlite3.connect(db) as conn:
        cur = conn.cursor()
        for tid in tids:
            res = cur.execute(q, tid).fetchone()
            if res:
                data.append(res)
    #data = np.array(data)
    #print data
    artists = list(set(get_col(data,1)))
    artist_data = {a:[[],[],0] for a in artists}
    for tid,aid,year in data:
        artist_data[aid][0].append(tid)
        artist_data[aid][1].append(int(year))
    for aid in artist_data:
        artist_data[aid][2]=mean(artist_data[aid][1])
    return artist_data

def split(samples, split_frac):
    result1 = []
    result2 = []
    n = len(samples)
    if split_frac > 1.0:
        split_frac = float(split_frac)/float(n)
    # we want to select 1 out of every m, so we just take the smaller percentage and swap it back when we return
    if split_frac <= .5:
        i = int(n*float(split_frac))
    else:
        i = int(n*float(1.0-split_frac))
    index = 0
    while i > 0:
        slice_size = n/i
        slice_samples = samples[index:index+slice_size]
        #print n, i, index, slice_size
        pos = random.randrange(0,slice_size)
        for idx,a in enumerate(slice_samples):
            if idx == pos:
                result2.append(a)
            else:
                result1.append(a)
        n -= slice_size
        index+= slice_size
        i-=1
    if split_frac < .5:
        print 'Split done, we have',len(result2),'train samples and',len(result1),'test samples.'
        return (result2, result1)
    else:
        print 'Split done, we have',len(result1),'train samples and',len(result2),'test samples.'
        return (result1, result2)

def make_msd_split(f_in, db, split_value, f1_out, f2_out):
    # grab ids
    ids = set()
    for tid in f_in:
        ids.add((tid.strip("\n"),))

    artist_data = tracks_by_artist(sorted(ids), db)
    artists = [(aid,artist_data[aid][2]) for aid in artist_data.keys()]
    artists = sorted(artists,key=lambda x:x[0]) # so its reporducible, first sort by artist id
    artists = sorted(artists,key=lambda x:x[1])

    if split_value > 1.0:
        split_value = float(split_value)/float(len(ids))

    [artists1, artists2] = split(get_col(artists,0), split_value)

    n1_tracks = 0
    n2_tracks = 0
    for aid in sorted(list(set(artists1))):
        n1_tracks += len(artist_data[aid][0])
        for tid in artist_data[aid][0]:
            f1_out.write(tid + '\n')
    for aid in sorted(list(set(artists2))):
        n2_tracks += len(artist_data[aid][0])
        for tid in artist_data[aid][0]:
            f2_out.write(tid + '\n')
    print 'We have',n1_tracks,'set1 tracks out of',len(ids),"when we wanted", int(split_value*len(ids)),"leaving us with",n2_tracks,"set2 tracks."

def make_split(f_in,  split_value, f1_out, f2_out):
    in_lines = f_in.readlines()
    [training, test] = split(in_lines, split_value)
    f1_out.writelines(training)
    f2_out.writelines(test)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="test")
    parser.add_argument('input', help="file to split, row-wise.")
    parser.add_argument('split', type=float, help="Split value. If value is < 1, specifies decimal fraction of rows to split off. Otherwise, specifies the number of rows to split off.")
    parser.add_argument('output', type=argparse.FileType('w'), help="file to put split off set of rows into.")
    parser.add_argument('rest', type=argparse.FileType('w'), help="file to store remaining rows not split off.")
    parser.add_argument('-d', '--database', default='/msd/AdditionalFiles/track_metadata.db', help="For MSD only: sql db with track,artist,year mapping")
    parser.add_argument('-r', '--seed', help="random seed to use")
    args = parser.parse_args()
    # set random seed
    if args.seed:
        random.seed(args.seed)

    with open(args.input,'r') as f_in:
        if args.input.split(".")[-1] == "msd":
            make_msd_split(f_in, args.database, args.split, args.output, args.rest)
        else:
            make_split(f_in, args.split, args.output, args.rest)
    args.output.close()
    args.rest.close()
