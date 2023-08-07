import java.io.*;
import java.util.*;

public class GroupRequest {
	private int ID;
	private User owner;
	private String groupID;	
	
	public GroupRequest(User owner, String groupID) {
		try {
			Scanner groupRequestFileReader = new Scanner(new File("groupRequests.txt"));
			String line = "0";
			while (groupRequestFileReader.hasNextLine()) {
				line = groupRequestFileReader.nextLine();
			}
			if (line == null) {
				this.ID = 1;
			} else {
				this.ID = Integer.parseInt(line.split(":")[0]) + 1;
			}
			
			groupRequestFileReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.owner = owner;
		this.groupID = groupID;
	}

	public String make(float amount) {
		String[] members = new Group(groupID).getMembersID();
		try {
			FileWriter groupRequestFileWriter = new FileWriter("groupRequests.txt", true);
			groupRequestFileWriter.write(ID + ":" + groupID + ":" + owner.getID());
			
			if(members.length != 0) {
				float newAmount =  (float) Math.round((amount / members.length) * 100) / 100;
				for (int i = 0; i < members.length; i++) {
					Request request = new Request(owner, new User(members[i]), newAmount);
					groupRequestFileWriter.write(":" + members[i] + ":" + request.make().split(":")[1]);
				}
			} else {
				groupRequestFileWriter.write("\n");
				groupRequestFileWriter.close();
				return "Group has no members";
			}
			groupRequestFileWriter.write("\n");
			groupRequestFileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return "Group request successful";
	}
	
}