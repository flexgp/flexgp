#!/bin/bash
##
## Copyright (c) 2012 EvoDesignOpt
##
## Licensed under the MIT License.
##
## See the "LICENSE" file for a copy of the license.
##
## THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
## EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
## MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
## NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
## BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
## ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
## CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
## SOFTWARE.
##
## authors: Owen Derby, Dylan Sherry and Ignacio Arnaldo

# This file is used to initiate a run of the FlexGP program;
# effectively bootstrapping the program from originating at a single
# node into n cloud nodes.

#########################################################################################
### Note: Must set this path before proceeding. Should point to the script which
### defines all user-specific variables
USER_VARS_SCRIPT_PATH=""
#########################################################################################

if [ ! -e $USER_VARS_SCRIPT_PATH ]; then
    echo "Error: USER_VARS script not found at $USER_VARS_SCRIPT_PATH"
    exit 1
fi
source $USER_VARS_SCRIPT_PATH
source certs/exportCredentials.sh

$DESCRIBE_CMD | grep $AMI | cut -f17 > ips.txt
parallel-slurp -OStrictHostKeyChecking=no -OCheckHostIP=no -OIdentityFile=certs/$CERT.pem -t 100 -h ips.txt -l ubuntu -L models/ /home/ubuntu/bestModelGeneration.txt bestModelGeneration.txt
parallel-slurp -OStrictHostKeyChecking=no -OCheckHostIP=no -OIdentityFile=certs/$CERT.pem -t 100 -h ips.txt -l ubuntu -L models/ /home/ubuntu/evolve.log evolve.log
parallel-slurp -OStrictHostKeyChecking=no -OCheckHostIP=no -OIdentityFile=certs/$CERT.pem -t 100 -h ips.txt -l ubuntu -L models/ /home/ubuntu/init.log init.log
parallel-slurp -OStrictHostKeyChecking=no -OCheckHostIP=no -OIdentityFile=certs/$CERT.pem -t 100 -h ips.txt -l ubuntu -L models/ /home/ubuntu/problem.properties problem.properties
