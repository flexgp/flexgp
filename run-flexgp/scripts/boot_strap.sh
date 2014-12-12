#!/bin/bash
#set -x
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

usage()
{
cat <<EOF
usage: $0 -n INT [-k INT] [-p FILE [-d FILE [-c NUM] [-s NUM] || -x FILE] [-f FILE] [-t FILE] [-l FILE]]

Boot script to start up FlexGP Distributed Computing Package.

Must specify the number of nodes. May optionally specify a problem
to run on, as well as data and other possible factorings to use.

Cannot specify both a data file to factor and to not factor with.

OPTIONS:
   -h      Show this message
   -n      Number of nodes to run
   -k      Fanout - number of children each node will try to start. Defaults to 5
   -s      Each node's train/validation split. Only used if -d also used. If value is < 1, specifies decimal fraction of samples for each node to use as training. Otherwise, specifies the number of training samples each node will use. If -c is used, then this is the proportion to use after splitting off samples for fusion training.
   -c      Fusion train/test split. Only used if -d is also used. if value is < 1, specifies decimal fraction of samples, otherwise specifies number of samples. This value determines how many training samples are reserved for fusion training.
   -p      Problem properties file
   -d      Problem training data file to factorize
   -x      Problem training data file, but don't factorize it
   -f      Factorize along function set
   -t      Factorize along variables (terminal set)
   -l      Factorize along pnorm functions (used in calculating fitness in SR)
   -r      specify certificate file to override the one loaded from user_vars.sh
   -m      Enable migration
   -i      Specify number of second-tier nodes to start
EOF
}

START_NODES=
PROBLEM=
DATA=
FUNC=
TERM=
NORM=
TRAIN=
FUSION=
EXTRA_OPTS=
while getopts “hn:p:d:f:t:l:x:k:s:c:r:a:m:i:” OPTION; do
     case $OPTION in
         h)
             usage
             exit 1
             ;;
         n)
             START_NODES=$OPTARG
             ;;
         p)
             PROBLEM=$OPTARG
             ;;
         d)
             DATA=$OPTARG
             ;;
         f)
             FUNC=$OPTARG
             ;;
         t)
             TERM=$OPTARG
             ;;
         l)
             NORM=$OPTARG
             ;;
         x)
             TRAIN=$OPTARG
             ;;
         k)
             EXTRA_OPTS="$EXTRA_OPTS k=$OPTARG:"
             ;;
         s)
             EXTRA_OPTS="$EXTRA_OPTS s=$OPTARG:"
             ;;
         c)
             FUSION=$OPTARG
             ;;
         r)
	     CERT=$OPTARG
             ;;
         a)
             ADDRESS=$OPTARG
             ;;
	 m)
	     EXTRA_OPTS="$EXTRA_OPTS m=$OPTARG:"
	     ;;
	 i)
	     EXTRA_OPTS="$EXTRA_OPTS i=$OPTARG:"
	     ;;
         ?)
             usage
             exit
             ;;
     esac
done

#echo "$START_NODES:$PROBLEM:$DATA:$FUNC:$TERM"

if [[ -z $START_NODES ]]; then
    echo "Must specify number of nodes to start!"
    usage
    exit 1
fi

if [[ -z $PROBLEM ]]; then
    if [[ $DATA ]] || [[ $FUNC ]] || [[ $TERM ]] || [[ $NORM ]] || [[ $TRAIN ]]; then
	usage
	exit 1
    fi
elif [ ! -f $PROBLEM ]; then
    echo "Problem file $PROBLEM does not exist!"
    usage
    exit 1
fi

if [[ $DATA ]] && [[ $TRAIN ]]; then
    echo "Cannot specify both data file to factorize and data file to not factorize!"
    usage
    exit 1
fi

if [ ! -f $DATA ]; then
    echo "Data file $DATA does not exist!"
    usage
    exit 1
fi

if [ ! -f $TRAIN ]; then
    echo "Train file $TRAIN does not exist!"
    usage
    exit 1
fi

if [ ! -f $FUNC ]; then
    echo "Function set file $FUNC does not exist!"
    usage
    exit 1
fi

if [ ! -f $TERM ]; then
    echo "Terminal set file $TERM does not exist!"
    usage
    exit 1
fi

if [ ! -f $NORM ]; then
    echo "pnorm file $NORM does not exist!"
    usage
    exit 1
fi

echo $START_NODES > start_nodes.txt

NUM_NODES=$START_NODES
echo "Starting up $NUM_NODES instances in hopes of getting $START_NODES running"
echo "Starting up gateway instance"

echo "change certificate permissions and export AWS variables"
chmod 0600 ./certs/*
source certs/exportCredentials.sh

echo $RUN_CMD -k $CERT -t $TYPE -n 1 $AMI | grep INSTANCE | cut -f2
INSTANCE=$($RUN_CMD -k $CERT -t $TYPE -n 1 $AMI | grep INSTANCE | cut -f2)
echo -n "Waiting for gateway to come up "

STATUS=$($DESCRIBE_CMD | grep $INSTANCE | cut -f6)
while [ "$STATUS" = "pending" ]; do
    echo -n "."
    sleep 1
    STATUS=$($DESCRIBE_CMD | grep $INSTANCE | cut -f6)
done

echo " First node up!"

if [ "$STATUS" != "running" ]; then
    echo "not running, $STATUS"
    exit 1
fi

#ADDRESS=$($DESCRIBE_CMD | grep $INSTANCE | cut -f4)
ADDRESS=$($DESCRIBE_CMD | grep $INSTANCE | cut -f17)

echo "First node started, with id $INSTANCE, with address $ADDRESS"

echo $NUM_NODES > nodes.txt
echo "copying local files"
cp $ROOT/scripts/select_data.py ./
cp $ROOT/scripts/split_files.py ./
cp $ROOT/scripts/evogpj_config.py ./
cp ./certs/$CERT.pem ./
cp $ROOT/scripts/$PART_HANDLER_NAME ./
cp $ROOT/$FLEXGP_JAR_PATH ./
cp $ROOT/$SOCKET_STRESS_JAR ./
cp $ROOT/lib/linux/x86_64/libsigar-amd64-linux.so ./
echo "tarring everything up"
tar cf stuff.tar certs nodes.txt start_nodes.txt select_data.py split_files.py evogpj_config.py $CERT.pem $PART_HANDLER_NAME $FLEXGP_JAR_PATH $SOCKET_STRESS_JAR libsigar-amd64-linux.so
if [ $PROBLEM ]; then
    cp $PROBLEM ./problem.properties
    tar rf stuff.tar ./problem.properties
fi

if [ $DATA ]; then
    EXT=$(echo $DATA | sed 's/.*\.\(.*\)/\1/')
    DATA_FILE="data.$EXT"
    shuf -o ./$DATA_FILE $DATA
    if [ ! -z "$FUSION" ]; then
	## create fusion_train data
	$ROOT/scripts/split_files.py ./$DATA_FILE $FUSION ./fusion_train.$EXT ./$DATA_FILE.tmp
	mv ./$DATA_FILE.tmp ./$DATA_FILE
    fi
    tar rf stuff.tar $DATA_FILE
fi

if [ $TRAIN ]; then
    TRAIN_FILE="train.$(echo $TRAIN | sed 's/.*\.\(.*\)/\1/')"
    shuf -o ./$TRAIN_FILE $TRAIN
    tar rf stuff.tar $TRAIN_FILE
fi

if [ $FUNC ]; then
    cp $FUNC ./func.txt
    #shuf -o ./func.txt $FUNC
    tar rf stuff.tar func.txt
fi
if [ $TERM ]; then
    cp $TERM ./term.txt
    #shuf -o ./term.txt $TERM
    tar rf stuff.tar term.txt
fi
if [ $NORM ]; then
    cp $NORM ./norm.txt
    #shuf -o ./norm.txt $NORM
    tar rf stuff.tar norm.txt
fi
if [ ! -z "$EXTRA_OPTS" ]; then
    echo "$EXTRA_OPTS" > options.txt
    tar rf stuff.tar options.txt
fi
if [ ! -z "$REPORT" ]; then
    echo "$REPORT" > report.txt
    tar rf stuff.tar report.txt
fi
sleep 3
echo "copy over to first node"

chmod 0600 certs/$CERT.pem
scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i certs/$CERT.pem $ROOT/scripts/$START_SCRIPT_NAME $USER@$ADDRESS:~/
while [ $? -ne 0 ]; do
    sleep 4
    echo "trying again"
    scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i certs/$CERT.pem $ROOT/scripts/$START_SCRIPT_NAME $USER@$ADDRESS:~/
done
scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i certs/$CERT.pem $USER_VARS_SCRIPT_PATH $USER@$ADDRESS:~/
scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i certs/$CERT.pem stuff.tar $USER@$ADDRESS:~/

echo "run gateway script on Gateway"
if [ -z "$REPORT" ]; then
    ssh -oStrictHostKeyChecking=no -oCheckHostIP=no -i certs/$CERT.pem $USER@$ADDRESS "bash $START_SCRIPT_NAME 1"
else
    ssh -oStrictHostKeyChecking=no -oCheckHostIP=no -i certs/$CERT.pem $USER@$ADDRESS "nohup bash $START_SCRIPT_NAME 1 &> start.log < /dev/null &" > flexgp_run.log
    echo "done"
fi
