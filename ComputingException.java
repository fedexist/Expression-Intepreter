
public class ComputingException extends Throwable {
	
	/**
	 * Rispetto a GeneralException, questa eccezione è più semplice, avendo come dato membro unicamente il messaggio passato dal costruttore
	 * e facendo l'override di Throwable.getMessage()
	 *  
	 */
	private static final long serialVersionUID = -1795978278143249382L;
	private String detailMessage;
	
	ComputingException(String message){
		
		detailMessage = message;
		
	}
	
	@Override
	public String getMessage(){
		
		return detailMessage;
		
	}
	

}
