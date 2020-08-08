package nas.emf.repair.gen.uml.prototype;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.henshin.interpreter.EGraph;
import org.eclipse.emf.henshin.interpreter.Engine;
import org.eclipse.emf.henshin.interpreter.UnitApplication;
import org.eclipse.emf.henshin.interpreter.impl.EGraphImpl;
import org.eclipse.emf.henshin.interpreter.impl.EngineImpl;
import org.eclipse.emf.henshin.interpreter.impl.ProfilingApplicationMonitor;
import org.eclipse.emf.henshin.interpreter.impl.UnitApplicationImpl;
import org.eclipse.emf.henshin.model.HenshinPackage;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Unit;
import org.eclipse.uml2.uml.UMLPackage;

/**
 * 
 * @author Nebras Nassar
 * @email nassarn@mathematik.uni-marburg.de
 * @version This is a prototype version of EMF Model Generator and customized
 *          for UML and for Model Fuzzer.
 */
public class EMFModelGenerator {

	private EObject root;
	private EPackage ePackage;
	private Engine engine;
	private EGraph graph;
	private Module grammarModule;
	private UnitApplication mainUnitApplication;
	private ProfilingApplicationMonitor monitor;

	public EMFModelGenerator(EObject root, EPackage ePackage) {
		System.out.println(
				"This is a prototype version of EMF Model Generator and customized for UML and for Model Fuzzer.");
		this.root = root;
		this.ePackage = ePackage;
	}

	public void loadGrammar(String henshinFile) {
		// Load and register
		System.out.println(henshinFile.toString());

		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
		HenshinPackage.eINSTANCE.eClass();
		resourceSet.getPackageRegistry().put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);
		URI uri = URI.createFileURI(henshinFile);
		System.out.println(uri.toFileString());

		Resource resource = resourceSet.getResource(uri, true);
		grammarModule = (Module) resource.getContents().get(0);
		// Initialize the interpreter
		//graph = new EGraphImpl(root);
		//engine = new EngineImpl();
		//engine.getOptions().put(Engine.OPTION_DETERMINISTIC, false);
	}

	private boolean run(Unit unit) {
		mainUnitApplication = new UnitApplicationImpl(engine, graph, unit, null);
		boolean b = mainUnitApplication.execute(null);
		return b;
	}

	/**
	 * Generating a model starting from a seed model
	 * 
	 * @param numberOfExecutions
	 * @param seed
	 * @param ePackage
	 */
	public void increaseSizeAdditionally(int numberOfExecutions, EObject seed) {
		graph = new EGraphImpl(seed);
		engine = new EngineImpl();
		engine.getOptions().put(Engine.OPTION_DETERMINISTIC, false);
		for (int i = 0; i < numberOfExecutions; i++) {
			try {
				run(getGrammarModule().getUnit(Constant.INDEP_Add_AddITIONAL_ELEMENTS));
			} catch (Exception e) {
			}
		}
	}

	private void completeModel() {
		try {
			run(getGrammarModule().getUnit(Constant.COMPLETE_INSTANCE));
		} catch (Exception e) {
		}
	}

	public void generateEMFModel(int increasingElementValue, EObject root, EPackage ePackage) {
		increaseSizeAdditionally(increasingElementValue, root);
		//completeModel();
	}

	public Module getGrammarModule() {
		return grammarModule;
	}

}
