package communication_functions;
public class Msg_serialization implements java.io.Serializable {
	
	String Arbeitsgang;
	String Material;
	String Status;
	String Agentenname;
	String Agententyp;
	String Content;
	String Datentyp;
	String Auftragstyp;
	Integer Anzahl_Auftr�ge_in_Puffer;
	Integer Anzahl_Auftragstypen_in_Puffer;
	Integer L�nge;
	Integer Durchmesser;
	Integer Tiefe;
	Integer Achsenzahl;
	Integer Verf�gbarkeit;
	Integer MTTR;
	Integer Arbeitsg�nge_bis_aktuell;
	Float ZeitbisBearbeitung;
	Float Bearbeitungszeitfaktor; // Produkt aus Maschinenzeitfaktor und jeweiligem Materialzeitfaktor
	Float Schlupfzeit;
	Float Bearbeitungszeit_bis_aktuell;
	Float GesamtzeitPuffer;
	Float GesamtzeitPuffer_1;
	Float KapazitaetMaschine;

	
	public String getArbeitsgang() {return Arbeitsgang;}
	public void setArbeitsgang(String a) {Arbeitsgang = a;}
	public String getMaterial() {return Material;}
	public void setMaterial(String m) {Material = m;}
	public String getStatus() {return Status;}
	public void setStatus(String s) {Status = s;}
	public String getAgentenname() {return Agentenname;}
	public void setAgentenname(String an) {Agentenname = an;}
	public String getAgententyp() {return Agententyp;}
	public void setAgententyp(String at) {Agententyp = at;}
	public String getContent() {return Content;}
	public void setContent(String c) {Content = c;}
	public String getDatentyp() {return Datentyp;}
	public void setDatentyp(String dt) {Datentyp = dt;}
	public String getAuftragstyp() {return Auftragstyp;}
	public void setAuftragstyp(String aut) {Auftragstyp = aut;}
	
	public Integer getAnzahl_Auftr�ge_in_Puffer() {return Anzahl_Auftr�ge_in_Puffer;}
	public void setAnzahl_Auftr�ge_in_Puffer(Integer aip) {Anzahl_Auftr�ge_in_Puffer = aip;}
	public Integer getAnzahl_Auftragstypen_in_Puffer() {return Anzahl_Auftragstypen_in_Puffer;}
	public void setAnzahl_Auftragstypen_in_Puffer(Integer atip) {Anzahl_Auftragstypen_in_Puffer = atip;}
	public Integer getL�nge() {return L�nge;}
	public void setL�nge(Integer l) {L�nge = l;}
	public Integer getDurchmesser() {return Durchmesser;}
	public void setDurchmesser(Integer d) {Durchmesser = d;}
	public Integer getTiefe() {return Tiefe;}
	public void setTiefe(Integer t) {Tiefe = t;}
	public Integer getAchsenzahl() {return Achsenzahl;}
	public void setAchsenzahl(Integer az) {Achsenzahl = az;}
	public Integer getVerf�gbarkeit() {return Verf�gbarkeit;}
	public void setVerf�gbarkeit(Integer v) {Verf�gbarkeit = v;}
	public Integer getArbeitsg�nge_bis_aktuell() {return Arbeitsg�nge_bis_aktuell;}
	public void setArbeitsg�nge_bis_aktuell(Integer aba) {Arbeitsg�nge_bis_aktuell = aba;}
	
	public Float getZeitbisBearbeitung() {return ZeitbisBearbeitung;}
	public void setZeitbisBearbeitung(Float z) {ZeitbisBearbeitung = z;}
	public Integer getMTTR() {return MTTR;}
	public void setMTTR(Integer mt) {MTTR = mt;}
	public Float getBearbeitungszeitfaktor() {return Bearbeitungszeitfaktor;}
	public void setBearbeitungszeitfaktor(Float bzf) {Bearbeitungszeitfaktor = bzf;}
	public Float getSchlupfzeit() {return Schlupfzeit;}
	public void setSchlupfzeit(Float sz) {Schlupfzeit = sz;}
	public Float getBearbeitungszeit_bis_aktuell() {return Bearbeitungszeit_bis_aktuell;}
	public void setBearbeitungszeit_bis_aktuell(Float bba) {Bearbeitungszeit_bis_aktuell = bba;}
	
	// Difference Rewards: 
	public Float getGesamtzeitPuffer() {return GesamtzeitPuffer;}
	public void setGesamtzeitPuffer(Float gzp) {GesamtzeitPuffer = gzp;}
	public Float getGesamtzeitPuffer_1() {return GesamtzeitPuffer_1;}
	public void setGesamtzeitPuffer_1(Float gzp_1) {GesamtzeitPuffer_1 = gzp_1;}
	public Float getKapazitaetMaschine() {return KapazitaetMaschine;}
	public void setKapazitaetMaschine(Float km) {KapazitaetMaschine = km;}
	
	

}