/*******************************************************************************
 * Copyright (c) 2016 Avaloq Evolution AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Avaloq Evolution AG - initial API and implementation
 *******************************************************************************/
package com.avaloq.tools.ddk.check.runtime.configuration;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.linking.lazy.LazyLinkingResource;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.resource.IResourceServiceProvider.Registry;
import org.eclipse.xtext.validation.Issue;


/**
 * The default check configuration service. Gets a {@link ICheckConfigurationStore check
 * configuration store} for any given context object associated to an {@link IProject}.
 * <p>
 * Subclasses may control how current project is derived from given context object.
 * </p>
 */
public class CheckConfigurationStoreService implements ICheckConfigurationStoreService {

  private static final Logger LOGGER = Logger.getLogger(CheckConfigurationStoreService.class);

  // CHECKSTYLE:OFF
  protected IProject project;
  // CHECKSTYLE:ON

  private ICheckConfigurationStore configurationStore;

  public CheckConfigurationStoreService() { // NOPMD
    // does nothing yet
  }

  /** {@inheritDoc} */
  @Override
  public ICheckConfigurationStore getCheckConfigurationStore(final Object context) {
    setProject(context);
    ICheckConfigurationStore store = internalGetCheckConfigurationStore(context);
    if (store instanceof CheckConfigurationStore) {
      // project can be null!
      ((CheckConfigurationStore) store).setProject(this.project);
    }
    String language = getLanguage(context);
    if (language != null) {
      return new LanguageSpecificCheckConfigurationStore(store, language);
    } else {
      return store;
    }
  }

  /**
   * Gets the language for the given object.
   *
   * @param context
   *          object
   * @return the language the corresponding resource was parsed from, may be {@code null}
   */
  private String getLanguage(final Object context) {
    Resource resource = null;
    if (context instanceof EObject) {
      resource = ((EObject) context).eResource();
    } else if (context instanceof Issue) {
      URI uri = ((Issue) context).getUriToProblem();
      if (uri != null) {
        Registry registry = IResourceServiceProvider.Registry.INSTANCE;
        IResourceServiceProvider resourceServiceProvider = registry.getResourceServiceProvider(uri);
        if (resourceServiceProvider != null) {
          resource = resourceServiceProvider.get(LazyLinkingResource.class);
        } else {
          LOGGER.error("Could not fetch a ResourceServiceProvider for URI: " + uri); //$NON-NLS-1$
        }
      } else {
        LOGGER.warn("Could not fetch eResource from issue: URI to problem is null"); //$NON-NLS-1$
      }
    }

    if (resource instanceof LazyLinkingResource) {
      return ((LazyLinkingResource) resource).getLanguageName();
    }

    return null;
  }

  /**
   * Gets the check configuration store instance. Clients may override this method in order
   * to specify different implementations of stores depending on given context object.
   *
   * @param context
   *          the context object of the validation being executed
   * @return the configuration store instance
   */
  protected ICheckConfigurationStore internalGetCheckConfigurationStore(final Object context) {
    if (configurationStore == null) {
      configurationStore = new CheckConfigurationStore(DEFAULT_CHECK_CONFIGURATION_NODE);
    }
    return configurationStore;
  }

  /**
   * Sets the project if an associated instance can be found for given context object.
   * <p>
   * This is the default implementation. Only platform URI-based schemas are supported. Other implementations may overwrite {@link #getProject()}.
   * </p>
   *
   * @param context
   *          the context object, potentially contained by an IProject
   */
  protected void setProject(final Object context) {
    if (context instanceof IProject) {
      this.project = (IProject) context;
    } else if (context instanceof IFile) {
      this.project = ((IFile) context).getProject();
    } else {
      URI uri = null;
      if (context instanceof EObject && ((EObject) context).eResource() != null) {
        uri = ((EObject) context).eResource().getURI();
      }
      if (context instanceof Issue) {
        uri = ((Issue) context).getUriToProblem();
      }
      if (uri != null && uri.isPlatform()) {
        final IFile file = (IFile) ResourcesPlugin.getWorkspace().getRoot().findMember(uri.toPlatformString(true));
        this.project = file.getProject();
      }
    }
  }
}
