/*******************************************************************************
 * Copyright (c) 2015 Abel G�mez (AtlanMod) 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Abel G�mez (AtlanMod) - Additional modifications      
 *******************************************************************************/

package fr.inria.atlanmod.instantiator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.xmi.XMIResource;

import fr.obeo.emf.specimen.SpecimenGenerator;

/**
 * @author <a href="mailto:abel.gomez-llana@inria.fr">Abel Gomez</a>
 *
 */
public class GenericMetamodelGenerator {
	
	private final static Logger LOGGER = Logger.getLogger(GenericMetamodelGenerator.class.getName());

	protected Path samplesPath = Paths.get("data", "samples");
	
	protected GenericMetamodelConfig config;

	protected SpecimenGenerator generator; 
	
	
	public GenericMetamodelGenerator(GenericMetamodelConfig config) throws IllegalArgumentException {
		super();
		this.config = config;
		generator = new SpecimenGenerator(config, config.getSeed());
	}
	
	public GenericMetamodelGenerator(GenericMetamodelConfig config, Class<?> generator) {
		this(config);
		try {
				this.generator = (SpecimenGenerator)
							generator.getDeclaredConstructor(SpecimenGenerator.class, Long.class).newInstance(config, config.getSeed());
			} catch (Exception e) {
				LOGGER.severe(e.getLocalizedMessage());
			} 
	}
	
	public Path getSamplesPath() {
		return samplesPath;
	}

	public void setSamplesPath(Path samplesPath) {
		this.samplesPath = samplesPath;
	}

	public void runGeneration(ResourceSet resourceSet, int numberOfModels, int averageSize, float variation) throws GenerationException {
		
		try {

			LOGGER.info(MessageFormat.format("Creating {0} models", numberOfModels));
			
			LOGGER.info(MessageFormat.format("Generator seed is ''{0}''", config.getSeed()));
			LOGGER.info(MessageFormat.format("Config parameters: range for models size is [{0}, {1}]", 
					config.getElementsRange().getMinimum(), config.getElementsRange().getMaximum()));
			LOGGER.info(MessageFormat.format("Config parameters: range for properties number is [{0}, {1}]", 
					config.getPropertiesRange().getMinimum(), config.getPropertiesRange().getMaximum()));
			LOGGER.info(MessageFormat.format("Config parameters: range for references number is [{0}, {1}]", 
					config.getReferencesRange().getMinimum(), config.getReferencesRange().getMaximum()));
			LOGGER.info(MessageFormat.format("Config parameters: range for values length is [{0}, {1}]", 
					config.getValuesRange().getMinimum(), config.getValuesRange().getMaximum()));
			
			for (int i = 0; i < numberOfModels; i++) {
				
				Resource resource = resourceSet.createResource(formatURI(getMetaModelResourceName(), i, averageSize));

				LOGGER.info(MessageFormat.format("Start generation of resource {0} with an average size of {1} elements", 
						resource.getURI(), averageSize));
				
				//generator.generate(resource);
			}
			for (Resource resource :  resourceSet.getResources()) {
				if (resource.isModified()) {
					LOGGER.info(MessageFormat.format("Saving resource {0}", resource.getURI()));
					resource.save(Collections.emptyMap());
				}
			}
			LOGGER.info("All resources have been saved");
		} catch (IOException e) {
			LOGGER.severe(e.getLocalizedMessage());
			throw new GenerationException(e);
		}
	}
	


	protected String getMetaModelResourceName() {
		//URI metamodelURI = config.getMetamodelResource().getURI();
		//return metamodelURI.lastSegment().substring(0, metamodelURI.lastSegment().indexOf("."));
		return "";
	}

	protected URI formatURI(String modelPrefix, long maxElement, int index) {
		StringBuilder builder = new StringBuilder();
		builder.append(samplesPath.toString());
		builder.append(File.separator);
		builder.append(modelPrefix);
		builder.append(File.separator);
//		builder.append("model");
//		builder.append(maxElement);
//		builder.append(File.separator);
		builder.append("result");
		builder.append(maxElement);
		builder.append("_");
		builder.append(index);
		builder.append(".");
		builder.append(XMIResource.XMI_NS);;
		return URI.createFileURI(builder.toString());
	}

}
