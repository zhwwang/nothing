package cn.com.aboobear.mailrelay.misc;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;

public class DES {
	Cipher cipher = null;
	SecretKeyFactory keyFactory = null;
	private String key;
	private Logger logger;
	private SecretKey secretKey;
	private IvParameterSpec iv;

	public DES(Logger logger, String key) {
		this.logger = logger;
		this.key = key;
		try {
			this.cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
			this.keyFactory = SecretKeyFactory.getInstance("DES");
			DESKeySpec desKeySpec = new DESKeySpec(this.key.getBytes("UTF-8"));
			secretKey = this.keyFactory.generateSecret(desKeySpec);
			iv = new IvParameterSpec(this.key.getBytes("UTF-8"));
		} catch (Exception e) {
			this.logger.log(Level.WARNING, "DES initialization error", e);
			this.cipher = null;
			this.keyFactory = null;
		}
	}

	public boolean isInitOK() {
		if (this.cipher != null && this.keyFactory != null) {
			return true;
		} else {
			return false;
		}
	}

	public String encrypt(String message) {
		byte[] enc = null;
		try {
			this.cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
			enc = this.cipher.doFinal(message.getBytes("UTF-8"));
		} catch (Exception ex) {
			this.logger.log(Level.WARNING, "DES encrypt error", ex);
			enc = null;
		}
		if (enc == null) {
			return null;
		} else {
			return new sun.misc.BASE64Encoder().encode(enc);
		}
	}

	public String decrypt(String message) {
		byte[] enc = null;
		try {
			byte[] b = new sun.misc.BASE64Decoder().decodeBuffer(message);
			this.cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
			enc = this.cipher.doFinal(b);
		} catch (Exception ex) {
			this.logger.log(Level.WARNING, "DES decrypt error", ex);
			enc = null;
		}
		if (enc == null) {
			return null;
		} else {
			return new String(enc);
		}
	}
}
