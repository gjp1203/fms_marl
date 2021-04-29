package communication_functions;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

public class Object_msg {
	private String msgtype;
	private String sender;
	private String content;
	private String laenge;
	private String Agententyp;
	private String Datentyp;
	private String DocMessage = "";
	
	public Object_msg() {
		msgtype = "";
		sender = "";
		content = "";
		laenge = "";
		Agententyp = "";
		Datentyp ="";
		DocMessage="";
	}
	
	public Object_msg(String newSender, String newContent, String newMsgtype,String newLaenge,String newAgententyp,String newDatentyp) {
		msgtype = newMsgtype;
		sender = newSender;
		content = newContent;
		laenge = newLaenge;
		Agententyp = newAgententyp;
		Datentyp = newDatentyp;
	}
	
	public Object_msg(BufferedReader inBuff) {
		msgtype = "";
		sender = "";
		content = "";
		laenge = "";
		Agententyp = "";
		Datentyp = "";
		recordMsg(inBuff);
	}
	
	public Object_msg(String packedMsg) {
		msgtype = "";
		sender = "";
		content = "";
		laenge = "";
		Agententyp = "";
		Datentyp = "";
		recordMsg(new BufferedReader(new StringReader(packedMsg)));
	}
	public String getDocumentation() {
		return DocMessage;
	}
	
	public String getMsgtype() {
		return msgtype;
	}
	
	public String getSender() {
		return sender;
	}
	
	public String getContent() {
		return content;
	}
	
	public String getLaenge() {
		return laenge;
	}
	
	public String getAgententyp() {
		return Agententyp;
	}
	
	public String getDatentyp() {
		return Datentyp;
	}
	
	public String packMsg() {
		return msgtype + ":" + sender + ":" + content + ":" + laenge + ":" + Agententyp + ":" + Datentyp + ";"; //Nachricht aus Sender und Inhalt zusammensetzen
	}
	
	public void setAchsen(String newContent) {
		content = newContent;
	}
	
	public void setLaenge(String newLaenge) {
		laenge = newLaenge;
	}
	
	public void recordMsg(BufferedReader inBuff) { //Ableiten einer strukturierten msg aus Input
		int fieldToWrite = 0; //Feldnummer zu Beginn 0 (Sender)
		//while(true)
		//{
			char inChar;
			
			try {
				//inChar = (char) inBuff.read();
				
				
				while (fieldToWrite <4)	{
					inChar = (char) inBuff.read();
					
					DocMessage = DocMessage + inChar; 
					
						if(inChar == ':') {
							//System.out.println(inChar + "BBB");
						//	inChar = (char) inBuff.read();
							fieldToWrite++;//zuerst Feld für Sender, dann ab : Feld für Inhalt (Feld=Feld+1)
							} 
						else if(inChar == ';') { //Beenden, wenn ; erreicht
							//System.out.println("Ende");
							return;
							}
						else if(fieldToWrite == 0) {
							sender = sender + inChar; //Zeichen für Sender sammeln	
							//inChar = (char) inBuff.read();
							//System.out.println(sender);
							}
						else if(fieldToWrite == 1) {
							Agententyp = Agententyp + inChar; //Zeichen für Sender sammeln	
							//inChar = (char) inBuff.read();
							//System.out.println(sender);
							}
						else if(fieldToWrite == 2) {
							Datentyp = Datentyp + inChar; //Zeichen für Sender sammeln	
							//inChar = (char) inBuff.read();
							//System.out.println(sender);
							}
						else if(fieldToWrite == 3) {
							
							while (inChar != ';') {
								content = content + inChar; //Zeichen für Inhalt sammeln
								inChar = (char) inBuff.read();
								DocMessage = DocMessage + inChar;
							}
							System.out.println("Sender: " + sender + ";");
							System.out.println("Agententyp: " + Agententyp + ";");
							System.out.println("Datentyp: " + Datentyp+";");
							System.out.println("Content: "+ content + ";");
							return;
							}
				}
				
	
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				
			}
		//}
	
	}
}