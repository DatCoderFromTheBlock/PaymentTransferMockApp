import java.net.*;
import java.nio.file.Files;
import java.io.*;
import java.util.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;

import com.google.zxing.NotFoundException;
import com.google.zxing.WriterException;

public class TrokosServer {

	private File userFile = new File("users.txt");
	private File balanceFile = new File("balance.txt");
	private File requestsFile = new File("requests.txt");
	private File groupFile = new File("groups.txt");
	private File groupRequestFile = new File("groupRequests.txt");
	private File qRcodeRequestFile = new File("QRcoderequests.txt");
	private Cypher cypher;


	private transient Key secretKey;

	private int socket = 45678;
	String passwordCifra;
	private String keystore;
	private String passwordKeystore;

	public TrokosServer(String[] args) throws IOException, ClassNotFoundException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableEntryException, CertificateException, InvalidKeySpecException{
		if(args.length == 4){
			this.socket = Integer.parseInt(args[0]);
			this.passwordCifra = args[1];
			this.keystore = args[2];
			this.passwordKeystore = args[3];
		} else {
			this.passwordCifra = args[0];
			this.keystore = args[1];
			this.passwordKeystore = args[2];
		}
		cypher = new Cypher(passwordKeystore);
		cypher.setKey(cypher.getKey());
		
		
	}
	public static void main(String[] args) throws ClassNotFoundException, IOException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableEntryException, CertificateException, InvalidKeySpecException {
		TrokosServer trokosServer = new TrokosServer(args);
		trokosServer.startServer();
	}

	public void startServer () {

		try {
				userFile.createNewFile();
				balanceFile.createNewFile();
				requestsFile.createNewFile();
				groupFile.createNewFile();
				groupRequestFile.createNewFile();
				qRcodeRequestFile.createNewFile();
				
			
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Server a executar...");
		System.setProperty("javax.net.ssl.keyStore", "Keys/" + keystore);
		System.setProperty("javax.net.ssl.keyStorePassword", passwordKeystore);

		SSLServerSocket skt = null;

		try {
			ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
			skt = (SSLServerSocket) ssf.createServerSocket(socket);

			FileInputStream fis = new FileInputStream("Keys/" + keystore);
			KeyStore kstore = KeyStore.getInstance("JCEKS");
			kstore.load(fis, this.passwordKeystore.toCharArray());
			this.secretKey = kstore.getKey("server", this.passwordKeystore.toCharArray());

			while (true) {
				new ServerThread(skt.accept(), passwordCifra, secretKey).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Algo Correu mal!");
		} catch (IllegalArgumentException e) {
			System.out.println("Introduza um numero inteiro de 0 a 65535.");
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			e.printStackTrace();
		}
	}

	class ServerThread extends Thread {

		private Socket socket = null;
		private String passwordCifra;
		private ObjectOutputStream outStream = null;
		private ObjectInputStream inStream = null;
		private Key key;


		ServerThread(Socket inSoc, String passwordCifra, Key secretKey) {
			this.socket = inSoc;
			this.passwordCifra = passwordCifra;
			this.key = secretKey;
		}

		public void run() {
			

			
			try {

				inStream = new ObjectInputStream(socket.getInputStream());
				outStream = new ObjectOutputStream(socket.getOutputStream());

				String clientID = (String) inStream.readObject();



				User user = new User(clientID);

				byte[] nonce = new byte[8];
				new Random().nextBytes(nonce);

				outStream.writeObject(nonce); 

				Boolean succ = false;
				Boolean newUser = !user.exists();
				outStream.writeObject(newUser);

				byte[] clientNonce;
				byte[] signature;
				Certificate cert;
				if(newUser) {
					clientNonce = (byte[]) inStream.readObject();
					signature = (byte[]) inStream.readObject();
					cert = (Certificate) inStream.readObject();

					File fileCert = new File("./pubKeys/" + clientID + ".cer");
					FileOutputStream outputStream = new FileOutputStream(fileCert);
					outputStream.write(cert.getEncoded());
					outputStream.close();

					PublicKey pk = cert.getPublicKey();
					Signature s = Signature.getInstance("MD5withRSA");
					s.initVerify(pk);
					s.update(clientNonce);
					succ = s.verify(signature) && Arrays.equals(nonce, clientNonce);

					
					FileWriter balanceFileWriter = new FileWriter("balance.txt", true);
					FileWriter userFileWriter = new FileWriter("users.txt", true);
					if(succ){
						cypher.decrypt(new File("balance.txt"));
                        cypher.decrypt(new File("users.txt"));
                        userFileWriter.write(clientID + "\n");
                        balanceFileWriter.write(clientID + ":" + 100.0 + "\n");
                        cypher.encrypt(new File("balance.txt"), "encrypt");
                        cypher.encrypt(new File("users.txt"), "encrypt");

					}
					userFileWriter.close();
					balanceFileWriter.close();


					outStream.writeObject(succ);
				} else {
					signature = (byte[]) inStream.readObject();
					String certPath = "./pubKeys/" + clientID + ".cer";
					File fileCert = new File(certPath);
					FileInputStream fis = new FileInputStream(fileCert);
					CertificateFactory cf = CertificateFactory.getInstance("X509");
					cert = cf.generateCertificate(fis);
					PublicKey pk = cert.getPublicKey();
					Signature s = Signature.getInstance("MD5withRSA");
					s.initVerify(pk);
					s.update(nonce);
					succ = s.verify(signature);
					outStream.writeObject(succ);
				}

				if(succ){
					System.out.println("Client " + clientID + " is connected");
					getCommands(inStream, outStream, clientID);
					inStream.close();
					outStream.close();
				}
			} catch (IOException | ClassNotFoundException | NoSuchAlgorithmException | SignatureException 
					| InvalidKeyException | CertificateException | NoSuchPaddingException | InvalidAlgorithmParameterException e) {
				e.printStackTrace();
			} 
			
		}
	}

	public static float balance(User clientID) {
		return clientID.getBal();
	}

	public static String makePayment(User clientID, User userID, float amount) {
		String makePayment = "Payment successful";
		if(!clientID.getID().equals(userID.getID())) {
			if (userID.exists()) {
				if(clientID.getBal() >= amount && amount > 0) {
					clientID.updateBal(clientID.getBal() - amount);
					userID.updateBal(userID.getBal() + amount);
				} else
					makePayment = "Insufficient funds";
			} else
				makePayment = "User does not exist";
		} else
			makePayment = "User can not be the one making payment";

		return makePayment;
	}

	public static String requestPayment(User clientID, User userID, float amount) {
		if (!clientID.getID().equals(userID.getID())){
			Request request = new Request(clientID, userID, amount);
			return request.make().split(":")[0];
		} else {
			return "Can not request payments to yourself";
		}
	}

	public static String viewRequests(User clientID) {
		return clientID.getRequests();
	}

	public static String payRequest(User clientID, String reqID) {
		Request updater = new Request(reqID);

		if(updater.exists() && updater.getReceiver().getID().equals(clientID.getID())){
			if(!updater.getSender().getID().equals(clientID.getID())){
				if(updater.getStatus().equals("PENDING")){
					if (makePayment(clientID, updater.getSender(), updater.getAmount()).equals("Payment successful")) {
						updater.updateStatus();
						return "Payment successful";
					} else {
						return "Insufficient Funds";
					}
				} else {
					return "Request has already been paid";
				}
			} else {
				return "You can not pay your own request";
			}
		} else {
			return "Request ID is invalid";
		}
	}

	public static byte[] obtainQRCode(User clientID, float amount) {
		QrCodeRequest qrCodeRequest = new QrCodeRequest(clientID, amount);
		try {
			return qrCodeRequest.make();
		} catch (NotFoundException | WriterException | IOException e) {
			return null;
		}
	}

	public static String confirmQRCode(User clientID, String QRcode) {
		QrCodeRequest updater = new QrCodeRequest(QRcode);

		if(updater.exists()){
			if(!updater.getSender().getID().equals(clientID.getID())){
				if(updater.getStatus().equals("PENDING")){
					if (makePayment(clientID, updater.getSender(), updater.getAmount()).equals("Payment successful")) {
						updater.updateStatus();
						return "Payment successful";
					} else {
						return "Insufficient Funds";
					}
				} else {
					return "Request has already been paid";
				}
			} else {
				return "You can not pay your own request";
			}
		} else {
			return "Request ID is invalid";
		}
	}

	public static String newGroup(User clientID, String groupID) {
		Group group = new Group(groupID);
		return group.create(clientID);
	}

	public static String addU(User clientID, User userID, String groupID) {
		Group group = new Group(groupID);
		return group.addMember(clientID, userID);
	}

	public static String groups(User clientID) {
		return clientID.groups();
	}

	public static String dividePayment(User clientID, String groupID, float amount) {
		Group group = new Group(groupID);
		return group.dividePayment(clientID, amount);
	}

	public static String statusPayments(User clientID, String groupID) {
		Group group = new Group(groupID);
		return group.status();
	}

	public static String history(User clientID, String groupID) {
		Group group = new Group(groupID);
		return group.history();
	}

	public static String help() {

		return    "\nexit                                 	- to exit the program \n"
				+ "b|balance                    			- check balance \n"
				+ "m|makepayment <userID> <amount>    	- transfer money to user \n"
				+ "r|requestpayment <userID> <amount>     	- request money from a certain person \n"
				+ "v|viewrequests                		- view all active requests \n"
				+ "p|payrequest <requestID>         	- pay an active request \n"
				+ "o|obtainQRcode <amount>         	- obtain a QRCode \n"
				+ "c|confirmQRcode <QRcode>        	- confirm a QRCode \n"
				+ "n|newgroup <groupID>             	- create a new group \n"
				+ "a|addu <userID> <groupID>         	- add a member to a current group you own \n"
				+ "g|groups                 		- check all groups you own or are member of \n"
				+ "d|dividepayment <groupID> <amount>    	- divide a payment for all member of the group \n"
				+ "s|statuspayments <groupID>         	- check all current active payments of a certain group you own \n"
				+ "h|history <groupID>            		- check all payments already made of a certain group you own \n";
	}

	public static String getLastQRID(){
		String line = "";
		try {
			Scanner idReader = new Scanner(new File("QRcoderequests.txt"));
			while (idReader.hasNextLine()) {
				line = idReader.nextLine();
			}
			idReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return line.split(":")[0];
	}

	public void getCommands(ObjectInputStream inStream, ObjectOutputStream outStream, String clientID) {
		decryptAll();
		String[] linha;
		User user = new User(clientID);
		try {
			while(true) {
				linha = ((String)inStream.readObject()).split(":");

				switch (linha[0]) {
				case "exit":
					return;
				case "b": case "balance":
					decryptAll();
					outStream.writeObject(String.valueOf(balance(user)));
					encryptAll();
					break;

				case "help": case "helpcommands":
					outStream.writeObject(help());
					break;
				case "m": case "makepayment":
					decryptAll();
					try {
						outStream.writeObject(makePayment(user, new User(linha[1]), Float.parseFloat(linha[2])));
					} catch (NumberFormatException e){
						outStream.writeObject("Amount must be a number");
					}
					encryptAll();
					break;

				case "r": case "requestpayment":
					decryptAll();
					try{
						outStream.writeObject(requestPayment(user, new User(linha[1]), Float.parseFloat(linha[2])));			
					}catch (NumberFormatException e){
						outStream.writeObject("Amount must be a number");
					}
					encryptAll();
					break;

				case "v": case "viewrequests":
					decryptAll();
					outStream.writeObject(viewRequests(user));
					encryptAll();
					break;

				case "p": case "payrequest":
					decryptAll();
					outStream.writeObject(payRequest(user, linha[1]));
					encryptAll();
					break;

				case "o": case "obtainQRcode":
					decryptAll();
					try {
						outStream.writeObject(obtainQRCode(user, Float.parseFloat(linha[1])));
						outStream.writeObject(getLastQRID());
					} catch (NumberFormatException e){
						outStream.writeObject("Amount must be a number");
					}
					encryptAll();
					break;

				case "c": case "confirmQRcode":
					decryptAll();
					System.out.println(linha[1]);
					outStream.writeObject(confirmQRCode(user, linha[1]));
					encryptAll();
					break;

				case "n": case "newgroup":
					decryptAll();
					outStream.writeObject(newGroup(user, linha[1]));
					encryptAll();
					break;

				case "a": case "addu":
					decryptAll();
					outStream.writeObject(addU(user, new User(linha[1]), linha[2]));
					encryptAll();
					break;

				case "g": case "groups":
					decryptAll();
					outStream.writeObject(groups(user));
					encryptAll();
					break;

				case "d": case "dividepayment":
					decryptAll();
					try{
						outStream.writeObject(dividePayment(user, linha[1], Float.parseFloat(linha[2])));
					} catch (NumberFormatException e){
						outStream.writeObject("Amount must be a number");
					}
					encryptAll();
					break;

				case "s": case "statuspayments":
					decryptAll();
					outStream.writeObject(statusPayments(user, linha[1]));
					encryptAll();
					break;

				case "h": case "history":
					decryptAll();
					outStream.writeObject(history(user, linha[1]));
					encryptAll();
					break;

				default:
					outStream.writeObject("Command not valid");

				}
			}
		}catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	private void encryptAll() {
		try {
			cypher.encrypt(balanceFile, "encrypt");
			cypher.encrypt(userFile, "encrypt");
			cypher.encrypt(requestsFile, "encrypt");
			cypher.encrypt(groupFile, "encrypt");
			cypher.encrypt(groupRequestFile, "encrypt");
			cypher.encrypt(qRcodeRequestFile, "encrypt");
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void decryptAll() {
		try {
			cypher.decrypt(balanceFile);
			cypher.decrypt(userFile);
			cypher.decrypt(requestsFile);
			cypher.decrypt(groupFile);
			cypher.decrypt(groupRequestFile);
			cypher.decrypt(qRcodeRequestFile);
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidAlgorithmParameterException | ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}