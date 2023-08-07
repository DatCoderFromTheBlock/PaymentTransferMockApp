import java.io.*;
import java.util.Scanner;

public class Group {
	private String groupID;
	private String owner;
	private String[] members;

	public Group(String groupID) {
		this.groupID = groupID;

		if(exists()) {
			this.members = getMembers();
			this.owner = getOwnerID();
		}

	}

	public String create(User owner) {
		try {
			if(!exists()) {
				FileWriter groupFileWriter = new FileWriter("groups.txt", true);
				groupFileWriter.write(groupID + ":" + owner.getID() + "\n");
				groupFileWriter.close();
			} else {
				return "Group ID already in use";
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "Group successfully created";
	}

	public Boolean exists() {
		try {
			Scanner groupFileReader = new Scanner(new File("groups.txt"));
			String[] line;
			while(groupFileReader.hasNextLine()) {
				line = groupFileReader.nextLine().split(":");
				if(line[0].equals(groupID)) {
					groupFileReader.close();
					return true;
				}
			}
			groupFileReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public String addMember(User owner, User userID) {
		Scanner groupFileReader;
		String line;
		StringBuffer inputBuffer = new StringBuffer();
		FileWriter groupFileWriter;

		try {

			for(int i = 1; i < members.length; i++){
				if(userID.getID().equals(members[i]))
					return "User is already in the group";
			}

			if(userID.exists()) {
				if(exists()) {
					if(owner.getID().equals(getOwnerID())) {
						if(!owner.getID().equals(userID.getID())){
							groupFileReader = new Scanner(new File("groups.txt"));
							while(groupFileReader.hasNextLine()) {
								line = groupFileReader.nextLine();
								if (line.split(":")[0].equals(groupID))
									inputBuffer.append(line + ":" + userID.getID());
								else 
									inputBuffer.append(line);
								inputBuffer.append("\n");
							}
							groupFileWriter = new FileWriter("groups.txt");
							groupFileWriter.write(inputBuffer.toString());

							groupFileWriter.close();
							groupFileReader.close();
						} else{
							return "You can not add yourself to the group";
						}
					} else {
						return "Only the owner of the group can add members";
					}
				} else {
					return "Group does not exist";
				}
			} else {
				return "User does not exist";
			}


		} catch (IOException e) {
			e.printStackTrace();
		}
		return "User successfully added to the group";
	}

	public String getOwnerID() {
		try {
			String[] line;
			Scanner groupFileReader = new Scanner(new File("groups.txt"));

			while(groupFileReader.hasNextLine()) {
				line = groupFileReader.nextLine().split(":");
				if (line[0].equals(groupID)) {
					groupFileReader.close();
					return line[1];
				}
			}

			groupFileReader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return "Group does not have an owner";
	}


	private String[] getMembers() {
		String result = "";

		try {
			String[] groups;
			Scanner groupFileReader = new Scanner(new File("groups.txt"));
			while(groupFileReader.hasNextLine()) {
				groups = groupFileReader.nextLine().split(":");
				if(groups[0].equals(groupID)){
					for(int i = 2; i < groups.length; i++) {
						result = result + ":" + groups[i];
					}
				}

			}
			groupFileReader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		return result.split(":");
	}

	public String[] getMembersID() {
		return this.members;
	}

	public String dividePayment(User owner, float amount) {
		if(owner.getID().equals(getOwnerID())) {
			GroupRequest groupRequest = new GroupRequest(owner, groupID);
			return groupRequest.make(amount);
		} else {
			return "You must be the owner of the group to make this request";
		}
	}

	public String status() {
		String result = "";
		try {
			String[] groups;
			Scanner groupFileReader = new Scanner(new File("groupRequests.txt"));
			while(groupFileReader.hasNextLine()) {
				groups = groupFileReader.nextLine().split(":");
				if(groups[1].equals(groupID) && (groups[2].equals(owner))){
					for(int i = 3; i < groups.length; i++) {
						if (i%2==0) {
							Request curr = new Request(groups[i]);
							if(curr.getStatus().equals("PENDING")) {
								result = result + groups[i-1];
								result = result + " Amount: " + String.valueOf(curr.getAmount()) + " " + curr.getStatus() + "\n";
							}
						}
					}
				}
			}

			groupFileReader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		if (result.equals(""))
			return "This group does not exist or you are not the owner of it";
		else
			return result;
	}
	public String history() {
		String result = "";
		try {
			String[] groups;
			Scanner groupFileReader = new Scanner(new File("groupRequests.txt"));
			while(groupFileReader.hasNextLine()) {
				groups = groupFileReader.nextLine().split(":");
				if(groups[1].equals(groupID) && (groups[2].equals(owner))){
					for(int i = 3; i < groups.length; i++) {
						if (i%2==0) {
							Request curr = new Request(groups[i]);
							if(!curr.getStatus().equals("PENDING")) {
								result = result + groups[i-1] + " Amount: " + String.valueOf(curr.getAmount()) + " " + curr.getStatus() + "\n";
							}
						}
					}
				}
			}

			groupFileReader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		if (result.equals(""))
			return "This group does not exist or you are not the owner of it";
		else
			return result;
	}

}