#!/usr/bin/python

## A tool for randomly dividing up a file into 1 or more subsets
##
## Author: Dylan Sherry
##
## Input:
## -i path to source file
## -o a string of form (path num)*, where path indicates the path to an output file, and num is either the
## number of lines or the percentage of lines from the input that file should contain
##
## Output: saves n files with specified lines
##
## USAGE
## to specify a specific number of lines for each output file (1 or more):
## python select_data.py -i path/to/infile -o path/to/outfile_1 123 path/to/outfile_2 4567 path/to/outfile_3 890
## to specify the percentage of the input file which should go to each output file:
## python select_data.py -i path/to/infile -o path/to/outfile_1 0.7 path/to/outfile_2 0.1 path/to/outfile_3 0.2

import random
import argparse
import math
import os

## f_in: file descriptor for input file
## output: list of files to output to, followed by number
def partition_random_lines(in_lines, output_dict):
    num_bins = len(output)
    bins = []
    assert sum([output_dict[i][0] for i in output_dict.keys()]) <= len(in_lines), "Error: sum of number of lines is too great"
    for fname in output_dict.keys():
        out_lines = []
        for line_number in xrange(output_dict[fname][0]):
            i = int(math.floor(len(in_lines) * random.random()))
            out_lines.append(in_lines[i])
            del in_lines[i]
        output_dict[fname][1].writelines(out_lines)
    return output_dict

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="test")
    parser.add_argument('-i', '--input', type = argparse.FileType('r'), default = '-')
    ## list of filenames and ints
    parser.add_argument('-o', '--output', default = '', type=str, nargs='+')
    args = parser.parse_args()

    ## read lines
    in_lines = args.input.readlines()

    ## convert output paths to files
    output = {}
    path = ""
    print "Selecting lines for \"%s\"" % (args.input.name)
    for i,value in enumerate(args.output):
        if i % 2 == 0: ## path
            path = value
            assert os.path.exists(os.path.split(path)[0]), "Invalid path %s" % (path)
            assert len(output.keys()) == int(i / 2.0)
            output[path] = (None,file(path, 'w'))
        else: ## int
            assert output.has_key(path), "Invalid key %s" % (path)
            ## if float (percentage specified)
            if "." in value:
                value = int(math.floor(len(in_lines) * float(value)))
            print "Selecting %s lines for \"%s\"" % (value, path)
            output[path] = (int(value), output[path][1])
    ## select random lines
    partition_random_lines(in_lines, output)
    args.input.close()
    for key in output.keys():
        output[key][1].close()
