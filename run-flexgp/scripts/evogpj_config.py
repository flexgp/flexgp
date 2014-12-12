#!/usr/bin/python

# Tool for interpretting various factors and modifying the EvoGPJ
# problem properties file to reflect those factors.

import random
import argparse

def rand_terms(f):
    ret = 'terminal_set ='
    up = int(f.readline().strip())
    t = range(1,up+1)
    num = int(rand_line(f).strip())
    for i in range(num):
        j = random.choice(range(len(t)))
        ret += " X" + str(t[j])
        del t[j]
    return ret + "\r\n"
    
def rand_line(f):
    return random.choice(f.readlines())

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="test")
    parser.add_argument('-f', '--funcs', type=argparse.FileType('r'),
                        help="factorize function_set")
    parser.add_argument('-t', '--terms', type=argparse.FileType('r'),
                        help="factorize terminal_set")
    parser.add_argument('-n', '--pnorm', type=argparse.FileType('r'),
                        help="factorize norms")
    parser.add_argument('config', help="Configuration file to work from")
    args = parser.parse_args()
    
    with open(args.config,"r") as f:
        opts = f.readlines()
    with open(args.config,"w") as f:
        for line in opts:
            if args.funcs and line.split()[0] == 'function_set':
                f.write(rand_line(args.funcs))
                args.funcs.close()
            elif args.terms and line.split()[0] == 'terminal_set':
                f.write(rand_terms(args.terms))
                args.terms.close()
            elif args.pnorm and line.split()[0] == 'fitness_mean_pow':
                f.write(rand_line(args.pnorm))
                args.pnorm.close()
            else:
                f.write(line)
