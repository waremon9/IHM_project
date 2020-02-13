package Application;

public class TupleProjet {
	public TupleProjet(int id, int largeur, int hauteur) {
		this.id=id;
		this.largeur=largeur;
		this.hauteur=hauteur;
	}
	
	public final int id;
	public final int largeur, hauteur;
	
	@Override
	public String toString() {
		return id+": "+largeur+"*"+hauteur;
	}
}
