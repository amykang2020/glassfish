/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html or
 * glassfish/bootstrap/legal/CDDLv1.0.txt.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at glassfish/bootstrap/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 */
package org.glassfish.persistence.jpa;

import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.PersistenceUnitDescriptor;
import com.sun.enterprise.module.ModuleDefinition;
import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.Module;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.MetaData;
import org.glassfish.api.deployment.InstrumentableClassLoader;
import org.glassfish.deployment.common.SimpleDeployer;
import org.glassfish.deployment.common.DeploymentException;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;

import javax.naming.NamingException;
import javax.persistence.spi.ClassTransformer;
import javax.sql.DataSource;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;


/**
 * Deployer for JPA applications
 * @author Mitesh Meswani
 */
@Service
public class JPADeployer extends SimpleDeployer<JPAContainer, JPAApplication> {

    @Inject
    private ConnectorRuntime connectorRuntime;

    @Inject
    private ModulesRegistry modulesRegistry;
    

    @Override public MetaData getMetaData() {

        return new MetaData(true /*invalidateCL */ ,
                null /* provides */,
                new Class[] {Application.class} /* requires Application from dol */);
    }

    protected void generateArtifacts(DeploymentContext dc) throws DeploymentException {
        // Noting to generate yet!!
    }

    protected void cleanArtifacts(DeploymentContext dc) throws DeploymentException {
        // Noting to cleanup yet!!
    }

    /**
     * @InheritDoc
     */
    public <V> V loadMetaData(Class<V> type, DeploymentContext context) {
        return null;
    }

    /**
     * EMFs for refered pus are created and stored in JPAApplication instance.
     * The JPAApplication instance is stored in given DeploymentContext to be retrieved by load
     */
    @Override public boolean prepare(DeploymentContext context) {
        boolean prepared = super.prepare(context);

        if(prepared) {

            Application application = context.getModuleMetaData(Application.class);
            Set<BundleDescriptor> bundles = application.getBundleDescriptors();

            //TODO Need to modify this to be more generic.
            // Iterate through all the bundles for the app and collect pu references in referencedPus
            List<PersistenceUnitDescriptor> allReferencedPus = new ArrayList<PersistenceUnitDescriptor>();
            for (BundleDescriptor bundle : bundles) {
                allReferencedPus.addAll(bundle.findReferencedPUs());
            }
            // EMFs get created here. JPAApplication maintains list of created EMFs so that they
            // can be closed at undeploy
            JPAApplication jpaApp = new JPAApplication(allReferencedPus, new ProviderContainerContractInfoImpl(context, connectorRuntime));

            // Store jpaApp in DeploymentContext to retrieve it during load
            context.addModuleMetaData(jpaApp);
        }

        return prepared;
    }

    /**
     * @InheritDoc
     */
    @Override public JPAApplication load(JPAContainer container, DeploymentContext context) {
        // Return the JPAApplication stored in DeploymentContext during prepaare phase
        return context.getModuleMetaData(JPAApplication.class);
    }

    private static class ProviderContainerContractInfoImpl
            implements ProviderContainerContractInfo {
        private final DeploymentContext deploymentContext;
        private final ConnectorRuntime connectorRuntime;
        private final ClassLoader finalClassLoader;
        
        public ProviderContainerContractInfoImpl(DeploymentContext deploymentContext, ConnectorRuntime connectorRuntime) {
            this.deploymentContext = deploymentContext;
            this.connectorRuntime = connectorRuntime;
            this.finalClassLoader=deploymentContext.getFinalClassLoader();
        }

        public ClassLoader getClassLoader() {
            return finalClassLoader;
        }

        public ClassLoader getTempClassloader() {
            return ( (InstrumentableClassLoader)deploymentContext.getClassLoader() ).copy();
        }

        public void addTransformer(final ClassTransformer transformer) {
            // Bridge between java.lang.instrument.ClassFileTransformer that DeploymentContext accepts
            // and javax.persistence.spi.ClassTransformer that JPA supplies.
            deploymentContext.addTransformer(new ClassFileTransformer() {
                public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                        ProtectionDomain protectionDomain, byte[] classfileBuffer)
                        throws IllegalClassFormatException {
                    return transformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
                }
            });
        }

        public String getApplicationLocation() {
            //TODO : This needs to be cleaned up post TP2 to be more generic than dc.getSourceDir().
            return deploymentContext.getSourceDir().getAbsolutePath();
        }

        public DataSource lookupDataSource(String dataSourceName) throws NamingException {
            return DataSource.class.cast(connectorRuntime.lookupPMResource(dataSourceName, false) );
        }

        public DataSource lookupNonTxDataSource(String dataSourceName) throws NamingException {
            return DataSource.class.cast(connectorRuntime.lookupNonTxResource(dataSourceName, false));
        }

    } // class ProviderContainerContractInfoImpl

}
