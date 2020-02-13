package Application;

import java.util.List;

import javax.swing.table.AbstractTableModel;

public class ModeleProjet extends AbstractTableModel {
	
	private List<TupleProjet> lignes;
	
	public ModeleProjet(List<TupleProjet> list) {
		super();
		lignes=list;
	}
	
	@Override
	public int getColumnCount() {
		return 3;
	}
	
	@Override
	public int getRowCount() {
		return this.lignes.size();
	}
	
	@Override
	public String getColumnName(int x) {
		switch(x) {
			case 0: return "ID";
			case 1: return "Largeur";
			case 2: return "Hauteur";
		}
		return null;
	}
	
	@Override
	public Class<?> getColumnClass(int x) {
		return int.class;
	}
	
	@Override
	public Object getValueAt(int y, int x) {
		TupleProjet t=this.lignes.get(y);
		switch(x) {
			case 0: return t.id;
			case 1: return t.largeur;
			case 2: return t.hauteur;
		}
		return null;
	}
	
}
