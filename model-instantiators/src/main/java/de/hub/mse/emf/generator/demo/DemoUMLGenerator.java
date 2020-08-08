package de.hub.mse.emf.generator.demo;

import java.util.Collections;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
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

public class DemoUMLGenerator extends Generator<Resource> {

	private ResourceSetImpl resourceSet;

	public DemoUMLGenerator() {
		super(Resource.class);

		// Disable logging
		// LogManager.getLogManager().reset();

		this.resourceSet = new ResourceSetImpl();

		UMLResourcesUtil.init(this.resourceSet);

		// Might be redundant
		this.resourceSet.getPackageRegistry().put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);
		this.resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);

		// Initialize model
		UMLPackage.eINSTANCE.eClass();
		EcoreUtil.resolveAll(UMLPackage.eINSTANCE);
	}

	// This method is invoked to generate a single test case
	@Override
	public Resource generate(SourceOfRandomness random, GenerationStatus __ignore__) {
		// Clean up resource from previous generation
		EList<Resource> resources = this.resourceSet.getResources();
		if (resources.size() > 0) {
			resources.get(0).unload();
			resources.remove(0);
			assert (resources.size() == 0);
		}

		// Create resource just in memory (we could also save it, of course)
		Resource modelResource = resourceSet.createResource(URI.createFileURI("uml_model" + ".uml"));

		// Delegate to some real model generator, just demo (we create the same simple
		// model in each and ever iteration)
		Model m = UMLFactory.eINSTANCE.createModel();
		m.setName("m");
		org.eclipse.uml2.uml.Class clazz = UMLFactory.eINSTANCE.createClass();
		clazz.setName("C");
		m.getPackagedElements().add(clazz);
		modelResource.getContents().add(m);
		
		// Lam: For some reason, the resource is not automatically set as modified, so we have to do it ourselves
		modelResource.setModified(true);
		
		if (modelResource.isModified()) {
			return modelResource;
		} else {
			throw new RuntimeException("Unable to create model");
		}
	}

}
