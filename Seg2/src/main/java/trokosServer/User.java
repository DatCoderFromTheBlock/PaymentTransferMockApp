import java.io.*;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class User {
	private String clientID;
	private float balance;
	private String password;
	private List<Group> groups;
	private List<Group> own;
	private String FILE = "users.txt";

	public User(String clientID) {
		this.clientID = clientID;

		if(exists()) {
			initializeBal();
		} else {
			this.balance = 100;
		}
	}
	
	public Boolean exists() {

		Scanner userFileReader;
		String[] line;

		try {
			File file = new File(FILE);
			userFileReader = new Scanner(file);

			while(userFileReader.hasNextLine()) {
				line = userFileReader.nextLine().split(":");
				if (line[0].equals(clientID)) {
					userFileReader.close();
					return true;
				}
			}
			userFileReader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}

	public String getRequests() {
		String[] requests;
		String result = "";
		
		try {
			Scanner userFileReader = new Scanner(new File("requests.txt"));
			while(userFileReader.hasNextLine()) {
				requests = userFileReader.nextLine().split(":");
				if (requests[2].equals(clientID) && requests[4].equals("PENDING")) {
					result = result + "Request ID: " + requests[0] + " - From: " + requests[1] + " - Amount: " + requests[3] + "\n";
				}
			}
			userFileReader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		if (result.isEmpty())
			return "You have no payment requests";
		return result;
	}

	public String getID(){
		return this.clientID;
	}

	public float getBal(){
		initializeBal();
		return this.balance;
	}
	
	private void initializeBal() {
		Scanner userFileReader;
		String[] line;

		try {
			userFileReader = new Scanner(new File("balance.txt"));

			while(userFileReader.hasNextLine()) {
				line = userFileReader.nextLine().split(":");
				if (line[0].equals(clientID)) {
					this.balance = Float.parseFloat(line[1]);
				}
			}
			
			userFileReader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void updateBal(float balance){
		Scanner balanceFileReader;
		String line;
		StringBuffer inputBuffer = new StringBuffer();
		String fileContents;
		FileWriter balanceFileWriter;
		try {
			balanceFileReader = new Scanner(new File("balance.txt"));
			while(balanceFileReader.hasNextLine()) {
				line = balanceFileReader.nextLine();
				inputBuffer.append(line);
				inputBuffer.append("\n");
			}

			fileContents = inputBuffer.toString().replaceAll(clientID + ":" + String.valueOf(this.balance), clientID + ":" + balance);

			balanceFileWriter = new FileWriter("balance.txt");
			balanceFileWriter.write(fileContents);

			balanceFileWriter.close();
			balanceFileReader.close();


		} catch (IOException e) {
			e.printStackTrace();
		}
		this.balance = balance;
	}
	
	public Boolean checkPassword() {
		Scanner userFileReader;
		String[] line;

		try {
			userFileReader = new Scanner(new File("users.txt"));

			while(userFileReader.hasNextLine()) {
				line = userFileReader.nextLine().split(":");
				if (line[0].equals(clientID)) {
					if(line[1].equals(password)) {
						userFileReader.close();
						return true;
					}
				}
			}

			userFileReader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}

	public String payRequest(String reqID) {
		String payRequest = "Request does not exist";
		Scanner requestsFileReader;
		String[] line;
		try{
			requestsFileReader = new Scanner(new File("requests.txt"));
			while(requestsFileReader.hasNextLine()){
				line = requestsFileReader.nextLine().split(":");
				if(reqID.equals(line[0])){
					if(Float.parseFloat(line[3]) <= this.balance){
						if(line[2].equals(this.clientID)){
							payRequest = "Payment successful";
							updateBal(this.balance - Float.parseFloat(line[3]));
						} else {
							payRequest = "Request is from another client";
						}
					} else{
						payRequest = "Insufficient funds";
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		return payRequest;
	}

	public String groups() {
		String[] groups;
		String resultOwner = "";
		String resultMember = "";
		String result = "";
		
		try {
			Scanner groupFileReader = new Scanner(new File("groups.txt"));
			while(groupFileReader.hasNextLine()) {
				groups = groupFileReader.nextLine().split(":");
				if (groups[1].equals(clientID)) {
					resultOwner = resultOwner + "Group ID: " + groups[0] + "\n";
				}
				for(int i = 2; i < groups.length; i++) {
					if(groups[i].equals(clientID))
						resultMember = resultMember + "Group ID: " + groups[0] + "\n";
				}
			}
			groupFileReader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		
		if (resultOwner.isEmpty())
			result = result + "You are not an owner of any group\n";
		else {
			result = result + "Groups you are an owner of:\n";
			result = result + resultOwner;
		}

		if (resultMember.isEmpty())
			result = result + "You do not belong to any group\n";
		else {
			result = result + "Groups you are a member of:\n";
			result = result + resultMember;
		}
			
		return result;
	}
	
	//public void putInFile(Cypher cypher) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IOException {
	//	cypher.encrypt(new File("user.cif"), clientID);
	//}

}