#!/bin/bash
set -x
##
## Copyright (c) 2013 EvoDesignOpt
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
## Authors: Owen Derby and Dylan Sherry and Ignacio Arnaldo


#############################################################################
#### 			User-specific variables:			 ####
#############################################################################

## path to your base FlexGP run folder
ROOT=

## name of your certificate (minus the .pem suffix)
CERT=

## cloud configuration
TYPE=t1.micro


#############################################################################
##   THE FOLLOWING VARIABLES SOULD ONLY BE MODIFIED BY FLEXGP DEVELOPERS   ##
#############################################################################


## Amazon EC2 AMI 
AMI=ami-e0016c88
# USERNAME FOR THE VIRTUAL MACHINES 
USER=ubuntu

## cloud api commands
RUN_CMD=ec2-run-instances
DESCRIBE_CMD=ec2-describe-instances

## FlexGP paths
MY_DIR=$(dirname $0)
FLEXGP_JAR_PATH=FlexGP.jar
START_SCRIPT_NAME=start.sh
PART_HANDLER_NAME=part_handler.py
