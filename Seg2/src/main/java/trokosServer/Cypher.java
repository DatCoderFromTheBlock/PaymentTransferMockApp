import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Cypher {

	private SecretKey key;
	private String passwordCifra;
	private byte[] params;
	
	public Cypher(String passwordCifra) {
		this.passwordCifra = passwordCifra;
	}
	
	public SecretKey getKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
		byte[] salt = { (byte) 0xc9, (byte) 0x36, (byte) 0x78, (byte) 0x99, (byte) 0x52, (byte) 0x3e, (byte) 0xea, (byte) 0xf2 };
		
		PBEKeySpec keySpec = new PBEKeySpec(this.passwordCifra.toCharArray(), salt, 20); // pass, salt, iterations
		SecretKeyFactory kf = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
		SecretKey key = kf.generateSecret(keySpec);
		
		return key;
	}
	
	public void setKey(SecretKey key) {
		this.key = key;
	}
	
	public void encrypt(File file, String data) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
		Cipher c = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
		c.init(Cipher.ENCRYPT_MODE, key);
		File parameters = new File("params.txt");
		if(parameters.exists()) {
			parameters.delete();
		}
		parameters.createNewFile();
		 
		
		 
		FileOutputStream fileOutputStream = new FileOutputStream(parameters);
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
		
		objectOutputStream.writeObject(c.getParameters().getEncoded());
		objectOutputStream.close();
		fileOutputStream.close();
		
		FileOutputStream fileOutputStreamCipher = new FileOutputStream(file);
		CipherOutputStream cos = new CipherOutputStream(fileOutputStreamCipher, c);

		cos.write(data.getBytes());
		cos.close();
	}
	
	public String decrypt(File file) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, ClassNotFoundException {
		AlgorithmParameters p = AlgorithmParameters.getInstance("PBEWithHmacSHA256AndAES_128");
		try{
			getParams();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		p.init(this.params);
		Cipher d = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
		d.init(Cipher.DECRYPT_MODE, key, p);
		FileInputStream fileInputStream = new FileInputStream(file);
		CipherInputStream cypherInputStream = new CipherInputStream(fileInputStream, d);
		ByteArrayOutputStream  byteArrayOutputStream = new ByteArrayOutputStream();
		byte[] b = new byte[16]; 
		int i;
		while ((i=fileInputStream.read(b) ) != -1) {
			byteArrayOutputStream.write(b, 0, i);
		}
		
		String result = byteArrayOutputStream.toString();
		cypherInputStream.close();
		fileInputStream.close();
		return result;
	}
	
	private void getParams() throws ClassNotFoundException, IOException {
		File parameters = new File("params.txt");
		FileInputStream fileInputStream = new FileInputStream(parameters);
		ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
		this.params = (byte[]) objectInputStream.readObject();
		objectInputStream.close();
		fileInputStream.close();
	}
	
}
