/**
 * Copyright (c) 2012 Evolutionary Design and Optimization Group
 * 
 * Licensed under the MIT License.
 * 
 * See the "LICENSE" file for a copy of the license.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.  
 *
 */
package network;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.sql.Timestamp;

import node.Mailbox;
import node.NodeList;
import node.NodeListEntry;
import utility.MsgID;

/**
 * The parent of all message types. Implements several utilities.
 * @author Dylan Sherry
 *
 */
public abstract class AbstractMessage implements Message {
	private static final long serialVersionUID = -704333880928760788L;
	// a send timeout value, in seconds
	public static final int SEND_TIMEOUT = 45;
	
	// the id of the source node
	public final String srcID;
	// the message's id
	public final MsgID msgID;
	// the message type, set through MessageType
	public final int msgType;

	// the srcIP and srcPort, used internally for received messages
	// note: srcIP and srcPort are transmitted over the network
	public String srcIP;
	public int srcPort;
	// the subnet ID of the src node
	public String srcSubnetID;
	// time this packet was created
	public Timestamp time;

	/**
	 * The default constructor for a message
	 * 
	 * @param _msgID
	 * @param _srcID
         * @param _srcIP
	 * @param _srcPort
	 * @param _srcSubnetID
	 * @param _msgType
	 */
	public AbstractMessage(MsgID _msgID, String _srcID, String _srcIP, int _srcPort, String _srcSubnetID, int _msgType) {
		msgID = _msgID;
		srcID = _srcID;
		srcIP = _srcIP;
		srcPort = _srcPort;
		srcSubnetID = _srcSubnetID;
		msgType = _msgType;
		time = NodeList.currTime();
	}

        /**
         * 
         * @return 
         */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((msgID == null) ? 0 : msgID.hashCode());
		result = prime * result + msgType;
		return result;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof AbstractMessage))
			return false;
		AbstractMessage other = (AbstractMessage) obj;
		if (msgID == null) {
			if (other.msgID != null)
				return false;
		} else if (!msgID.equals(other.msgID))
			return false;
		if (msgType != other.msgType)
			return false;
		if (!srcID.equals(other.srcID))
			return false;
		if (!srcSubnetID.equals(other.srcSubnetID))
			return false;
		return true;
	}

    /**
     *
     * @return
     */
    @Override
	public String toString() {
		return "AbstractMessage [msgID=" + msgID + ", msgType=" + msgType
				+ ", srcID=" + srcID + ", srcIP=" + srcIP + ", srcPort="
				+ srcPort + ", srcSubnetID=" + srcSubnetID + ", time=" + time
				+ "]";
	}

	/**
	 * Public method to be called in order to send this message
	 * @param nodelist the master nodelist, for logging success/failure of sending
     * @param destID
	 * @param nle_dest
	 */
	public void scheduleSend(NodeList nodelist, String destID, NodeListEntry nle_dest) {
		MessageSender sw = new MessageSender(nodelist, destID, nle_dest, this, SEND_TIMEOUT);
		sw.start();
		System.out.println("Sent message");
	}

	/**
	 * Send this message through a socket
	 * 
     * @param nle_dest
	 * @throws IOException
     * @throws java.net.SocketTimeoutException
     * @throws java.net.SocketException
     * @throws java.net.ConnectException
     * @throws java.io.StreamCorruptedException
     * @throws java.lang.InterruptedException
	 * @throws UnknownHostException
	 */
	public void send(NodeListEntry nle_dest) throws UnknownHostException,
			SocketTimeoutException, SocketException, ConnectException,
			StreamCorruptedException, InterruptedException, IOException {
		String ip = nle_dest.ip;
		int port = nle_dest.port;
		String destSubnetID = nle_dest.subnetLabel;
		Mailbox.println(String.format("Sending message %s to %s:%d on subnet %s",msgID,ip,port,destSubnetID));
		Socket socket = new Socket();
		InetSocketAddress isa = new InetSocketAddress(ip, port);
		Mailbox.println("Connecting to socket at " + isa.toString());
		socket.connect(isa, 300);
		Mailbox.println("Default socket timeout:" + socket.getSoTimeout());
		// socket.setSoTimeout(100);
		// Mailbox.println("New socket timeout:" + socket.getSoTimeout());

		Mailbox.println("Getting object output stream");
		OutputStream soo = socket.getOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(
				new BufferedOutputStream(soo));
		// make sure we can connect to the address before proceeding
		// send this through the socket
		Mailbox.println("Sending object through socket");

		oos.writeObject(this);
		oos.flush();
		Mailbox.println("Closing socket");
		socket.close();
		Mailbox.println("Message has been sent successfully");
	}

        /**
         * Send message
         * @param m
         * @param destIP
         * @param destPort
         * @throws UnknownHostException
         * @throws IOException 
         */
	public static void sendObject(AbstractMessage m, String destIP, int destPort)
			throws UnknownHostException, IOException {
		Socket socket = new Socket(destIP, destPort);
		ObjectOutputStream oos = new ObjectOutputStream(
				socket.getOutputStream());
		oos.writeObject(m);
		socket.close();
	}	
}
