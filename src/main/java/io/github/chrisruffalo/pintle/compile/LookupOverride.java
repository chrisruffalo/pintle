package io.github.chrisruffalo.pintle.compile;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.xbill.DNS.Lookup;

@TargetClass(Lookup.class)
public final class LookupOverride {

    @Substitute
    public static synchronized void refreshDefault() {

    }

}
