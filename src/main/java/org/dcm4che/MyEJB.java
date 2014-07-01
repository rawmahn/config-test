package org.dcm4che;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.dcm4che3.conf.prefs.PreferencesDicomConfiguration;
import org.dcm4che3.conf.prefs.audit.PreferencesAuditRecordRepositoryConfiguration;

@Stateless
public class MyEJB {

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public int loadConfigSingleTransaction(PreferencesDicomConfiguration conf) throws BackingStoreException, IOException {
        
        return new ConfigTestServicesServlet().loadConfig(conf);
    }
    
}
