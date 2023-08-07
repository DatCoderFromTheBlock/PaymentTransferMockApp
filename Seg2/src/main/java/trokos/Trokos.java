// A Java program for a Client
import java.util.Scanner;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.Certificate;

import javax.imageio.ImageIO;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import java.awt.image.BufferedImage;
 
public class Trokos {
	
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private String hostname;
	private int port = 45678;
	private String trustStore;
	private String keyStore;
	private String storePass;
	private String clientID;
	private transient PrivateKey pk = null;
	private Scanner sc = new Scanner(System.in);
	private static final String OPTIONS = "\nComandos: \n"
			+ "e|exit                                 	- to exit the program \n"
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
	
	private static final String RUN = "Argumentos invalidos\n" + "Usage: Trokos <serverAddress> "
			+ "<truststore> <keystore> <keystore-password> <userID>";
		
    public static void main(String[] args) {

    	if (args.length == 5) {
			Trokos user = new Trokos(args[0], args[1], args[2], args[3], args[4]);
			user.start();
		} else {
			System.out.println(RUN);
		}
    	
    }
  
	public Trokos(String address, String trustStore, String keyStore, String storePass, String clientID) {
		if(address.split(":").length == 2){
			this.hostname = address.split(":")[0];
			this.port = Integer.parseInt(address.split(":")[1]);
		} else {
			this.hostname = address;
		}
		this.trustStore = trustStore;
		this.keyStore = keyStore;
		this.storePass = storePass;
		this.clientID = clientID;
	}
	

    
	public void start() {

		System.setProperty("javax.net.ssl.trustStore", "truststore/" + trustStore);
		
		Certificate cert = null;
		KeyStore kstore = null;
		
		SocketFactory sf = SSLSocketFactory.getDefault();

    	try {

			SSLSocket skt = (SSLSocket) sf.createSocket(hostname, port);

			out = new ObjectOutputStream(skt.getOutputStream());
	        in = new ObjectInputStream(skt.getInputStream());
	     
	        out.writeObject(clientID);

			byte[] nonce = (byte[]) in.readObject();

			boolean novo = (boolean) in.readObject();



			if (novo) {
				FileInputStream kfile = new FileInputStream("clientKeys/" + "keystore." + clientID);
				kstore = KeyStore.getInstance("JCEKS");
				kstore.load(kfile, storePass.toCharArray());
				cert = kstore.getCertificate(this.clientID);
				kfile.close();
				pk = (PrivateKey) kstore.getKey(this.clientID, storePass.toCharArray());
				Signature signature = Signature.getInstance("MD5withRSA");
				signature.initSign(pk);
				signature.update(nonce);
				out.writeObject(nonce);
				out.writeObject(signature.sign());
				out.writeObject(cert);

			}else{
				FileInputStream kfile = new FileInputStream("clientKeys/" + "keystore." + clientID);
				kstore = KeyStore.getInstance("JCEKS");
				kstore.load(kfile, storePass.toCharArray());
				cert = kstore.getCertificate(this.clientID);
				kfile.close();
				pk = (PrivateKey) kstore.getKey(this.clientID, storePass.toCharArray());
				Signature signature = Signature.getInstance("MD5withRSA");
				signature.initSign(pk);
				signature.update(nonce);
				out.writeObject(signature.sign());
			}
			if ((boolean) in.readObject()) {
				System.out.println("Autenticacao realizada com sucesso.");
				run();
				sc.close();
				System.out.println("Adeus.");
			} else {
				System.out.println("Autenticacao falhou!");
			}


		} catch (NumberFormatException e) {
			System.out.println("O porto e o ip tem de ser um numero.");
		} catch (UnknownHostException e) {
			System.out.println("Nao foi possivel conectar ao servidor.");
		} catch (SocketException e) {
			System.out.println("Erro na ligacao ao servidor.");
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("Conexao com o servidor perdida.");
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				System.out.println("Erro ao fechar OutputStream.");
			}
			try {
				in.close();
			} catch (IOException e) {
				System.out.println("Erro ao fechar InputStream.");
			}
		}
    }

	private void run() throws IOException, ClassNotFoundException {
		
		System.out.println(OPTIONS);
		
		while (true) {
	        	
		String aux = sc.nextLine();
		String[] command = aux.split(" ");
		String option = command[0];
			switch (option) {
				case "help":
					System.out.println(OPTIONS);
					break;
				case "b": case "balance":
					if (command.length != 1)
						System.out.println("Argumentos inválidos. Usage: 'b'|'balance'");
					else
						balance();
					break;
				case "m": case "makepayment":
					if (command.length != 3)
						System.out.println("Argumentos inválidos. Usage: 'm'|'makepayment' <userID> <amount>");
					else
						makepayment(command[1], command[2]);
					break;
				case "r": case "requestpayment":
					if (command.length != 3)
						System.out.println("Argumentos inválidos. Usage: 'r'|'requestpayment' <userID> <amount>");
					else
						requestpayment(command[1], command[2]);
					break;
				case "v": case "viewrequests":
					if (command.length != 1)
						System.out.println("Argumentos inválidos. Usage: 'v'|'viewrequests'");
					else
						viewrequests();
					break;
				case "p": case "payrequest":
					if (command.length != 2)
						System.out.println("Argumentos inválidos. Usage: 'p'|'payrequest' <requestID>");
					else
						payrequest(command[1]);
					break;
				case "o": case "obtainQRcode":
					if (command.length != 2)
						System.out.println("Argumentos inválidos. Usage: 'o'|'obtainQRcode' <amount>");
					else
						obtainQRcode(command[1]);
					break;
				case "c": case "confirmQRcode":
					if (command.length != 2)
						System.out.println("Argumentos inválidos. Usage: 'c'|'confirmQRcode' <QRCode>");
					else
						confirmQRcode(command[1]);
					break;
				case "n": case "newgroup":
					if (command.length != 2)
						System.out.println("Argumentos inválidos. Usage: 'n'|'newgroup' <groupID>");
					else
						newgroup(command[1]);
					break;
				case "a": case "addu":
					if (command.length != 3)
						System.out.println("Argumentos inválidos. Usage: 'a'|'addu' <userID> <groupID>");
					else
						addu(command[1], command[2]);
					break;
				case "g": case "groups":
					if (command.length != 1)
						System.out.println("Argumentos inválidos. Usage: 'g'|'groups'");
					else
						groups();
					break;
				case "d": case "dividepayment":
					if (command.length != 3)
						System.out.println("Argumentos inválidos. Usage: 'd'|'dividepayment' <groupID> <amount>");
					else
						dividepayment(command[1], command[2]);
					break;
				case "s": case "statuspayments":
					if (command.length != 2)
						System.out.println("Argumentos inválidos. Usage: 's'|'statuspayments' <groupID>");
					else
						statuspayments(command[1]);
					break;
				case "h": case "history":
					if (command.length != 2)
						System.out.println("Argumentos inválidos. Usage: 'h'|'history' <groupID>");
					else
						history(command[1]);
					break;
				case "e": case "exit":
					out.writeObject("exit");
					return;
				default:
					System.out.println(option);
					break;

				}
			}
		}


	private void balance() throws IOException, ClassNotFoundException {
		out.writeObject("balance:");
		System.out.println((String) in.readObject());
	}
	
	private void makepayment(String userID, String amount) throws IOException, ClassNotFoundException {
		out.writeObject("makepayment:" + userID + ":" + amount);
		System.out.println((String) in.readObject());
	}
	
	private void requestpayment(String userID, String amount) throws IOException, ClassNotFoundException {
		out.writeObject("requestpayment:" + userID + ":" + amount);
		System.out.println((String) in.readObject());
	}
	
	private void viewrequests() throws IOException, ClassNotFoundException {
		out.writeObject("viewrequests:");
		System.out.println((String) in.readObject());
	}

	private void payrequest(String reqID) throws IOException, ClassNotFoundException {
		out.writeObject("payrequest:" + reqID);
		System.out.println((String) in.readObject());
	}

	private void obtainQRcode(String amount) throws IOException, ClassNotFoundException {
		out.writeObject("obtainQRcode:" + amount);
		
		byte[]b = (byte[]) in.readObject();
		String name = (String) in.readObject();

		InputStream is = new ByteArrayInputStream(b);
		BufferedImage buffer = ImageIO.read(is);

		File outputFile = new File("QRCode" + name + ".png");
		ImageIO.write(buffer, "png", outputFile);

		System.out.println("QRcode made");
	}

	private void confirmQRcode(String QRcode) throws IOException, ClassNotFoundException {
		out.writeObject("confirmQRcode:" + QRcode);
		System.out.println((String) in.readObject());
	}

	private void newgroup(String groupID) throws IOException, ClassNotFoundException {
		out.writeObject("newgroup:" + groupID);
		System.out.println((String) in.readObject());
	}

	private void addu(String userID, String groupID) throws IOException, ClassNotFoundException {
		out.writeObject("addu:" + userID + ":" + groupID);
		System.out.println((String) in.readObject());
	}

	private void groups() throws IOException, ClassNotFoundException {
		out.writeObject("groups:");
		System.out.println((String) in.readObject());
	}

	private void dividepayment(String groupID, String amount) throws IOException, ClassNotFoundException {
		out.writeObject("dividepayment:");
		System.out.println((String) in.readObject());
	}

	private void statuspayments(String groupID) throws IOException, ClassNotFoundException {
		out.writeObject("statuspayments:");
		System.out.println((String) in.readObject());
	}

	private void history(String groupID) throws IOException, ClassNotFoundException {
		out.writeObject("history:");
		System.out.println((String) in.readObject());
	}
}