/*
 * 18641 java smart phone development - final project - Shair - Web Server
 * Zheng Lei(zlei), Sen Yue(seny)
 */

package email;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/*
 * SendEmail - class used for generating email and send email to users - implements Thread
 * Constructor: public SendEmail(String email, String userName, String account, String password)
 * Methods: 1. run - start the thread
 * 			2. generateAndSendEmail - generate and send email
 */
public class SendEmail extends Thread{
	private static Properties mailServerProperties;
	private static Session getMailSession;
	private static MimeMessage generateMailMessage;
	private String userName;
	private String account;
	private String password;
	private String email;
	
	public SendEmail(String email, String userName, String account, String password){
		this.email = email;
		this.userName = userName;
		this.account = account;
		this.password = password;
	}
	
	@Override
	public void run(){
		try{
			generateAndSendEmail(email,userName,account,password);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
 
	public void generateAndSendEmail(String email, String userName, String account, String password) 
			throws AddressException, MessagingException {
		mailServerProperties = System.getProperties();
		mailServerProperties.put("mail.smtp.port", "587");
		mailServerProperties.put("mail.smtp.auth", "true");
		mailServerProperties.put("mail.smtp.starttls.enable", "true");
		System.out.println("Mail Server Properties have been setup successfully..");
 
		getMailSession = Session.getDefaultInstance(mailServerProperties, null);
		generateMailMessage = new MimeMessage(getMailSession);
		generateMailMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
		generateMailMessage.setSubject("Welcome to Shair Community");
		String emailBody = "<h4>Dear " + userName + ",</h4>Welcome to Shair Community!<br><br>Account: " 
		+ account + "<br>Password: " + password 
		+ "<br><br><img src=\"https://s3.amazonaws.com/shair-application-image/profile-image/shair.png\" style=\"width:360px;height:225px;\">"
		+ "<br><br><br>Regard, <br>Shair Team";
		generateMailMessage.setContent(emailBody, "text/html");
		System.out.println("Mail Session has been created successfully..");
 
		Transport transport = getMailSession.getTransport("smtp");

		transport.connect("smtp.gmail.com", "<Enter Your Email Address Here>", "<Enter Your Email Password Here>");
		transport.sendMessage(generateMailMessage, generateMailMessage.getAllRecipients());
		transport.close();
	}

}