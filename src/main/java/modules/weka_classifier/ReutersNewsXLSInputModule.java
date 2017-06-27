package modules.weka_classifier;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import modules.CharPipe;
import modules.ModuleImpl;
import modules.OutputPort;
import common.parallelization.CallbackReceiver;
import de.uni_koeln.spinfo.stocknews.articles.data.Article;
import de.uni_koeln.spinfo.stocknews.articles.io.XLSReader;

public class ReutersNewsXLSInputModule extends ModuleImpl {
	
	// Define property keys (every setting has to have a unique key to associate it with)
	public static final String PROPERTYKEY_XLSINPUT = "path to xls file";
	
	// Define I/O IDs (must be unique for every input or output)
	private static final String ID_OUTPUT = "output";
	
	private final static Type OUTPUT_TYPE = new TypeToken<Map<Integer,String>>() {
	}.getType();
	
	// Local variables
	private String xlsFilePath;

//	// Main method for stand-alone execution
//	public static void main(String[] args) throws Exception {
//		ModuleRunner.runStandAlone(ResolveClassificationModule.class, args);
//	}
	
	public ReutersNewsXLSInputModule(CallbackReceiver callbackReceiver,
			Properties properties) throws Exception {
		
		// Call parent constructor
		super(callbackReceiver, properties);
		
		// Add module description
		this.setDescription("<p>Reads articles and metadata from special xls file</p>");
		
		// You can override the automatic category selection (for example if a module is to be shown in "deprecated")
		//this.setCategory("Examples");

		// the module's name is defined as a property
		// Property key for module name is defined in parent class
//		this.getPropertyDefaultValues().put(ModuleImpl.PROPERTYKEY_NAME, "Example Module");

		// Add property descriptions (obligatory for every property!)
		this.getPropertyDescriptions().put(PROPERTYKEY_XLSINPUT, "Path to a Reuters News xls File");

		
		// Add property defaults (_should_ be provided for every property)
		this.getPropertyDefaultValues().put(ModuleImpl.PROPERTYKEY_NAME, "Reuters News XLS Input Module");
		this.getPropertyDefaultValues().put(PROPERTYKEY_XLSINPUT, "/Strings/src/test/resources/News_filtered_DE_1.1.xls");

		
		// Define I/O
		/*
		 * I/O is structured into separate ports (input~/output~).
		 * Every port can support a range of pipe types (currently
		 * byte or character pipes). Output ports can provide data
		 * to multiple pipe instances at once, input ports can
		 * in contrast only obtain data from one pipe instance.
		 */
		
		OutputPort outputPort = new OutputPort(ID_OUTPUT, "json serialized map of article contents", this);
		outputPort.addSupportedPipe(CharPipe.class);
		
		// Add I/O ports to instance (don't forget...)
		super.addOutputPort(outputPort);
	}

	@Override
	public boolean process() throws Exception {
		
		List<Article> articles = XLSReader.getArticlesFromXlsFile(xlsFilePath);
		
		Map<Integer,String> articleContents = new TreeMap<Integer,String>();
		Map<Integer,String> articleContentsH = new HashMap<Integer,String>();
		for (int i = 0; i < articles.size(); i++) {
			Article art = articles.get(i);
			articleContentsH.put(i, articles.get(i).getTitle() + " " + articles.get(i).getContent());	
			if((art.getTitle().trim().isEmpty() && art.getContent().trim().isEmpty())){
				System.out.println("No content in article Nr " + i + ": " + art.printComplete());
			}
		}
		articleContents.putAll(articleContentsH);
		
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		final String jsonOut = gson.toJson(articleContents, OUTPUT_TYPE);
		this.getOutputPorts().get(ID_OUTPUT).outputToAllCharPipes(jsonOut);
		
		// Close outputs (important!)
		this.closeAllOutputs();
		
		// Done
		return true;
	}
	
	@Override
	public void applyProperties() throws Exception {
		
		// Set defaults for properties not yet set
		super.setDefaultsIfMissing();
		
		// Apply own properties
		xlsFilePath = this.getProperties().getProperty(PROPERTYKEY_XLSINPUT, this.getPropertyDefaultValues().get(PROPERTYKEY_XLSINPUT));
		
		// Apply parent object's properties (just the name variable actually)
		super.applyProperties();
	}

}