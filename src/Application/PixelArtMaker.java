package Application;

import java.awt.event.*;
import javax.swing.*;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.event.ChangeEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.NumberFormatter;

/**
 *
 * @author verhi
 */
public class PixelArtMaker implements FocusListener, MouseListener {
    
    private static NumberFormat fmt;
    private static Connection conn;
    private static PreparedStatement listeProjets, recuperePixels, effacePixels, creeImage, effaceImage, creePixel;
    static {
        fmt = NumberFormat.getIntegerInstance();
        fmt.setGroupingUsed(false);
        
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            String url = "jdbc:oracle:thin:@iutdoua-oracle.univ-lyon1.fr:1521:orcl";
            File cred=new File(PixelArtMaker.class.getResource("/res/connexion.txt").toURI());
            List<String> credentials=Files.readAllLines(cred.toPath());
            try {
            	conn=DriverManager.getConnection(url,credentials.get(0),credentials.get(1));
            } catch(Exception e) {
            	JOptionPane.showMessageDialog(null, "Impossible de se connecter à la base de données, vérifiez votre connexion internet et les identifiants présents dans "+cred.getAbsolutePath(), "Erreur de connection", JOptionPane.ERROR_MESSAGE);
            	System.exit(1);
            }
            for(String stmt:Files.readAllLines(new File(PixelArtMaker.class.getResource("/res/database.sql").toURI()).toPath())) {
                try {
                    conn.createStatement().execute(stmt.substring(0, stmt.length()-1));
                } catch(SQLException e) {}
            }
            listeProjets=conn.prepareStatement("SELECT id, largeur, hauteur FROM etat ORDER BY id ASC");
            recuperePixels=conn.prepareStatement("SELECT x, y, color FROM pixels WHERE id=?");
            effacePixels=conn.prepareStatement("DELETE FROM pixels WHERE id=?");
            creeImage=conn.prepareStatement("INSERT INTO etat(id, largeur, hauteur) VALUES(?,?,?)");
            effaceImage=conn.prepareStatement("DELETE FROM etat WHERE id=?");
            creePixel=conn.prepareStatement("INSERT INTO pixels(id, x, y, color) VALUES(?,?,?,?)");
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ImageIcon getIcon(String name) {
        try {
            return new ImageIcon(ImageIO.read(PixelArtMaker.class.getResourceAsStream(name)));
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static final int CRAYON=1;
    public static final int GOMME=2;
    public static final int PIPETTE=3;

    private Color couleurActu = Color.BLACK;
    private JFrame fenetre = new JFrame();
    private JPanel panelFenetre = new JPanel();
    private JPanel panelGauche = new JPanel();
    private JPanel panelPixels = new JPanel();
    private JPanel panelOutils = new JPanel();
    private JPanel panelMenu = new JPanel();
    private JPanel panelCouleur = new JPanel();
    private JPanel panelSlider = new JPanel();
    private JPanel panelRGB = new JPanel();
    private JPanel panelCouleurActu = new JPanel();
    private JPanel panelOutilsBordure = new JPanel();

    private JButton boutonNewProject = new JButton("New project");
    private JButton boutonClearAll = new JButton("Clear ALL!");
    private JButton bRed = new JButton();
    private JButton bOrange = new JButton();
    private JButton bYellow = new JButton();
    private JButton bGreen = new JButton();
    private JButton bBlue = new JButton();
    private JButton bMagenta = new JButton();
    private JButton bWhite = new JButton();
    private JButton bBlack = new JButton();

    private JSlider sliderRed = new JSlider(0, 255);
    private JSlider sliderGreen = new JSlider(0, 255);
    private JSlider sliderBlue = new JSlider(0, 255);

    private JPanel newColor = new JPanel();

    private JFormattedTextField tfRed = new JFormattedTextField(new NumberFormatter(fmt));
    private JFormattedTextField tfGreen = new JFormattedTextField(new NumberFormatter(fmt));
    private JFormattedTextField tfBlue = new JFormattedTextField(new NumberFormatter(fmt));
    
    private JMenuBar menuBar = new JMenuBar();
    private JMenu menu = new JMenu("Menu");
    private JMenuItem menuNew = new JMenuItem("New project");
    private JMenuItem menuOpen = new JMenuItem("Open project");
    private JMenuItem menuSave = new JMenuItem("Save project");
    
    private JToolBar toolBar = new JToolBar();
    private JButton crayon = new JButton(getIcon("/res/crayon.png"));
    private JButton gomme = new JButton(getIcon("/res/gomme.png"));
    private JButton pipette = new JButton(getIcon("/res/pipette.png"));
    
    private Map<String, JPanel> pixels=new HashMap<String, JPanel>();
    private int largeur, hauteur;
    private int mode=CRAYON;
    
    // change la couleur en cours et MAJ l'interface
    private void setColor(Color c) {
        this.couleurActu = c;
        int r = c.getRed();
        int g = c.getGreen();
        int b = c.getBlue();
        sliderRed.setValue(r);
        sliderGreen.setValue(g);
        sliderBlue.setValue(b);
        tfRed.setText(r + "");
        tfGreen.setText(g + "");
        tfBlue.setText(b + "");
        newColor.setBackground(c);
    }

    // bouton couleur prédéfinie
    private void onColorButton(ActionEvent e) {
        this.setColor(((JButton) e.getSource()).getBackground());
    }
    
    // sliders de couleur
    private void onSlide(ChangeEvent e) {
        int r=sliderRed.getValue();
        int g=sliderGreen.getValue();
        int b=sliderBlue.getValue();
        this.setColor(new Color(r, g, b));
    }
    
    // bouton crayon
    private void onCrayonClicked(ActionEvent e) {
        this.mode=CRAYON;
    }
    // bouton gomme
    private void onGommeClicked(ActionEvent e) {
        this.mode=GOMME;
    }
    // bouton pipette
    private void onPipetteClicked(ActionEvent e) {
        this.mode=PIPETTE;
    }

    // constructeur, créé la fenêtre
    public PixelArtMaker() {
    	// créé la fenêtre en elle-même 
        fenetre.setSize(1500, 800);
        fenetre.setTitle("Fenetre Pixel Art");
        fenetre.setLocationRelativeTo(null);
        fenetre.setResizable(false);
        fenetre.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // créé le contentPane
        fenetre.getContentPane().add(panelFenetre);
        panelFenetre.setLayout(new BorderLayout());
        panelFenetre.add("Center", panelGauche);
        panelFenetre.add("East", panelOutilsBordure);
        
        // créé le paneau de droite avec les outils
        panelOutilsBordure.setLayout(new BorderLayout());
        panelOutilsBordure.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
        panelOutilsBordure.setPreferredSize(new Dimension(400, 0));
        panelOutilsBordure.add(panelOutils, BorderLayout.CENTER);

        // remplit le panneau d'outils
        panelOutils.setBorder(BorderFactory.createLineBorder(Color.WHITE, 15));
        panelOutils.setBackground(Color.WHITE);
        panelOutils.setLayout(new BoxLayout(panelOutils, BoxLayout.Y_AXIS));
        panelOutils.add(panelMenu);
        panelOutils.add(panelCouleur);
        panelOutils.add(Box.createVerticalStrut(50));
        panelOutils.add(new JLabel("Color picker"));
        panelOutils.add(panelSlider);
        panelOutils.add(panelRGB);

        // créé les boutons nouveau projet et tout effacer
        GridLayout l1 = new GridLayout(2, 1, 50,10);
        panelMenu.setLayout(l1);
        panelMenu.setBackground(Color.WHITE);
        boutonNewProject.setBorder(BorderFactory.createLineBorder(Color.WHITE, 4));
        boutonNewProject.addActionListener(this::onNewProject);
        panelMenu.add(boutonNewProject);
        boutonClearAll.setBorder(BorderFactory.createLineBorder(Color.WHITE, 4));
        panelMenu.add(boutonClearAll);
        boutonClearAll.addActionListener(this::onClearAll);

        // créé les couleurs prédéfinies
        panelCouleur.setLayout(new GridLayout(2, 4, 4, 4));
        panelCouleur.setBackground(Color.WHITE);
        bRed.setBorder(BorderFactory.createLineBorder(Color.black, 3));
        bRed.setBackground(Color.red);
        bRed.addActionListener(this::onColorButton);
        panelCouleur.add(bRed);
        bOrange.setBorder(BorderFactory.createLineBorder(Color.black, 3));
        bOrange.setBackground(Color.orange);
        bOrange.addActionListener(this::onColorButton);
        panelCouleur.add(bOrange);
        bYellow.setBorder(BorderFactory.createLineBorder(Color.black, 3));
        bYellow.setBackground(Color.yellow);
        bYellow.addActionListener(this::onColorButton);
        panelCouleur.add(bYellow);
        bGreen.setBorder(BorderFactory.createLineBorder(Color.black, 3));
        bGreen.setBackground(Color.green);
        bGreen.addActionListener(this::onColorButton);
        panelCouleur.add(bGreen);
        bBlue.setBorder(BorderFactory.createLineBorder(Color.black, 3));
        bBlue.setBackground(Color.blue);
        bBlue.addActionListener(this::onColorButton);
        panelCouleur.add(bBlue);
        bMagenta.setBorder(BorderFactory.createLineBorder(Color.black, 3));
        bMagenta.setBackground(Color.magenta);
        bMagenta.addActionListener(this::onColorButton);
        panelCouleur.add(bMagenta);
        bWhite.setBorder(BorderFactory.createLineBorder(Color.black, 3));
        bWhite.setBackground(Color.white);
        bWhite.addActionListener(this::onColorButton);
        panelCouleur.add(bWhite);
        bBlack.setBorder(BorderFactory.createLineBorder(Color.black, 3));
        bBlack.setBackground(Color.black);
        bBlack.addActionListener(this::onColorButton);
        panelCouleur.add(bBlack);

        // créé les sliders de couleur
        panelSlider.setLayout(new GridLayout(6, 1));
        panelSlider.setBackground(Color.WHITE);
        panelSlider.add(new JLabel("Red"));
        sliderRed.addChangeListener(this::onSlide);
        sliderRed.setBackground(Color.WHITE);
        panelSlider.add(sliderRed);
        panelSlider.add(new JLabel("Green"));
        sliderGreen.addChangeListener(this::onSlide);
        sliderGreen.setBackground(Color.WHITE);
        panelSlider.add(sliderGreen);
        panelSlider.add(new JLabel("Blue"));
        sliderBlue.addChangeListener(this::onSlide);
        sliderBlue.setBackground(Color.WHITE);
        panelSlider.add(sliderBlue);

        // créé le panel avec la couleur visible et les champs RGB
        panelRGB.setLayout(new GridLayout(1, 2));
        panelRGB.add(panelCouleurActu);
        newColor.setBorder(BorderFactory.createLineBorder(Color.WHITE, 10));
        panelRGB.add(newColor);

        // créé le panel avec les champs RGB
        panelCouleurActu.setLayout(new GridLayout(3, 2));
        panelCouleurActu.setBackground(Color.WHITE);
        panelCouleurActu.add(new JLabel("Red"));
        tfRed.addFocusListener(this);
        panelCouleurActu.add(tfRed);
        panelCouleurActu.add(new JLabel("Green"));
        tfGreen.addFocusListener(this);
        panelCouleurActu.add(tfGreen);
        panelCouleurActu.add(new JLabel("Blue"));
        tfBlue.addFocusListener(this);
        panelCouleurActu.add(tfBlue);
    
        // créé la barre de menu
        fenetre.setJMenuBar(menuBar);
        menuBar.add(menu);
        menuNew.addActionListener(this::onNewProject);
        menu.add(menuNew);
        menuOpen.addActionListener(this::onOpen);
        menu.add(menuOpen);
        menuSave.addActionListener(this::onSave);
        menu.add(menuSave);
        
        // créé le panel de dessin et la toolbar
        panelGauche.setLayout(new BorderLayout());
        panelGauche.add(toolBar, BorderLayout.NORTH);
        toolBar.setFloatable(false);
        crayon.addActionListener(this::onCrayonClicked);
        toolBar.add(crayon);
        gomme.addActionListener(this::onGommeClicked);
        toolBar.add(gomme);
        pipette.addActionListener(this::onPipetteClicked);
        toolBar.add(pipette);
        panelGauche.add(panelPixels, BorderLayout.CENTER);
        
        
        // règle la couleur par défaut
        setColor(Color.BLACK);

        // affiche la fenêtre
        fenetre.setVisible(true);
    }
    
    // bouton nouveau projet
    private void onNewProject(ActionEvent e) {
    	// on créé une dialog custom qui demande la largeur et la hauteur
        JPanel panel=new JPanel();
        JFormattedTextField largeur=new JFormattedTextField(new NumberFormatter(fmt));
        JFormattedTextField hauteur=new JFormattedTextField(new NumberFormatter(fmt));
        panel.setLayout(new GridLayout(2,2));
        panel.add(new JLabel("Largeur (max 50):"));
        panel.add(largeur);
        panel.add(new JLabel("Hauteur (max 60):"));
        panel.add(hauteur);
        
        // on affiche la dialog et vérifie qu'on a bien une confirmation
        if(JOptionPane.showConfirmDialog(fenetre, panel, "Créer nouveau projet", JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION) {
            //on récupère les résultats et créé la grille
        	int h=Integer.parseInt(largeur.getText());
            if (h>60){
                h=60;
            }else if (h<0){
                h=0;
            }
            int w=Integer.parseInt(hauteur.getText());
            if (w>50){
                w=50;
            }else if (w<0){
                w=0;
            }
            createPixels(w, h);
        }
    }
    
    // efface les pixels
    private void onClearAll(ActionEvent e) {
        for (JPanel pixel:pixels.values()) pixel.setBackground(Color.WHITE);
    }
    
    // créé les pixels dans la grille
    private void createPixels(int w, int h) {
    	// supprime les vieux pixels
        List<String> toRemove= new ArrayList<String>();
        toRemove.addAll(pixels.keySet());
        for(String s:toRemove) panelPixels.remove(pixels.remove(s));
        panelPixels.revalidate();
        
        // créé la grille et les pixels
        panelPixels.setLayout(new GridLayout(w, h));
        largeur=w;
        hauteur=h;
        for(int y=0; y<h; y++) {
            for(int x=0; x<w; x++) {
                JPanel pixel=new JPanel();
                pixel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                pixel.setOpaque(true);
                pixel.setBackground(Color.WHITE);
                pixel.addMouseListener(this);
                pixels.put(x+","+y, pixel); // les stocke dans une Map pour la simplicité d'accès
                panelPixels.add(pixel);
            }
        }
        panelPixels.revalidate();
    }
    
    // change la couleur d'un pixel lors d'un clic
    private void onPixelClicked(JPanel pixel) {
        if(mode==CRAYON) pixel.setBackground(couleurActu);
        else if (mode==PIPETTE) setColor(pixel.getBackground());
        else pixel.setBackground(Color.WHITE);
    }

    @Override
    public void focusGained(FocusEvent e) {} // inutilisé

    // gestion du focus des champs numériques RGB
    @Override
    public void focusLost(FocusEvent e) {
    	// récupération des valeurs RGB
        int r=Integer.parseInt(tfRed.getText());
        if (r>255){
            r=255;
        }else if (r<0){
            r=0;
        }
        int g=Integer.parseInt(tfGreen.getText());
        if (g>255){
            g=255;
        }else if (r<0){
            g=0;
        }
        int b=Integer.parseInt(tfBlue.getText());
        if (b>255){
            b=255;
        }else if (r<0){
            b=0;
        }
        
        // mise à jour de la couleur active
        this.setColor(new Color(r, g, b));
    }

    // gestion des clics sur les pixels
    @Override
    public void mouseClicked(MouseEvent e) {
        onPixelClicked((JPanel) e.getSource());
    }
    @Override
    public void mousePressed(MouseEvent e) {
        onPixelClicked((JPanel) e.getSource());
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        onPixelClicked((JPanel) e.getSource());
    }

    @Override
    public void mouseEntered(MouseEvent e) {} // inutilisé

    @Override
    public void mouseExited(MouseEvent e) {} // inutilisé
    
    // créé menu selection projet
    public TupleProjet selectionProjet(boolean autoriseNouveau) {
    	try {
    		// créé un panel pour le popup
    		JPanel pnl=new JPanel(new BorderLayout());
    		pnl.add(new JLabel("Sélectionner un emplacement de projet"), BorderLayout.NORTH);
    		
	    	// créé une table avec les projets déjà créés
	    	List<TupleProjet> projetsL=new ArrayList<TupleProjet>();
	    	ResultSet rst=listeProjets.executeQuery();
	    	while(rst.next()) {
	    		TupleProjet tp=new TupleProjet(rst.getInt(1), rst.getInt(2), rst.getInt(3));
	    		projetsL.add(tp);
	    	}
	    	JTable projets=new JTable(new ModeleProjet(projetsL));
	    	projets.setRowSelectionAllowed(true);
	    	projets.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	    	pnl.add(new JScrollPane(projets), BorderLayout.CENTER);
	    	
	    	if(autoriseNouveau) {
	    		// créé une option pour un nouveau projet
	    		JButton nouveau=new JButton("Nouveau projet");
	    		JFormattedTextField id=new JFormattedTextField(new NumberFormatter(fmt));
	    		if(projetsL.isEmpty()) id.setValue(1);
                        else id.setValue(projetsL.get(projetsL.size()-1).id+1);
	    		id.setEditable(false);
	    		id.setColumns(5);
	    		nouveau.addActionListener(e -> {
	    			projetsL.add(new TupleProjet(((Number) id.getValue()).intValue(), largeur, hauteur));
	    			((AbstractTableModel) projets.getModel()).fireTableDataChanged();
	    			projets.setRowSelectionInterval(projetsL.size()-1, projetsL.size()-1);
	    			nouveau.setEnabled(false);
	    		});
	    		JPanel pnl1=new JPanel(new FlowLayout());
	    		pnl1.add(nouveau);
	    		pnl1.add(id);
	    		pnl.add(pnl1, BorderLayout.SOUTH);
	    	}
	    	
	    	// l'affiche à l'utilisateur et attend la confirmation
	    	if(JOptionPane.showConfirmDialog(fenetre, pnl, "Sélectionner l'emplacement de projet", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE)==JOptionPane.OK_OPTION) {
	    		return projetsL.get(projets.getSelectedRow());
	    	}
    	} catch(SQLException e) {
    		e.printStackTrace();
    	}
    	return null;
    }
    
    // menu ouvrir
    private void onOpen(ActionEvent e) {
    	// on demande le projet à l'utilisateur
    	TupleProjet tp=selectionProjet(false);
    	if(tp==null) return;
    	
    	// on créé le projet en local
    	createPixels(tp.largeur, tp.hauteur);
    	
    	// on remplit les pixels
    	try {
	    	recuperePixels.setInt(1, tp.id);
	    	ResultSet rst=recuperePixels.executeQuery();
	    	while(rst.next()) {
	    		String s=rst.getInt(1)+","+rst.getInt(2);
	    		Color c=Color.decode("#"+rst.getString(3));
	    		System.out.println(s+": "+c);
	    		pixels.get(s).setBackground(c);
	    	}
    	} catch(SQLException ex) {
    		// pas normal
    		ex.printStackTrace();
    		return;
    	}
    }
    
    // menu sauvegarder
    private void onSave(ActionEvent e) {
    	// on s'assure d'avoir un projet
    	if(pixels.isEmpty()) {
    		JOptionPane.showMessageDialog(fenetre, "Aucun projet à enregistrer", "Erreur sauvegarde", JOptionPane.ERROR_MESSAGE);
    		return;
    	}
    	
    	// on demande le projet à l'utilisateur
    	TupleProjet tp=selectionProjet(true);
    	if(tp==null) return;
    	
    	// on créé le projet dans la BD
    	try {
    		effacePixels.setInt(1, tp.id);
    		effacePixels.execute();
    		effaceImage.setInt(1, tp.id);
    		effaceImage.execute();
    	} catch(SQLException ex) {
    		// le projet n'existe pas
    	}
    	try {
	    	creeImage.setInt(1, tp.id);
	    	creeImage.setInt(2, largeur);
	    	creeImage.setInt(3, hauteur);
	    	creeImage.execute();
    	} catch(SQLException ex) {
			// pas normal...
			ex.printStackTrace();
			return;
    	}
    	
    	// on enregsitre les pixels
    	try {
	    	creePixel.setInt(1, tp.id);
	    	for(String s:pixels.keySet()) {
	    		String[] sp=s.split(",");
	    		creePixel.setInt(2, Integer.parseInt(sp[0]));
	    		creePixel.setInt(3, Integer.parseInt(sp[1]));
	    		int rgb=pixels.get(s).getBackground().getRGB()&0xffffff;
	    		String rgbS=Integer.toHexString(rgb);
	    		while(rgbS.length()<6) rgbS="0"+rgbS;
	    		creePixel.setString(4, rgbS);
	    		creePixel.execute();
	    	}
    	} catch(SQLException ex) {
    		// pas normal
    		ex.printStackTrace();
    		return;
    	}
    	
    	// on commit
    	try {
    		conn.createStatement().execute("COMMIT");
    	} catch(SQLException ex) {
    		// pas normal
    		ex.printStackTrace();
    	}
    }
}
