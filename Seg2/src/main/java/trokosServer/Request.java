import java.io.*;
import java.util.Scanner;
public class Request {
	private int requestID;
	private User clientID;
	private User userID;
	private float amount;
	private Status status;

	public Request(String reqID){
		Scanner requestFileReader;
		String[] line;
		try {
			requestFileReader = new Scanner(new File("requests.txt"));
			while (requestFileReader.hasNextLine()) {
				line = requestFileReader.nextLine().split(":");
				if(line[0].equals(reqID)){
					this.requestID = Integer.parseInt(line[0]);
					this.clientID = new User(line[1]);
					this.userID = new User(line[2]);
					this.amount = Float.parseFloat(line[3]);
					this.status = Status.valueOf(line[4]);
				} 
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Request(User clientID, User userID, float amount) {
		try {
			Scanner idReader = new Scanner(new File("requests.txt"));
			String line = "0";
			while (idReader.hasNextLine()) {
				line = idReader.nextLine();
			}
			if (line == null) {
				this.requestID = 1;
			} else {
				this.requestID = Integer.parseInt(line.split(":")[0]) + 1;
			}
			
			idReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.clientID = clientID;
		this.userID = userID;
		this.amount = amount;
		this.status = Status.PENDING;
	}
	
	public String make() {
		String requestPayment = "Request successful";
		FileWriter requestsFileWriter;
    	try {
    		requestsFileWriter = new FileWriter("requests.txt", true);
    		if(userID.exists())
    			requestsFileWriter.write(getFile());
    		else
    			requestPayment = "User does not exist";
    		requestsFileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return requestPayment + ":" + getFile().split(":")[0];
	}

	private String getFile() {
		return requestID + ":" + clientID.getID() + ":" + userID.getID() + ":" + amount + ":" + status.name() + "\n";
	}

	public String updateStatus(){
		
		String oldLine = getFile();
		this.status = Status.CLOSED;
		String newLine = getFile();
		Scanner requestFileReader;
		String line;
		StringBuffer inputBuffer = new StringBuffer();
		String fileContents;
		FileWriter requestFileWriter;
		try {
			requestFileReader = new Scanner(new File("requests.txt"));
			while(requestFileReader.hasNextLine()) {
				line = requestFileReader.nextLine();
				inputBuffer.append(line);
				inputBuffer.append("\n");
			}

			fileContents = inputBuffer.toString().replaceAll(oldLine, newLine);

			requestFileWriter = new FileWriter("requests.txt");
			requestFileWriter.write(fileContents);

			requestFileWriter.close();
			requestFileWriter.close();


		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return "Status updated";
	}

	public User getReceiver() {
		return this.userID;
	}

	public User getSender() {
		return this.clientID;
	}

	public float getAmount() {
		return this.amount;
	}

	public boolean exists() {
		try {
			Scanner requestFileReader = new Scanner(new File("requests.txt"));
			String[] line;
			while(requestFileReader.hasNextLine()) {
				line = requestFileReader.nextLine().split(":");
				if(Integer.parseInt(line[0]) == requestID) {
					requestFileReader.close();
					return true;
				}
			}
			requestFileReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public String getStatus() {
		return status.name();
	}

}