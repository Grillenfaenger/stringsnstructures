package base.workbench;

import java.awt.BorderLayout;
import java.awt.LayoutManager;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Displays a single property edit line
 * @author Marcel Boeing
 *
 */
public class ModulePropertyPanel extends JPanel implements KeyListener {

	public static final String DESCRIPTIONBOX_PREFIX = "<html><div style='width:300px; background-color:FFFFFF;'>";
	public static final String DESCRIPTIONBOX_SUFFIX = "</div></html>";
	
	private static final long serialVersionUID = -1586278882294925349L;
	private PropertyQuadrupel property;
	private JTextField valueTextField;

	public ModulePropertyPanel(PropertyQuadrupel property) {
		this(property, new BorderLayout(), true);
	}

	public ModulePropertyPanel(PropertyQuadrupel property, LayoutManager layout) {
		this(property, layout, true);
	}

	public ModulePropertyPanel(PropertyQuadrupel property, boolean isDoubleBuffered) {
		this(property, new BorderLayout(), isDoubleBuffered);
	}

	public ModulePropertyPanel(PropertyQuadrupel property, LayoutManager layout, boolean isDoubleBuffered) {
		super(layout, isDoubleBuffered);
		this.property = property;
		this.initialize();
	}
	
	private void initialize(){
		// Clear old components (if present)
		this.removeAll();
		// Create description text
		String description = DESCRIPTIONBOX_PREFIX+"<h1 style='font-size:12px;'>"+this.property.getKey()+"</h1>"+this.property.getDescription()+DESCRIPTIONBOX_SUFFIX;
		// Create labels and input fields according to given property
		JLabel keyLabel = new JLabel(this.property.getKey()+" ");
		keyLabel.setToolTipText(description);
		this.valueTextField = new JTextField();
		this.valueTextField.setToolTipText(description);
		this.valueTextField.addKeyListener(this);
		if (this.property.getValue() != null){
			this.valueTextField.setText(this.property.getValue());
		} else {
			this.valueTextField.setText(this.property.getDefaultValue());
		}
		// Place components on panel
		this.add(keyLabel, BorderLayout.WEST);
		this.add(valueTextField, BorderLayout.CENTER);
	}

	/**
	 * @return the property
	 */
	public PropertyQuadrupel getProperty() {
		return property;
	}

	/**
	 * @param property the property to set
	 */
	public void setProperty(PropertyQuadrupel property) {
		this.property = property;
		this.initialize();
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// not needed, thus empty
	}

	@Override
	public void keyPressed(KeyEvent e) {
		// not needed, thus empty
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// Update the property object if there is text input
		this.property.setValue(this.valueTextField.getText());
	}
	
	/**
	 * Sets the property value.
	 * @param value Value to set
	 */
	public void setValue(String value){
		this.valueTextField.setText(value);
		this.property.setValue(value);
	}

}
