package org.dcm4che;

/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa Healthcare.
 * Portions created by the Initial Developer are Copyright (C) 2012
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.ejb.EJB;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.prefs.PreferencesDicomConfiguration;
import org.dcm4che3.conf.prefs.audit.PreferencesAuditLoggerConfiguration;
import org.dcm4che3.conf.prefs.audit.PreferencesAuditRecordRepositoryConfiguration;
import org.dcm4che3.conf.prefs.cdi.PrefsFactory;
import org.dcm4che3.conf.prefs.hl7.PreferencesHL7Configuration;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Device;
import org.dcm4chee.proxy.conf.prefs.PreferencesProxyConfigurationExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST services for the browser's frontend. Mostly these are wrappers around
 * EJB/Webservice interfaces of XDS
 * 
 * @author Roman K
 * 
 */
@SuppressWarnings("serial")
@Path("/")
public class ConfigTestServicesServlet {

    private static Logger log = LoggerFactory.getLogger(ConfigTestServicesServlet.class);

    
    @EJB
    MyEJB ejb;
    
    @Path("/read")
    @GET
    public Response readConfig() throws ConfigurationException, IOException, BackingStoreException {

        int howmany = 10;
        
        Date start = new Date();
        int num = 0;
        
        /*for (int i = 0; i < howmany; i++) {
            PreferencesDicomConfiguration conf = loadConfig();
            log.info("Iteration {}",i);
            num += ejb.loadConfigSingleTransaction(conf);
            conf.close();
        }

        log.info("Single transaction - Total time spend {}, visited nodes {}",new Date().getTime() - start.getTime(), num);
                
        start = new Date();
        num = 0;*/
        
        for (int i = 0; i < howmany; i++) {
            PreferencesDicomConfiguration conf = loadConfig();
            log.info("Iteration {}",i);
            num += loadConfig(conf);
            conf.close();
        }

        log.info("Multi transaction - Total time spend {}, visited nodes {}",new Date().getTime() - start.getTime(), num);

        return Response.ok().build();
    }

    
    
    @Path("/write")
    @GET
    public Response writeConfig() throws ConfigurationException, IOException, BackingStoreException {

        
        Date start = new Date();
        int num = 0;

        PreferencesDicomConfiguration conf = loadConfig();
        
        for (int i = 0; i < 10; i++) {

            log.info("Device {}",i);

            
            Device d = new Device("TstDevice"+i);
            ApplicationEntity ae = new ApplicationEntity("aeOf"+i);
            TransferCapabilitiesUtils.addTCsToAE(ae);
            
            d.addApplicationEntity(ae);
            
            conf.persist(d);
            
        }

        conf.close();

        log.info("Total time spend writing {}",new Date().getTime() - start.getTime());
                
        return Response.ok().build();
    }

    
    @Inject
    Instance<PrefsFactory> prefsFactoryInstance;

    public PreferencesDicomConfiguration loadConfig() {
        PreferencesDicomConfiguration conf;

        // check if there is an implementation of PrefsFactory provided and
        // construct DicomConfiguration accordingly
        if (!prefsFactoryInstance.isUnsatisfied()) {
            Preferences prefs = prefsFactoryInstance.get().getPreferences();
            log.info("Using custom Preferences implementation {}", prefs.getClass().toString());
            conf = new PreferencesDicomConfiguration(prefs);
        } else
            conf = new PreferencesDicomConfiguration();

        conf.addDicomConfigurationExtension(new PreferencesProxyConfigurationExtension());
        conf.addDicomConfigurationExtension(new PreferencesAuditLoggerConfiguration());
        conf.addDicomConfigurationExtension(new PreferencesAuditRecordRepositoryConfiguration());
        conf.addDicomConfigurationExtension(new PreferencesHL7Configuration());
        return conf;
    }

    
    public int  loadConfig(PreferencesDicomConfiguration conf) throws BackingStoreException, IOException {
        
        int num =0;
        // travel around
        Deque<Preferences> stack = new ArrayDeque<Preferences>();
        stack.push(conf.getRootPrefs());
        while (!stack.isEmpty()) {
            num++;
            Preferences p = stack.pop();
            for (String n : p.childrenNames()) 
                stack.push(p.node(n));
        }
        
        return num;
    }
}
