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

#
# This is the second script in the startup sequence of a FlexGP
# run. This script runs on every cloud node. It is responsible for
# assembling the required files (either via un-tarring or copying from
# parent), selecting any factorings, spawning (possibly none) children
# and then running the FlexGP java library.
#

# synchronize our time before we start
sudo /usr/sbin/ntpdate-debian -s -b

USER_VARS_SCRIPT_PATH=/home/ubuntu/user_vars.sh
if [ ! -e $USER_VARS_SCRIPT_PATH ]; then
    echo "Error: USER_VARS script not found at $USER_VARS_SCRIPT_PATH"
    ls -lhA > /home/ubuntu/start_fail.log
    echo "Error: USER_VARS script not found at $USER_VARS_SCRIPT_PATH" >> /home/ubuntu/start_fail.log
    exit 1
fi
source $USER_VARS_SCRIPT_PATH

START=$(date +%s.%N)
GATEWAY_NODE=$1

export HOME=/home/$USER
cd $HOME
if [ -z $GATEWAY_NODE ]; then
    export HOME=/home/$USER
else
    cd $HOME
    tar xf stuff.tar
fi

FLEXGP_JAR_PATH=$HOME/FlexGP.jar
if [ -z $CLASSPATH ]; then
    export CLASSPATH=$FLEXGP_JAR_PATH
else
    export CLASSPATH=$CLASSPATH:$FLEXGP_JAR_PATH
fi

if [ -z $GATEWAY_NODE ]; then
# get library
    chmod 0600 $HOME/$CERT.pem
    echo -n "other ips "
    cat $HOME/info.txt
    echo
    ADDRESS=$(cat $HOME/info.txt | rev | cut -d " " -f 1 | rev)
    echo "Getting rest of stuff from parent at $ADDRESS"
    sudo scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i $HOME/$CERT.pem $USER@$ADDRESS:"$FLEXGP_JAR_PATH" $HOME
    if [ ! -z "$SOCKET_STRESS_JAR" ]; then
	sudo scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i $HOME/$CERT.pem $USER@$ADDRESS:"$SOCKET_STRESS_JAR" $HOME
    fi
    
    ## SCP IN ORIGINAL VERSION ##
    sudo scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i $HOME/$CERT.pem $USER@$ADDRESS:$HOME/start_nodes.txt $HOME
    sudo scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i $HOME/$CERT.pem $USER@$ADDRESS:$HOME/select_data.py $HOME
    sudo scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i $HOME/$CERT.pem $USER@$ADDRESS:$HOME/split_files.py $HOME
    sudo scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i $HOME/$CERT.pem $USER@$ADDRESS:$HOME/evogpj_config.py $HOME
    sudo scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i $HOME/$CERT.pem $USER@$ADDRESS:$HOME/problem.properties $HOME
    sudo scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i $HOME/$CERT.pem $USER@$ADDRESS:$HOME/data.* $HOME
    sudo scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i $HOME/$CERT.pem $USER@$ADDRESS:$HOME/train.* $HOME
    sudo scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i $HOME/$CERT.pem $USER@$ADDRESS:$HOME/func.txt $HOME
    sudo scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i $HOME/$CERT.pem $USER@$ADDRESS:$HOME/term.txt $HOME
    sudo scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i $HOME/$CERT.pem $USER@$ADDRESS:$HOME/norm.txt $HOME
    sudo scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i $HOME/$CERT.pem $USER@$ADDRESS:$HOME/options.txt $HOME
    sudo scp -oStrictHostKeyChecking=no -oCheckHostIP=no -r -i $HOME/$CERT.pem $USER@$ADDRESS:$HOME/certs $HOME/
    sudo scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i $HOME/$CERT.pem $USER@$ADDRESS:$HOME/libsigar-amd64-linux.so $HOME
    # again!
    sudo scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i $HOME/$CERT.pem $USER@$ADDRESS:"$FLEXGP_JAR_PATH" $HOME
fi

if [ -e $HOME/certs/exportCredentials.sh ]; then
    echo "attempting to source"
    # change permissions in the certs dir for easy transport
    sudo chmod -R 755 $HOME/certs
    . $HOME/certs/exportCredentials.sh
else
    echo "can't source non-existant file!"
    ls -lhA $HOME/certs/
fi

# Gather information for starting up other instances
echo "Getting my own address"
#IP=$(curl --retry 3 --retry-delay 10 http://169.254.169.254/latest/meta-data/local-ipv4)
IP=$(ifconfig eth0 | grep inet | grep -v inet6 | cut -d':' -f2 | cut -d' ' -f1)
if [ -z $GATEWAY_NODE ]; then
    OTHER_IPS=$(cat $HOME/info.txt)
    echo -n " $IP" >> $HOME/info.txt
else
    echo -n $IP > info.txt
fi
echo "Our address is $IP"

NUM_NODES=$(cat $HOME/nodes.txt)
NUM_NODES=$(($NUM_NODES - 1))

# the default training/validation split (% of this node's data which is used for training)
NUM_TRAIN=0.80
FAN_OUT=5
MIGRATION=false
SUBNET_ENABLED=false
NUM_SUBNET_NODES=

if [ -e $HOME/options.txt ]; then
    LL=$(cat $HOME/options.txt)
    #VAL=$(echo "$LL" | sed 's/\s\+[^k]=[^:\s]*://g' | sed 's/.*k=\([^:\s]\+\):.*/\1/')
    VAL=$(echo $LL | grep -oE "k=[0-9\.]*" | cut -c3- )
    if [ ! -z $VAL ]; then
	FAN_OUT=$VAL
    fi
    #VAL=$(echo "$LL" | sed 's/\s\+[^s]=[^:\s]*://g' | sed 's/.*s=\([^:\s]\+\):.*/\1/')
    VAL=$(echo $LL | grep -oE "s=[0-9\.]*" | cut -c3- )
    if [ ! -z $VAL ]; then
	NUM_TRAIN=$VAL
    fi
    #VAL=$(echo "$LL" | sed 's/\s\+[^m]=[^:\s]*://g' | sed 's/.*m=\([^:\s]\+\):.*/\1/')
    VAL=$(echo $LL | grep -oE "m=[A-Za-z]*" | cut -c3- )
    if [ ! -z $VAL ]; then
	MIGRATION=$VAL
    fi
    #VAL=$(echo "$LL" | sed 's/\s\+[^i]=[^:\s]*://g' | sed 's/.*i=\([^:\s]\+\):.*/\1/')
    VAL=$(echo $LL | grep -oE "i=[0-9A-Za-z_\.]*" | cut -c3- )
    if [ ! -z $VAL ]; then ## if NUM_SUBNET_NODES given here, second tier is enabled
	SUBNET_ENABLED=true
	NUM_SUBNET_NODES=$VAL;
	NUM_SUBNET_NODES=$(($NUM_SUBNET_NODES - 1))
    fi
fi
echo $MIGRATION

# data file may have different file extensions, so be extension agnostic
DATA_FILE=$(ls $HOME | grep -E "^data\.")
if [[ -n $DATA_FILE ]]; then
    #$HOME/select_data.py -i $HOME/data.txt -n 3000 -o $HOME/data.tmp.txt
    EXT=$(echo $HOME/$DATA_FILE | sed 's/.*\.\(.*\)/\1/')
    rm -f $HOME/train*
    shuf -o $HOME/data.tmp $HOME/$DATA_FILE
    mv $HOME/data.tmp $HOME/$DATA_FILE
    #cp /msd/AdditionalFiles/flexgp.db $HOME/
    if [ $(echo "$NUM_TRAIN < 1.0" | bc) -eq 1 ]; then
	$HOME/split_files.py -d $HOME/flexgp.db $HOME/$DATA_FILE $NUM_TRAIN $HOME/train.$EXT $HOME/validation.$EXT
    else # no validation data
	cp $HOME/$DATA_FILE $HOME/train.$EXT
    fi
fi

CONFIG_ARGS=
if [ -e $HOME/func.txt ]; then
    CONFIG_ARGS="-f $HOME/func.txt $CONFIG_ARGS"
fi
if [ -e $HOME/term.txt ]; then
    CONFIG_ARGS="-t $HOME/term.txt $CONFIG_ARGS"
fi
if [ -e $HOME/norm.txt ]; then
    CONFIG_ARGS="-n $HOME/norm.txt $CONFIG_ARGS"
fi
if [ -n "$CONFIG_ARGS" ]; then
    echo " sudo $HOME/evogpj_config.py $HOME/problem.properties $CONFIG_ARGS"
    $HOME/evogpj_config.py $HOME/problem.properties $CONFIG_ARGS
fi


i=$FAN_OUT
NODES_LEFT=$NUM_NODES
# USER_START_NAME=user_start_combined.sh
if [ $NODES_LEFT -lt 1 ]; then
    echo "No more nodes to start!"
elif [ $NODES_LEFT -le $FAN_OUT ]; then
    while [ $NODES_LEFT -gt 0 ]; do
	# only need to start up 1,2 or 3 nodes
	echo "starting last few nodes!"
	echo 0 > $HOME/nodes.txt

	echo "starting 1 new last layer instances"
	echo "starting 1 new last layer instances" >> new_instances.log
	INSTANCE=$($RUN_CMD -k $CERT -t $TYPE -n 1 $AMI | grep INSTANCE | cut -f2)


	echo -n "Waiting for last layer node to come up "
	STATUS=$($DESCRIBE_CMD | grep $INSTANCE | cut -f6)
	while [ "$STATUS" = "pending" ]; do
	    echo -n "."
    	    sleep 1
	    STATUS=$($DESCRIBE_CMD | grep $INSTANCE | cut -f6)
	done
	echo " last layer node up!"
	if [ "$STATUS" != "running" ]; then
	    echo "last layer node not running, $STATUS"
	    exit 1
	fi

	ADDRESS=$($DESCRIBE_CMD | grep $INSTANCE | cut -f17)
	echo "Last layer instance started, with id $INSTANCE, with address $ADDRESS"


	## SEND VIA SCP INSTEAD OF --user_data: start.sh, user_vars.sh, nodes.txt, info.txt, key.pem, part_handler.py
	chmod 0600 $CERT.pem
	scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i $CERT.pem $START_SCRIPT_NAME $USER@$ADDRESS:~/
	while [ $? -ne 0 ]; do
	    sleep 4
    	    echo "trying again"
	    scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i $CERT.pem $START_SCRIPT_NAME $USER@$ADDRESS:~/
	done
	scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i $CERT.pem $USER_VARS_SCRIPT_PATH $USER@$ADDRESS:~/
	scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i $CERT.pem nodes.txt $USER@$ADDRESS:~/
	scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i $CERT.pem info.txt $USER@$ADDRESS:~/
	scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i $CERT.pem $CERT.pem $USER@$ADDRESS:~/
	scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i $CERT.pem $PART_HANDLER_NAME $USER@$ADDRESS:~/

	## RUN start.sh on remote node ##
	echo "run start.sh on remote node"
	#ssh -oStrictHostKeyChecking=no -oCheckHostIP=no -i $CERT.pem $USER@$ADDRESS "bash $START_SCRIPT_NAME 0"
    	ssh -oStrictHostKeyChecking=no -oCheckHostIP=no -i $CERT.pem $USER@$ADDRESS "bash $START_SCRIPT_NAME &> start.log &"
	echo "done"
	NODES_LEFT=$(($NODES_LEFT - 1))
#write-mime-multipart -z -o $HOME/multi.txt.gz $HOME/$PART_HANDLER_NAME:text/part-handler $HOME/$PART_HANDLER_NAME:text/plain $USER_VARS_SCRIPT_PATH:text/plain $HOME/$CERT.pem:text/plain $HOME/info.txt:text/plain $HOME/nodes.txt:text/plain $HOME/$START_SCRIPT_NAME:text/x-shellscript $HOME/$START_SCRIPT_NAME:text/plain
    done
else
    nts=$(($NUM_NODES / $FAN_OUT))
    while [ $NODES_LEFT -gt 0 ]; do
	
	if [ $NODES_LEFT -ge $FAN_OUT ]; then
	    echo $nts > $HOME/nodes.txt
	    NODES_LEFT=$(($NODES_LEFT - $nts))
	    echo "starting 1 node, telling it to start $nts nodes"
	    echo "starting 1 inner layer  node, telling it to start $nts nodes" >> new_instances.log
	else
            echo $NODES_LEFT > $HOME/nodes.txt
	    NODES_LEFT=0
	    echo "starting 1 node, telling it to start $NODES_LEFT nodes"
	    echo "starting 1 inner layer  node, telling it to start $NODES_LEFT nodes" >> new_instances.log
	fi
	
	INSTANCE=$($RUN_CMD -k $CERT -t $TYPE -n 1 $AMI | grep INSTANCE | cut -f2)

	echo -n "Waiting for inner layer node to come up "
	STATUS=$($DESCRIBE_CMD | grep $INSTANCE | cut -f6)
	while [ "$STATUS" = "pending" ]; do
	    echo -n "."
    	    sleep 1
	    STATUS=$($DESCRIBE_CMD | grep $INSTANCE | cut -f6)
	done
	echo " inner layer node up!"
	if [ "$STATUS" != "running" ]; then
	    echo "inner layer node not running, $STATUS"
	    exit 1
	fi
	ADDRESS=$($DESCRIBE_CMD | grep $INSTANCE | cut -f17)
	echo "Inner layer instance started, with id $INSTANCE, with address $ADDRESS"


	## SEND VIA SCP INSTEAD OF --user_data: start.sh, user_vars.sh, nodes.txt, info.txt, key.pem, part_handler.py
	chmod 0600 $CERT.pem
	scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i $CERT.pem $START_SCRIPT_NAME $USER@$ADDRESS:~/
	while [ $? -ne 0 ]; do
	    sleep 4
    	    echo "trying again"
	    scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i $CERT.pem $START_SCRIPT_NAME $USER@$ADDRESS:~/
	done
	scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i $CERT.pem $USER_VARS_SCRIPT_PATH $USER@$ADDRESS:~/
	scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i $CERT.pem nodes.txt $USER@$ADDRESS:~/
	scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i $CERT.pem info.txt $USER@$ADDRESS:~/
	scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i $CERT.pem $CERT.pem $USER@$ADDRESS:~/
	scp -oStrictHostKeyChecking=no -oCheckHostIP=no -i $CERT.pem $PART_HANDLER_NAME $USER@$ADDRESS:~/

	## RUN start.sh on remote node ##
	echo "run start.sh on remote node"
	#ssh -oStrictHostKeyChecking=no -oCheckHostIP=no -i $CERT.pem $USER@$ADDRESS "bash $START_SCRIPT_NAME 0"
	ssh -oStrictHostKeyChecking=no -oCheckHostIP=no -i $CERT.pem $USER@$ADDRESS "bash $START_SCRIPT_NAME &> start.log &"
	echo "done"
#	echo "Writing mime-multipart"
#	write-mime-multipart -z -o $HOME/multi_$i.txt.gz $HOME/$PART_HANDLER_NAME:text/part-handler $HOME/$PART_HANDLER_NAME:text/plain $USER_VARS_SCRIPT_PATH:text/plain $HOME/$CERT.pem:text/plain $HOME/info.txt:text/plain $HOME/nodes.txt:text/plain $HOME/$START_SCRIPT_NAME:text/x-shellscript $HOME/$START_SCRIPT_NAME:text/plain
#	$RUN_CMD -k $CERT -n 1 $AMI -t $TYPE -f $HOME/multi_$i.txt.gz >> new_instances.log &
    done
fi


## handling the second-tier
SUBNET_LABEL=
if $SUBNET_ENABLED; then
    if [ ! -e $HOME/subnet_label.txt ]; then
	## pick new 32-char subnet label
	SUBNET_LABEL=$(< /dev/urandom tr -dc _A-Z-a-z-0-9 | head -c32)
	echo $SUBNET_LABEL > $HOME/subnet_label.txt
	if [ $NUM_SUBNET_NODES -gt 0 ]; then
	    echo "Starting $NUM_SUBNET_NODES more nodes under subnet ID $SUBNET_LABEL"
	    echo "Writing mime-multipart"
	    write-mime-multipart -z -o $HOME/multi-subnet.txt.gz $HOME/$PART_HANDLER_NAME:text/part-handler $HOME/$PART_HANDLER_NAME:text/plain $USER_VARS_SCRIPT_PATH:text/plain $HOME/$CERT.pem:text/plain $HOME/info.txt:text/plain $HOME/nodes.txt:text/plain $HOME/subnet_label.txt:text/plain $HOME/$START_SCRIPT_NAME:text/x-shellscript
	    echo "starting $NUM_SUBNET_NODES new second-tier instances"
	    $RUN_CMD -k $CERT -n $NUM_SUBNET_NODES $AMI -t $TYPE -f $HOME/multi-subnet.txt.gz >> new_instances.log &
	else
	    echo "No second-tier nodes to start."
	fi
    else ## subnet id exists, load it
	SUBNET_LABEL=$(cat $HOME/subnet_label.txt)
	echo "Loaded subnet ID of $SUBNET_LABEL"
	## don't need to start any more nodes here
    fi
else
    echo "Subnet disabled"
fi

# Finally, run the program!
echo "running library"
TRAIN_FILE=$(ls $HOME | grep train)
#ARGS="-Djava.library.path=/msd/lib/ext:/msd/lib/linux:/msd/lib:/home/ubuntu/ -Xmx$(($(cat /proc/meminfo | grep MemTotal | sed 's/MemTotal:[ ]*\([0-9]\+\).*/\1/')*8/10))k -jar $FLEXGP_JAR_PATH"
ARGS="-Djava.library.path=/home/ubuntu/ -Xmx$(($(cat /proc/meminfo | grep MemTotal | sed 's/MemTotal:[ ]*\([0-9]\+\).*/\1/')*8/10))k -jar $FLEXGP_JAR_PATH"

if [ ! -z $SUBNET_LABEL ]; then
    ## if SUBNET_LABEL is non-empty
    ARGS="$ARGS -s $SUBNET_LABEL"
fi


ARGS="$ARGS --migration $MIGRATION -i $IP -t $(cat $HOME/start_nodes.txt)"
if [ -z $GATEWAY_NODE ]; then
	ARGS="$ARGS -n $OTHER_IPS"
fi
if [ -e $HOME/problem.properties ]; then
	if [[ -n $TRAIN_FILE ]]; then
		echo "java $ARGS --args $HOME/problem.properties $HOME/$TRAIN_FILE"
		java $ARGS --args $HOME/problem.properties $HOME/$TRAIN_FILE
	else
		echo "java $ARGS --args $HOME/problem.properties"
		java $ARGS --args $HOME/problem.properties
	fi
else
	# if errors here, try removing &
	echo "java $ARGS &"
	java $ARGS &
fi
#fi
