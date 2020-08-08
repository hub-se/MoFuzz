package de.hub.mse.emf.generator.internal;

import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;


/**
 * Wrapper class for handling metamodels loaded from .ecore files as well as from plugins
 * Required since the eAllContents iterator of an {@link EPackage} does not include the package itself.
 * 
 * FIXME currently this implementation only supports a single {@link EPackage} as the content provider
 */
public class MetamodelResource {
	
	/** The metamodel {@link Resource}. */
	private Resource metamodelResource;
	
	/** The metamodel {@link EPackage}, can be set instead if no metamodel resource is available. */
	private EPackage metamodelEPackage;
	
	/** The contents of the {@link EPackage} */
	private List<EObject> ePackageContent;
	
	/**
	 * Instantiates a wrapper for the metamodel {@link Resource}
	 * @param metamodelResource the {@link Resource} containing the metamodel
	 */
	public MetamodelResource(Resource metamodelResource) {
		this.metamodelResource = metamodelResource;
	}
	
	/**
	 * Instantiates a wrapper for the metamodel {@link EPackage}
	 * @param metamodelEPackage the {@link EPackage} with the contents of the metamodel
	 */
	public MetamodelResource(EPackage metamodelEPackage) {
		this.metamodelEPackage = metamodelEPackage;
		ePackageContent = new BasicEList<EObject>();
		ePackageContent.add(metamodelEPackage);
		for(TreeIterator<EObject> it = metamodelEPackage.eAllContents(); it.hasNext();) {
			ePackageContent.add(it.next());
		}
	}
	
	/**
	 * Returns the contents of the metamodel as an iterator
	 * @return the contents of the metamodel
	 */
	public Iterator<EObject> getAllContents(){
		if(this.metamodelResource != null) {
			return this.metamodelResource.getAllContents();
		}
		else if(this.metamodelEPackage != null) {
			return this.ePackageContent.iterator();
		}
		else {
			return null;
		}
	}
}
