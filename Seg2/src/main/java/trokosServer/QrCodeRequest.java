import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class QrCodeRequest {
    private int id;
    private User owner;
    private float amount;
    private Status status;


    public QrCodeRequest(String id){
		Scanner requestFileReader;
		String[] line;
		try {
			requestFileReader = new Scanner(new File("QRcoderequests.txt"));
			while (requestFileReader.hasNextLine()) {
				line = requestFileReader.nextLine().split(":");
				if(line[0].equals(id)){
					this.id = Integer.parseInt(line[0]);
					this.owner = new User(line[1]);
					this.amount = Float.parseFloat(line[2]);
					this.status = Status.valueOf(line[3]);
				} 
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public QrCodeRequest(User owner, float amount) {
		try {
			Scanner idReader = new Scanner(new File("QRcoderequests.txt"));
			String line = "0";
			while (idReader.hasNextLine()) {
				line = idReader.nextLine();
			}
			if (line == null) {
				this.id = 1;
			} else {
				this.id = Integer.parseInt(line.split(":")[0]) + 1;
			}
			
			idReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.owner = owner;
		this.amount = amount;
		this.status = Status.PENDING;
	}

	public byte[] make() throws WriterException, IOException, NotFoundException{

    	try {
    		FileWriter requestsFileWriter = new FileWriter("QRcoderequests.txt", true);
    		requestsFileWriter.write(this.id + ":" + this.owner.getID() + ":" + this.amount + ":" + this.status.name() + "\n");
    		requestsFileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

        String data = String.valueOf(this.id);

		String charset = "UTF-8";
 
        Map<EncodeHintType, ErrorCorrectionLevel> hashMap
            = new HashMap<EncodeHintType, ErrorCorrectionLevel>();
        hashMap.put(EncodeHintType.ERROR_CORRECTION,
                    ErrorCorrectionLevel.L);

		BitMatrix matrix = new MultiFormatWriter().encode(
            new String(data.getBytes(charset), charset),
            BarcodeFormat.QR_CODE, 500, 500);
 
        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        
        byte[] bytes = baos.toByteArray();
		return bytes;
    }

	public String getID(){
		return String.valueOf(this.id);
	}

	public boolean exists() {
		try {
			Scanner QRcodeFileReader = new Scanner(new File("QRcoderequests.txt"));
			String[] line;
			while(QRcodeFileReader.hasNextLine()) {
				line = QRcodeFileReader.nextLine().split(":");
				if(Integer.parseInt(line[0]) == id) {
					QRcodeFileReader.close();
					return true;
				}
			}
			QRcodeFileReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public String getStatus() {
		return this.status.name();
	}

	public User getSender() {
		return this.owner;
	}

	public float getAmount() {
		return this.amount;
	}

	private String getFile() {
		return id + ":" + owner.getID() + ":" + amount + ":" + status.name() + "\n";
	}

	public String updateStatus() {
		
		String oldLine = getFile();
		this.status = Status.CLOSED;
		String newLine = getFile();
		Scanner requestFileReader;
		String line;
		StringBuffer inputBuffer = new StringBuffer();
		String fileContents;
		FileWriter requestFileWriter;
		
		try {
			requestFileReader = new Scanner(new File("QRcoderequests.txt"));
			while(requestFileReader.hasNextLine()) {
				line = requestFileReader.nextLine();
				inputBuffer.append(line);
				inputBuffer.append("\n");
			}

			fileContents = inputBuffer.toString().replaceAll(oldLine, newLine);

			requestFileWriter = new FileWriter("QRcoderequests.txt");
			requestFileWriter.write(fileContents);

			requestFileWriter.close();
			requestFileWriter.close();


		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return "Status updated";
	}
}
