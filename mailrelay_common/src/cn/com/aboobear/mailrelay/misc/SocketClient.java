package cn.com.aboobear.mailrelay.misc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Date;

public class SocketClient {
	
	
	private Socket clientSocket;
	
	private InetSocketAddress tcpAddress;
	
	private int timeOut = 1000;
	
	private OutputStream out;
	
	private InputStream in;
	
	private Date sendTime;
	
	private final int receiveMaxSize = 1024 * 1024;
	
	private long clientID = -1;
	
	public SocketClient(String ip, int port)
	{
		tcpAddress = new InetSocketAddress(ip,port);
	}
	
	public void setTimeOut(int tm)
	{
		timeOut = tm;
	}
	
	
	public boolean connect()
	{
		try
		{
			if(clientSocket != null && clientSocket.isConnected()) 
			{
				out.close();
				in.close();
				clientSocket.close();
				clientSocket = null;
			}
			clientSocket = new Socket();
			clientSocket.connect(tcpAddress);
			
			if( clientSocket.isConnected() )
			{
				clientSocket.setSoTimeout(timeOut);
				out = clientSocket.getOutputStream();
				in = clientSocket.getInputStream();
				return true;
			}
		}
		catch(IOException ex)
		{
		}
		clientSocket = null;
		out = null;
		in = null;
		return false;
	}
	
	public void send(String sendString, String charset) throws Exception
	{
		
		byte[] datas = sendString.getBytes(charset);
		send(datas);
	}
	
	
	public void send(byte[] datas) throws Exception
	{
		if( null == clientSocket || clientSocket.isClosed()) 
		{
			throw new Exception("socket closed!");
		}
		out.write(datas);
		out.flush();
		sendTime = new Date(System.currentTimeMillis());
	}
	
	public String receive(String charset) throws Exception
	{
		byte[] receiveData = receive();
		if (receiveData == null)
		{
			return null;
		}
		
		String sData = new String(receiveData, charset);
		return sData.trim();
	}
	
	public byte[] receive() throws Exception
	{
		if( null == clientSocket || clientSocket.isClosed()) 
		{
			throw new Exception("socket closed!");
		}
		byte[] bufIn = new byte[receiveMaxSize];
		Thread.sleep(1000);
		int bytesLen = in.available();
		int totalCount = 0;
		
		while( bytesLen > 0)
		{
			
			try
			{
				bytesLen = in.read(bufIn, totalCount, bytesLen);
				totalCount += bytesLen;
				bytesLen = in.available();
			}catch(SocketTimeoutException e)
			{
				break;
			}
			
		}
		
		byte[] stores = new byte[totalCount];
		
		System.arraycopy(bufIn, 0, stores, 0, totalCount);
		return stores;
	}


	public Date getSendTime() {
		return sendTime;
	}
	
	public void close()
	{
		try {
			clientSocket.close();
			in.close();
			out.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public long getClientID() {
		return clientID;
	}

	public void setClientID(long clientID) {
		this.clientID = clientID;
	}
	
	
	public InputStream getInputStream()
	{
		return in;
	}
	
	
	public OutputStream getOutputStream()
	{
		return out;
	}
}