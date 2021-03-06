package com.avalon.server;

import java.net.*;
import java.io.*;
import java.util.*;
import java.sql.*;

import com.avalon.client.entry.MessageEntry;
import com.avalon.client.entry.RoomEntry;
import com.avalon.client.entry.UserEntry;
import com.avalon.client.social.FBManager;
import com.avalon.packet.CPacket;
import com.avalon.server.pool.Database;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class ServerMgr {
		
	   ArrayList<ServerThread> ClientList;
	   
	   ArrayList<UserEntry> users = new ArrayList<UserEntry>();
	   ArrayList<RoomEntry> rooms = new ArrayList<RoomEntry>(); // 서버 방 목록
	   
	   static DBMgr dbManager = new DBMgr();
	   
	   ObjectMapper om = new ObjectMapper();
	   

	   public ServerMgr() {
	      ClientList = new ArrayList<ServerThread>();
	      this.getAllUser();
	   }

	   public void addClient(ServerThread client) {
	      ClientList.add(client);
	   }

	   public void removeClient(ServerThread client) {
		   for(UserEntry user : users) {
			   if(user.getId().equals(client.id)){
				   users.remove(user);
				   break;
			   }
		   }
		   
		   for(RoomEntry room : rooms){
			   if(room.getOwner().equals(client.id)){
				  rooms.remove(room); 
			   }
		   }
		   
		   
	      ClientList.remove(client);
	      
	      try {
	    	  	this.getAllUser();
	    	  	this.getUserScore();
				this.getUsers();
				this.getRooms();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	   }

	   public void sendUserChat(CPacket tpkt) throws IOException
	   {
		   System.out.println("MODE-CHATTING");
		   for(ServerThread thd : ClientList){
			   
		   }

		   System.out.println("쪽지보내기 실패!");
	   }
	   
	   // 사용자 접속 통지 및 사용자 저장.
	   
	   public void addUser(CPacket tpkt) throws IOException {
		   
		   
		   boolean flag = false;
		   UserEntry tuser = new UserEntry();
		   System.out.println("TOKEN : "+tpkt.getToken());
		   tuser.setACCESS_KEY(tpkt.getToken());
		   tuser.setFromJSON2(tpkt.getMessage());
		   
		   for(UserEntry user : users) {
			   if(user.getId().equals(tuser.getId())){
				   flag = true;
				   user.setState("접속중");
				   break;
			   }
		   }
		   if(!flag){
			   System.out.println("[ADD USER : "+tuser.getName()+" ]");
			   this.users.add(tuser);
		   }
		   
		   // 사용자 정보 update
		   if(dbManager.isUser(tuser) == 0) {
			   dbManager.insertUser(tuser);
		   }else{
			   dbManager.updateUserInfo(tuser);
			   getUserScore();
		   }
		   
		   tpkt.setMessage(tuser.getJSON2());
		   tpkt.setTask_type("RECV");
		   
		   for(ServerThread thd : ClientList){
			   if(thd.id.equals(tuser.getId())){
				   thd.sendMessage(tpkt);
			   }
		   }
		   getRooms();
	   }
	   
	   
	   
	   // 사용자 score 업데이트.
	   public void getAllUser() {
		   this.users = dbManager.getAllUser();
	   }
	   public void getUserScore() {
		   ArrayList<UserEntry> tusers = dbManager.getAllUser();
		   for(UserEntry user : users){
			   for(UserEntry tuser : tusers){
				   if(user.getId().equals(tuser.getId())){
					   user.setWin(tuser.getWin());
					   user.setLose(tuser.getLose());
					   break;
				   }
			   }
			   for(ServerThread thd : ClientList){
				   if(thd.id.equals(user.getId())){
					   user.setState("대기중");
				   }
			   }
		   }
	   }
	// 사용자 접속 통지
	   public void getUsers() throws IOException {
		   CPacket tpkt = new CPacket("","RECV","USERS","GET","");
		   getUserScore();
		   for(UserEntry user : users) {
			   
			   tpkt.setOmessage(user.getJSON2());
			   
		   }
		   for(ServerThread thd : ClientList){
			   thd.sendMessage(tpkt);
	       
	      }
	   }
	   
 // 사용자 접속 통지 및 사용자 저장.
	   
	   public void makeRoom(CPacket tpkt) throws IOException {
		   
		   
		   boolean flag = false;
		   RoomEntry troom = new RoomEntry();
		   troom.setFromJSON(tpkt.getMessage());
		   for(RoomEntry room : rooms) {
			   if(room.getOwner().equals(troom.getOwner())){
				   flag = true;
				   break;
			   }
		   }
		   
		   if(!flag){
			   System.out.println("[ADD ROOMS : "+troom.getTitle()+" ]");
			   int rnum = 0;
			   for(RoomEntry room : rooms){
				   rnum = room.getNum();
			   }
			   
			   troom.setNum(rnum+1);
			   this.rooms.add(troom);
			   
		   }
		   
		   // 사용자 정보 update
		   updateUserState(troom.me,"게임중");
		   
		   // 패킷 설정
		   tpkt.setTask_type("RECV");
		   tpkt.setMessage(troom.getJSON());
		   
		   for(ServerThread thd : ClientList){
			   if(thd.id.equals(troom.getOwner())){
				   thd.sendMessage(tpkt);
			   }
			
	      }
	   }
	   
// 사용자 접속 통지 및 사용자 저장.
	   
	   public void delRoom(CPacket tpkt) throws IOException {
		   
		   
		   boolean flag = false;
		   RoomEntry troom = new RoomEntry();
		   troom.setFromJSON(tpkt.getMessage());
		// 사용자 정보 update
		   updateUserState(troom.me,"대기중");
		   updateUserState(troom.enemy,"대기중");
		   
		   for(RoomEntry room : rooms) {
			   if(room.getNum() == troom.getNum()){
				   rooms.remove(room);
				   //this.rooms.remove(room);
				   break;
			   }
		   }
		// 패킷 설정
		   tpkt.setTask_type("RECV");
		   tpkt.setMessage(troom.getJSON());
		   
		   for(ServerThread thd : ClientList){
			   if(thd.id.equals(tpkt.getId())){
				   thd.sendMessage(tpkt);
			   }
			
	      }
		   
		   getRooms();
		   
		   
		   
	   }
	   
// 사용자 삭제
	   
	   public void delUser(CPacket tpkt) throws IOException {
		   
		   
		   boolean flag = false;
		   UserEntry tuser = new UserEntry();
		   
		   tuser.setFromJSON2(tpkt.getMessage());
		   
		   tpkt.setTask_type("RECV");
		   
		   for(ServerThread thd : ClientList){
			   if(thd.id.equals(tuser.getId())){
				   thd.sendMessage(tpkt);
				   this.removeClient(thd);
				   break;
			   }
		   }
		   
		// 패킷 설정

		  getUsers();
		   
		   
		   
	   }
	   
	   public void updateUserState(UserEntry tuser,String state) {
		   for(UserEntry user : users) {
			   if(user.getId().equals(tuser.getId())){
				   user.setState(state);
				   break;
			   }
		   }
		   try {
			getUsers();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	   }
	   
	// 방 목록 Get
	   public void getRooms() throws IOException {
		   CPacket tpkt = new CPacket("","RECV","ROOMS","GET","");
		   if(rooms.size() != 0){
			   
			   for(RoomEntry room : rooms) {
				   
				   tpkt.setOmessage(room.getJSON());
				   
			   }
			   
			   for(ServerThread thd : ClientList){
				   
				   
				   thd.sendMessage(tpkt);
		       
		      }
		   }
	   }
	   
	   public void sendMsg(CPacket tpkt) {
		   MessageEntry tmsg = new MessageEntry();
		   try {
			tmsg.setFromJSON(tpkt.getMessage());
			// 패킷 설정
			   tpkt.setTask_type("RECV");
			   tpkt.setMessage(tmsg.getJSON());
			   
			   for(ServerThread thd : ClientList){
				   if(thd.id.equals(tmsg.getTo_id()) || thd.id.equals(tmsg.getFrom_id())){
					   thd.sendMessage(tpkt);
				   }
		      }
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		   
		   
	   }
	   
	   public void joinRoom(CPacket tpkt) {
		   
		   HashMap<String, String> m;
		   String jsonStr = tpkt.getMessage();
		   UserEntry tuser = new UserEntry();
		   RoomEntry troom = new RoomEntry();
		   
		   try {
			m = om.readValue(jsonStr, new TypeReference<HashMap<String, String>>(){});
			tuser.setFromJSON2(m.get("USER"));
			troom.setFromJSON(m.get("ROOM"));
			
			for(RoomEntry room : rooms){
				if(room.getNum() == troom.getNum()){
					room.setEnemy(tuser);
					break;
				}
			}
			

			   // 사용자 정보 update
			   updateUserState(troom.enemy,"게임중");
			   
			   // 패킷 설정
			   tpkt.setTask_type("RECV");
			   tpkt.setMessage(troom.getJSON());
			   
			   for(ServerThread thd : ClientList){
				   if(thd.id.equals(troom.me.getId()) || thd.id.equals(troom.enemy.getId())){
					   thd.sendMessage(tpkt);
				   }
		      }
			
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		   
			
	   }

	   public static void main(String[] args) {
	      ServerMgr server;
	      ServerSocket serverSocket = null;
	      int port = 3333;
	      boolean listening = true;

	      server = new ServerMgr();

	      try {
	         serverSocket = new ServerSocket(port);
	      } catch(IOException e) {
	         System.err.println("연결 실패입니다.");
	         System.exit(-1);
	      }

	        System.out.println("서버 ["+serverSocket.getInetAddress()+"]에서 연결을 기다립니다.");

	      try {
	         while(listening) {
	            ServerThread thread;

	            thread = new ServerThread(server, serverSocket.accept());
	            thread.start();

	            server.addClient(thread);
	         }

	         serverSocket.close();
	      } catch(IOException e) {
	      }

	      System.out.println("서버를 종료합니다.");
	   }
	   

	}

	class ServerThread extends Thread {
	   private Database db;
	   private ServerMgr Server;
	   private Socket Socket;

	   PrintWriter streamOut;
	   BufferedReader streamIn;
	   
	   ObjectOutputStream ToClient;
	   ObjectInputStream FromClient;
	   
	   String id,pw,ip;
	   CPacket pkt;
	   
	   
       
	   public ServerThread(ServerMgr server, Socket socket) {
	      super("Echo Service Thread");

	      Server = server;
	      Socket = socket;

	      streamOut = null;
	      streamIn = null;
	   }

	   public void sendMessage(CPacket tpkt) throws IOException {
	      if(ToClient != null) {
	    	  ToClient.writeObject(tpkt);
	    	  ToClient.flush();
	      }
	   }

	   public void run() {
	      try {
	         System.out.println("["+Socket.getInetAddress()+"]에서 접속하였습니다.");
	         

	         //streamOut = new PrintWriter(Socket.getOutputStream(), true);
	         //streamIn = new BufferedReader(new InputStreamReader(Socket.getInputStream()));
	         
	         ToClient = new ObjectOutputStream(Socket.getOutputStream());
	         FromClient = new ObjectInputStream(Socket.getInputStream());
	         
	         try {
				while(true) {
					pkt = (CPacket)FromClient.readObject();
					if(pkt != null){
						System.out.println("PACKET_ACCEPT");
						 if(pkt.getTask_title().equals("LOGIN")) {
                             System.out.println("LOGIN_MODE");
                             this.id = pkt.getId();
                             this.ip = pkt.getIp();
                             Server.addUser(pkt);
                             Server.getUsers();
                         }else if(pkt.getTask_title().equals("CHAT")) {
                             Server.sendMsg(pkt);
                         }else if(pkt.getTask_title().equals("MAKEROOM")) {
                             Server.makeRoom(pkt);
                             Server.getRooms();
                         }else if(pkt.getTask_title().equals("ENTERROOM")) {
                             Server.joinRoom(pkt);
                             Server.getRooms();
                         }else if(pkt.getTask_title().equals("DELROOM")) {
                             Server.delRoom(pkt);
                         }else if(pkt.getTask_title().equals("DELUSER")) {
                             Server.delUser(pkt);
                         }

					}
					
					try {
						 this.sleep(1000L);
						 System.out.println("유저수 : "+Server.users.size());
						 System.out.println("방수 : "+Server.rooms.size());
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				    
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	         //FromClient.close();
	         //ToClient.close();
	         //Socket.close();
	      } catch(IOException e) {
	         Server.removeClient(this);
	         
	         System.out.println("["+ Socket.getInetAddress() + "]의 접속이 끊겼습니다.");
	      } 
	   }
	}
