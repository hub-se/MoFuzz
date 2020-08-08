package nas.emf.repair.gen.uml.prototype;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

/**
 * 
 * @author Nebras Nassar
 * @email nassarn@mathematik.uni-marburg.de
 * @version This is a prototype version of EMF Model Generator and customized 
 *          for UML and for Model Fuzzer.
 */
public class EMFModelGeneratorAdapter extends Generator<Resource> {

	private ResourceSetImpl resourceSet;
	public static boolean isLoaded = false;
	EMFModelGenerator nasGen;

	public EMFModelGeneratorAdapter() {
		super(Resource.class);

		this.resourceSet = new ResourceSetImpl();

		UMLResourcesUtil.init(this.resourceSet);

		this.resourceSet.getPackageRegistry().put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);
		this.resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(UMLResource.FILE_EXTENSION,
				UMLResource.Factory.INSTANCE);

		UMLPackage.eINSTANCE.eClass();
		EcoreUtil.resolveAll(UMLPackage.eINSTANCE);
		
		EPackage ePackage = UMLPackage.eINSTANCE;
		nasGen = new EMFModelGenerator(null, ePackage);
		if (!isLoaded) {
			System.out.println("... << Loading the UML grammars >> ... ");
			System.out.println("... UML grammars are very large and the loading may take a few minutes (e.g., 8 minutes) ...");
			System.out.println("... However, loading the grammars is needed only once ..." );
			System.out.println("... Generating a model usually takes a few seconds, thereafter ...");
			try {
				// Loading using a command line
				nasGen.loadGrammar("model-instantiators/" + Constant.ADDITIONAL_GRAMMAR);
				System.out.println("... << Grammars are loaded (using a command line) >> ...");
				isLoaded = true;
			}

			catch (Exception e) {

				System.err.println(e.getMessage());
			}
		}
	}

	@Override
	public Resource generate(SourceOfRandomness random, GenerationStatus __ignore__) {
		// Clean up resource from previous generation
		EList<Resource> resources = this.resourceSet.getResources();
		if (resources.size() > 0) {
			resources.get(0).unload();
			resources.remove(0);
			assert (resources.size() == 0);
		}

		// Create new model, starting with the root element: Model
		Resource modelResource = resourceSet.createResource(URI.createFileURI("uml_model" + ".uml"));
		Model seed = UMLFactory.eINSTANCE.createModel();
		modelResource.getContents().add(seed);
		
		// Construct model by iteratively adding elements to it
		int numberOfExecutions = 500;
		//System.out.println("... strating generating a random model of size about " + numberOfExecutions);
		nasGen.increaseSizeAdditionally(numberOfExecutions, seed);
		//System.out.println("... << A new model is generated successfully >> ...");

		return modelResource;

	}

}